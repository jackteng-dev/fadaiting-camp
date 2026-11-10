package com.fadaiting.camp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fadaiting.camp.config.LineIdentityProperties;
import com.fadaiting.camp.expection.LineIdentityException;
import com.fadaiting.camp.service.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LineIdentityVerifierTest {
    private final LineIdentityProperties properties = new LineIdentityProperties();
    private final LineIdTokenClient client = mock(LineIdTokenClient.class);
    private final ObjectMapper json = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-06T01:00:00Z"),ZoneId.of("Asia/Taipei"));
    private final LineIdentityVerifier verifier = new LineIdentityVerifier(properties,client,json,clock);
    private Map<String,Object> claims;
    @BeforeEach void setup() {
        properties.setEnabled(true); properties.setChannelId("synthetic-channel");
        claims = new HashMap<>(Map.of("iss","https://access.line.me","aud","synthetic-channel","sub","U"+"a".repeat(32),"exp",clock.instant().getEpochSecond()+60,"iat",clock.instant().getEpochSecond()-60,"name","官方核驗名稱","picture","https://profile.line-scdn.net/synthetic"));
    }
    private void response() throws Exception { when(client.verify("synthetic-token","synthetic-channel")).thenReturn(json.writeValueAsString(claims)); }
    private void invalid() throws Exception { response(); assertFalse(assertThrows(LineIdentityException.class,()->verifier.verify("synthetic-token")).isUnavailable()); }
    @Test void disabledIdentityNeverTrustsCaller() { properties.setEnabled(false); assertTrue(assertThrows(LineIdentityException.class,()->verifier.verify("synthetic-token")).isUnavailable()); verifyNoInteractions(client); }
    @Test void missingChannelIsUnavailable() { properties.setChannelId(""); assertTrue(assertThrows(LineIdentityException.class,()->verifier.verify("synthetic-token")).isUnavailable()); verifyNoInteractions(client); }
    @Test void emptyOrOversizedTokenIsRejectedBeforeProvider() { for(String token : new String[]{""," ","x".repeat(8193)}) assertFalse(assertThrows(LineIdentityException.class,()->verifier.verify(token)).isUnavailable()); verifyNoInteractions(client); }
    @Test void verifiedSubjectAndProfileComeFromProvider() throws Exception { response(); var identity = verifier.verify("synthetic-token"); assertEquals("U"+"a".repeat(32),identity.userId()); assertEquals("官方核驗名稱",identity.displayName()); }
    @Test void wrongIssuerIsRejected() throws Exception { claims.put("iss","https://attacker.invalid"); invalid(); }
    @Test void wrongChannelIsRejected() throws Exception { claims.put("aud","another-channel"); invalid(); }
    @Test void invalidSubjectIsRejected() throws Exception { claims.put("sub","caller-supplied-user"); invalid(); }
    @Test void expiredTokenIsRejected() throws Exception { claims.put("exp",clock.instant().getEpochSecond()); invalid(); }
    @Test void stringExpiryIsNotCoercedToValidNumber() throws Exception { claims.put("exp","9999999999"); invalid(); }
    @Test void missingExpiryIsRejected() throws Exception { claims.remove("exp"); invalid(); }
    @Test void futureIssuedTokenIsRejected() throws Exception { claims.put("iat",clock.instant().getEpochSecond()+61); invalid(); }
    @Test void malformedProviderResponseFailsClosed() { when(client.verify(anyString(),anyString())).thenReturn("not-json"); assertTrue(assertThrows(LineIdentityException.class,()->verifier.verify("synthetic-token")).isUnavailable()); }
    @Test void providerRejectionIsPreservedWithoutTokenLeak() { when(client.verify(anyString(),anyString())).thenThrow(new LineIdentityException(false)); var error=assertThrows(LineIdentityException.class,()->verifier.verify("synthetic-token")); assertFalse(error.getMessage().contains("synthetic-token")); assertFalse(error.isUnavailable()); }
    @Test void providerFailureNeverFallsBackToUnverifiedIdentity() { when(client.verify(anyString(),anyString())).thenThrow(new LineIdentityException(true)); assertTrue(assertThrows(LineIdentityException.class,()->verifier.verify("synthetic-token")).isUnavailable()); }
    @Test void ProfileLengthAndImageSchemeAreBounded() throws Exception { claims.put("name","名".repeat(101)); claims.put("picture","javascript:alert(1)"); response(); var identity=verifier.verify("synthetic-token"); assertEquals(100,identity.displayName().length()); assertEquals("",identity.pictureUrl()); }
}
