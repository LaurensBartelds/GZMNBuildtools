package nl.gzmn.gZMNBuildtools.modules.webeditor.server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebAccessTest {

    @Test
    void noConfiguredCodeMeansOpen() {
        assertTrue(WebAccess.codeAccepted(null, null));
        assertTrue(WebAccess.codeAccepted("", null));
        assertTrue(WebAccess.codeAccepted("   ", "anything"));
    }

    @Test
    void codeMustMatchExactly() {
        assertTrue(WebAccess.codeAccepted("s3cret", "s3cret"));
        assertFalse(WebAccess.codeAccepted("s3cret", "s3cre"));
        assertFalse(WebAccess.codeAccepted("s3cret", null));
        assertFalse(WebAccess.codeAccepted("s3cret", "S3CRET"));
    }

    @Test
    void corsOnlyForListedOrigins() {
        List<String> allowed = List.of("https://build.example.com");
        assertEquals("https://build.example.com", WebAccess.corsOrigin("https://build.example.com", allowed));
        assertNull(WebAccess.corsOrigin("https://evil.example.com", allowed));
        assertNull(WebAccess.corsOrigin(null, allowed));
        assertNull(WebAccess.corsOrigin("https://build.example.com", List.of()));
    }
}
