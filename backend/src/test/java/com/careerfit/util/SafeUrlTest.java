package com.careerfit.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SafeUrlTest {
    @Test
    void acceptsHttpAndHttps() {
        assertEquals("https://example.com/apply", SafeUrl.sanitize(" https://example.com/apply "));
        assertEquals("http://example.com", SafeUrl.sanitize("http://example.com"));
    }

    @Test
    void rejectsDangerousOrMalformedUrls() {
        assertNull(SafeUrl.sanitize("javascript:alert(1)"));
        assertNull(SafeUrl.sanitize("data:text/html,<script>1</script>"));
        assertNull(SafeUrl.sanitize("ftp://example.com"));
        assertNull(SafeUrl.sanitize("//evil.com/x"));
        assertNull(SafeUrl.sanitize("not a url"));
        assertNull(SafeUrl.sanitize(""));
        assertNull(SafeUrl.sanitize(null));
    }
}
