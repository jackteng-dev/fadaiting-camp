package com.fadaiting.camp.service;

import com.fasterxml.jackson.databind.*;
import com.fadaiting.camp.config.LineIdentityProperties;
import com.fadaiting.camp.expection.LineIdentityException;
import org.springframework.stereotype.Service;
import java.time.Clock;

@Service
public class LineIdentityVerifier {
    public record Identity(String userId, String displayName, String pictureUrl) {}
    private final LineIdentityProperties properties;
    private final LineIdTokenClient client;
    private final ObjectMapper json;
    private final Clock clock;
    public LineIdentityVerifier(LineIdentityProperties properties, LineIdTokenClient client, ObjectMapper json, Clock clock) {
        this.properties = properties; this.client = client; this.json = json; this.clock = clock;
    }

    public Identity verify(String idToken) {
        if (idToken == null || idToken.isBlank() || idToken.length() > 8192) throw new LineIdentityException(false);
        if (!properties.isEnabled() || properties.getChannelId() == null || properties.getChannelId().isBlank()) throw new LineIdentityException(true);
        try {
            JsonNode claims = json.readTree(client.verify(idToken,properties.getChannelId()));
            if (claims == null || !claims.isObject()) throw new LineIdentityException(true);
            long now = clock.instant().getEpochSecond();
            if (!claims.path("iss").isTextual() || !claims.path("aud").isTextual() || !claims.path("sub").isTextual()
                    || !"https://access.line.me".equals(claims.path("iss").asText())
                    || !properties.getChannelId().equals(claims.path("aud").asText())
                    || !claims.path("sub").asText().matches("U[0-9a-f]{32}")
                    || !claims.path("exp").isIntegralNumber() || !claims.path("exp").canConvertToLong() || claims.path("exp").asLong() <= now
                    || !claims.path("iat").isIntegralNumber() || !claims.path("iat").canConvertToLong() || claims.path("iat").asLong() <= 0 || claims.path("iat").asLong() > now+60)
                throw new LineIdentityException(false);
            String name = claims.path("name").asText(""), picture = claims.path("picture").asText("");
            if (name.length() > 100) name = name.substring(0,100);
            if (picture.length() > 512 || !picture.startsWith("https://")) picture = "";
            return new Identity(claims.path("sub").asText(),name,picture);
        } catch (LineIdentityException error) { throw error; }
        catch (Exception error) { throw new LineIdentityException(true); }
    }
}
