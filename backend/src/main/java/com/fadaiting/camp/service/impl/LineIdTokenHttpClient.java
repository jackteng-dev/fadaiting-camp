package com.fadaiting.camp.service.impl;

import com.fadaiting.camp.expection.LineIdentityException;
import com.fadaiting.camp.service.LineIdTokenClient;
import org.springframework.stereotype.Component;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.io.InputStream;

@Component
public class LineIdTokenHttpClient implements LineIdTokenClient {
    private static final URI VERIFY_URL = URI.create("https://api.line.me/oauth2/v2.1/verify");
    private final HttpClient client;

    public LineIdTokenHttpClient() { this(buildClient()); }
    LineIdTokenHttpClient(HttpClient client) { this.client = client; }

    private static HttpClient buildClient() {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER);
        String proxy = System.getenv("HTTPS_PROXY");
        if (proxy == null || proxy.isBlank()) proxy = System.getenv("https_proxy");
        if (proxy != null && !proxy.isBlank()) {
            URI uri = URI.create(proxy);
            if (!"http".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) throw new IllegalArgumentException("LINE 驗證的 HTTPS proxy 設定無效");
            builder.proxy(ProxySelector.of(new InetSocketAddress(uri.getHost(),uri.getPort() < 0 ? 80 : uri.getPort())));
        }
        return builder.build();
    }

    @Override public String verify(String idToken, String channelId) {
        String body = "id_token="+URLEncoder.encode(idToken,StandardCharsets.UTF_8)+"&client_id="+URLEncoder.encode(channelId,StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(VERIFY_URL).timeout(Duration.ofSeconds(10))
                .header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            HttpResponse<InputStream> response = client.send(request,HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream stream = response.body()) {
                if (response.statusCode() == 400 || response.statusCode() == 401 || response.statusCode() == 403) throw new LineIdentityException(false);
                if (response.statusCode() != 200) throw new LineIdentityException(true);
                byte[] bytes = stream.readNBytes(16385);
                if (bytes.length > 16384) throw new LineIdentityException(true);
                return new String(bytes,StandardCharsets.UTF_8);
            }
        } catch (LineIdentityException error) { throw error; }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new LineIdentityException(true); }
        catch (Exception error) { throw new LineIdentityException(true); }
    }
}
