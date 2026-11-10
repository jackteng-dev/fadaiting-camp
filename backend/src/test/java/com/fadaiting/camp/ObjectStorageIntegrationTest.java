package com.fadaiting.camp;

import com.fadaiting.camp.config.ObjectStorageProperties;
import com.fadaiting.camp.service.ObjectStorageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** 真 SeaweedFS，不以 mock S3 取代授權、簽名與位元組驗收。 */
@EnabledIfEnvironmentVariable(named = "CAMP_STORAGE_INTEGRATION", matches = "true")
class ObjectStorageIntegrationTest {
    private ObjectStorageProperties config;
    private ObjectStorageService storage;
    private S3Client client;
    private final List<String[]> created = new ArrayList<>();
    private static final byte[] IMAGE = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/lN0AAAAASUVORK5CYII=");

    @BeforeEach void setup() {
        config = new ObjectStorageProperties();
        config.setEndpoint(System.getenv("CAMP_STORAGE_ENDPOINT"));
        config.setAccessKey(System.getenv("CAMP_STORAGE_ACCESS_KEY"));
        config.setSecretKey(System.getenv("CAMP_STORAGE_SECRET_KEY"));
        config.setPublicBasePath("");
        storage = new ObjectStorageService(config);client = ObjectStorageService.client(config);
    }
    @AfterEach void cleanup() {
        for (String[] object : created) client.deleteObject(b -> b.bucket(object[0]).key(object[1]));
        client.close();
    }
    private String upload(String bucket, String key) throws Exception {
        String url = storage.fileupload(new MockMultipartFile("file", "test.png", "image/png", IMAGE), bucket, key);
        String actual = URI.create(url).getPath().substring(bucket.length() + 2);
        created.add(new String[]{bucket, actual});
        return url;
    }
    private HttpResponse<byte[]> read(String url) throws Exception {
        try (HttpClient http = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build()) {
            return http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        }
    }
    @Test void allExistingBucketsUploadAndPresignedReadExactBytes() throws Exception {
        for (String bucket : ObjectStorageService.BUCKETS) {
            String url = upload(bucket, null);var response = read(url);
            assertEquals(200, response.statusCode());assertArrayEquals(IMAGE, response.body());
            assertEquals("image/png", response.headers().firstValue("Content-Type").orElseThrow());
            assertTrue(url.contains("X-Amz-Expires=604800"));
        }
    }
    @Test void unsignedAndTamperedReadsAreDenied() throws Exception {
        String url = upload("camp-images", null);
        assertEquals(403, read(url.substring(0, url.indexOf('?'))).statusCode());
        assertEquals(403, read(url.replaceFirst("X-Amz-Signature=.", "X-Amz-Signature=z")).statusCode());
    }
    @Test void overwritesSameLegacyObjectKeyWithoutChangingUrlPath() throws Exception {
        String key = UUID.randomUUID().toString();String first = upload("user-avatar", key);
        assertEquals(URI.create(first).getPath(), URI.create(upload("user-avatar", key)).getPath());
        assertArrayEquals(IMAGE, read(first).body());
    }
    @Test void sameOriginPrefixPreservesSignatureAndObject() throws Exception {
        config.setPublicBasePath("/objects");
        String key = UUID.randomUUID().toString();
        String relative = storage.fileupload(new MockMultipartFile("file", "a.png", "image/png", IMAGE), "rental-item-images", key);
        created.add(new String[]{"rental-item-images", key});
        assertTrue(relative.startsWith("/objects/rental-item-images/"));
        assertEquals(200, read(config.getEndpoint() + relative.substring("/objects".length())).statusCode());
    }
    @Test void wrongCredentialsDoNotTurnAccessDeniedIntoBucketCreation() throws Exception {
        upload("camp-images", null);config.setSecretKey("incorrect-synthetic-secret");
        var error = assertThrows(S3Exception.class, () -> storage.fileupload(new MockMultipartFile("file", "a.png", "image/png", IMAGE), "camp-images", null));
        assertEquals(403, error.statusCode());
    }
    @Test void leavesPrivateRestartProofForFixtureVerifier() throws Exception {
        String url = upload("camp-images", null);
        assertArrayEquals(IMAGE, read(url).body());
        created.clear(); // 僅拋棄容器中的合成物件，供完整 suite 後真正 restart 核對。
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(IMAGE));
        Path target = Path.of(System.getenv("CAMP_STORAGE_PROOF_FILE"));
        Files.createFile(target, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
        Files.writeString(target, "{\"url\":\"" + url + "\",\"sha256\":\"" + digest + "\"}");
    }
}
