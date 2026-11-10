package com.fadaiting.camp.service;

import com.fadaiting.camp.config.ObjectStorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/** SeaweedFS S3 上傳；沿用既有 bucket、object key 與七天簽名網址契約。 */
@Service
public class ObjectStorageService {
    public static final Set<String> BUCKETS = Set.of("user-avatar", "camp-images", "rental-item-images");
    private final ObjectStorageProperties properties;

    public ObjectStorageService(ObjectStorageProperties properties) {
        this.properties = properties;
    }

    public String fileupload(MultipartFile file, String bucket, String filename) throws Exception {
        validateObject(bucket, filename);
        if (file == null || file.isEmpty() || file.getSize() > 30L * 1024 * 1024
                || file.getContentType() == null || !Set.of("image/png", "image/jpeg", "image/webp", "image/gif").contains(file.getContentType())) {
            throw new IllegalArgumentException("請選擇 30MB 以內的 PNG、JPEG、WebP 或 GIF 圖片");
        }
        URI endpoint = endpoint(properties);
        String key = filename == null ? UUID.randomUUID().toString() : filename;
        try (S3Client client = client(properties); S3Presigner signer = presigner(properties)) {
            try {
                client.headBucket(b -> b.bucket(bucket));
            } catch (S3Exception error) {
                if (error.statusCode() != 404) throw error;
                try {
                    client.createBucket(b -> b.bucket(bucket));
                } catch (S3Exception creation) {
                    if (creation.awsErrorDetails() == null || !"BucketAlreadyOwnedByYou".equals(creation.awsErrorDetails().errorCode())) throw creation;
                }
            }
            try (var stream = file.getInputStream()) {
                client.putObject(b -> b.bucket(bucket).key(key).contentType(file.getContentType()),
                        RequestBody.fromInputStream(stream, file.getSize()));
            }
            String signed = signer.presignGetObject(b -> b.signatureDuration(Duration.ofDays(7))
                    .getObjectRequest(o -> o.bucket(bucket).key(key))).url().toString();
            // Gateway 必須保留此 endpoint 的 Host 與原始 query，不能公開 filer 或 bucket。
            if (!URI.create(signed).getAuthority().equals(endpoint.getAuthority())) throw new IllegalStateException("圖片簽名來源不符");
            return browserUrl(signed, properties.getPublicBasePath());
        }
    }

    public static void validateObject(String bucket, String key) {
        if (bucket == null || !BUCKETS.contains(bucket) || key != null && (!key.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,199}") || key.contains(".."))) {
            throw new IllegalArgumentException("圖片 bucket 或 object key 不合法");
        }
    }

    public static URI endpoint(ObjectStorageProperties properties) {
        URI endpoint = URI.create(properties.getEndpoint());
        if (!Set.of("http", "https").contains(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getRawUserInfo() != null || endpoint.getRawQuery() != null || endpoint.getRawFragment() != null
                || !Set.of("", "/").contains(endpoint.getRawPath())
                || properties.getAccessKey().isBlank() || properties.getSecretKey().isBlank()) {
            throw new IllegalStateException("請設定 Camp 專用 S3 endpoint 與憑證");
        }
        browserUrl(endpoint + "/check", properties.getPublicBasePath());
        return endpoint;
    }

    public static S3Client client(ObjectStorageProperties properties) {
        return S3Client.builder().endpointOverride(endpoint(properties)).region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofSeconds(60)).retryPolicy(RetryPolicy.none()))
                .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(5)).socketTimeout(Duration.ofSeconds(30)))
                .build();
    }

    public static S3Presigner presigner(ObjectStorageProperties properties) {
        return S3Presigner.builder().endpointOverride(endpoint(properties)).region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
    }

    public static String browserUrl(String signedUrl, String prefix) {
        if (prefix == null || prefix.isBlank()) return signedUrl;
        if (!prefix.matches("/[a-zA-Z0-9][a-zA-Z0-9/_-]*") || prefix.contains("//") || prefix.endsWith("/")) {
            throw new IllegalArgumentException("圖片前綴必須是固定同源相對路徑");
        }
        URI uri = URI.create(signedUrl);
        if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null || uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException("圖片簽名必須是 HTTP URL");
        }
        return prefix + uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }
}
