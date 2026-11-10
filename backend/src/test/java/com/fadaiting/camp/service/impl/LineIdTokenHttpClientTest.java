package com.fadaiting.camp.service.impl;

import com.fadaiting.camp.expection.LineIdentityException;
import org.junit.jupiter.api.*;
import java.io.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class LineIdTokenHttpClientTest {
    private final HttpClient http=mock(HttpClient.class);
    private final LineIdTokenHttpClient client=new LineIdTokenHttpClient(http);
    @SuppressWarnings("unchecked") private final HttpResponse<InputStream> response=mock(HttpResponse.class);
    private void reply(int status,String body) throws Exception { when(response.statusCode()).thenReturn(status);when(response.body()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response); }
    @Test void postsToFixedHttpsOfficialEndpointWithTimeout() throws Exception {
        reply(200,"{}"); assertEquals("{}",client.verify("synthetic","channel"));var request=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(http).send(request.capture(),any(HttpResponse.BodyHandler.class));assertEquals("https://api.line.me/oauth2/v2.1/verify",request.getValue().uri().toString());assertEquals("POST",request.getValue().method());assertEquals(10,request.getValue().timeout().orElseThrow().toSeconds());assertEquals("application/x-www-form-urlencoded",request.getValue().headers().firstValue("Content-Type").orElseThrow());
    }
    @Test void providerRejectionIs401CategoryAndDoesNotEchoBody() throws Exception { reply(400,"synthetic-secret-token");var error=assertThrows(LineIdentityException.class,()->client.verify("synthetic-secret-token","channel"));assertFalse(error.isUnavailable());assertFalse(error.getMessage().contains("synthetic-secret-token")); }
    @Test void redirectsAreNotAcceptedAsIdentityProof() throws Exception { reply(302,"{}");assertTrue(assertThrows(LineIdentityException.class,()->client.verify("synthetic","channel")).isUnavailable()); }
    @Test void oversizedResponseIsRejected() throws Exception { reply(200,"x".repeat(16385));assertTrue(assertThrows(LineIdentityException.class,()->client.verify("synthetic","channel")).isUnavailable()); }
    @Test void ioFailureDoesNotExposeProviderOrToken() throws Exception { when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("synthetic-secret-token"));var error=assertThrows(LineIdentityException.class,()->client.verify("synthetic-secret-token","channel"));assertTrue(error.isUnavailable());assertFalse(error.getMessage().contains("synthetic-secret-token")); }
}
