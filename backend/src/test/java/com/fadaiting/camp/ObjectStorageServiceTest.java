package com.fadaiting.camp;

import com.fadaiting.camp.config.ObjectStorageProperties;
import com.fadaiting.camp.service.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class ObjectStorageServiceTest {
    @Test void preservesEncodedPathAndExactSignedQuery() {
        assertEquals("/objects/camp-images/a%20b?X-Amz-Signature=abc%2Fdef&X-Amz-Credential=x",
                ObjectStorageService.browserUrl("http://seaweed:8333/camp-images/a%20b?X-Amz-Signature=abc%2Fdef&X-Amz-Credential=x", "/objects"));
    }
    @Test void supportsExplicitDirectLocalRead() {
        assertEquals("http://localhost:9000/a?x=y", ObjectStorageService.browserUrl("http://localhost:9000/a?x=y", ""));
    }
    @Test void refusesExternalTraversalAndAmbiguousPrefixes() {
        for (String prefix : new String[]{"https://evil.invalid", "//evil.invalid", "/../objects", "/objects?x=1", "/objects/", "/obj//ects"}) {
            assertThrows(IllegalArgumentException.class, () -> ObjectStorageService.browserUrl("http://seaweed:8333/a", prefix));
        }
    }
    @Test void refusesNonHttpAndEmbeddedCredentials() {
        for (String url : new String[]{"file:///etc/passwd", "http://user:password@storage/a"}) {
            assertThrows(IllegalArgumentException.class, () -> ObjectStorageService.browserUrl(url, "/objects"));
        }
    }
    @Test void requiresExplicitCredentialsRatherThanAwsDefaultChain() {
        assertThrows(IllegalStateException.class, () -> ObjectStorageService.client(new ObjectStorageProperties()));
    }
    @Test void rejectsUnknownBucketsAndTraversalKeys() {
        assertThrows(IllegalArgumentException.class, () -> ObjectStorageService.validateObject("private-records", null));
        for (String key : new String[]{"../secret", "a/b", "a?query", ""}) {
            assertThrows(IllegalArgumentException.class, () -> ObjectStorageService.validateObject("camp-images", key));
        }
    }
    @Test void rejectsEmptyAndActiveContentBeforeNetworkAccess() {
        var service = new ObjectStorageService(new ObjectStorageProperties());
        assertThrows(IllegalArgumentException.class, () -> service.fileupload(new MockMultipartFile("file", new byte[0]), "camp-images", null));
        assertThrows(IllegalArgumentException.class, () -> service.fileupload(new MockMultipartFile("file", "a.svg", "image/svg+xml", "<svg/>".getBytes()), "camp-images", null));
    }
    @Test void rejectsEndpointsWithCredentialsPathsOrQuery() {
        var config = new ObjectStorageProperties();config.setAccessKey("synthetic-access");config.setSecretKey("synthetic-secret");
        for (String endpoint : new String[]{"file:///data", "http://user:pass@storage", "http://storage/path", "http://storage?query"}) {
            config.setEndpoint(endpoint);
            assertThrows(IllegalStateException.class, () -> ObjectStorageService.endpoint(config));
        }
    }
}
