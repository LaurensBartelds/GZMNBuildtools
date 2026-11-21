package nl.gzmn.gZMNBuildtools.modules.webeditor.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/** Access control for the web editor's HTTP API: an optional shared access code and CORS allow-list. */
public final class WebAccess {

    private WebAccess() {
    }

    /**
     * Whether a request may proceed: no configured code means open (the personal
     * session tokens still guard brush changes); otherwise the provided code
     * must match, compared in constant time.
     */
    public static boolean codeAccepted(String configuredCode, String providedCode) {
        if (configuredCode == null || configuredCode.isBlank()) {
            return true;
        }
        String provided = providedCode == null ? "" : providedCode;
        return MessageDigest.isEqual(configuredCode.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }

    /** The request's Origin when it is in the allow-list, or null (no CORS header) otherwise. */
    public static String corsOrigin(String requestOrigin, List<String> allowedOrigins) {
        if (requestOrigin == null || requestOrigin.isBlank() || allowedOrigins == null || allowedOrigins.isEmpty()) {
            return null;
        }
        return allowedOrigins.contains(requestOrigin) ? requestOrigin : null;
    }
}
