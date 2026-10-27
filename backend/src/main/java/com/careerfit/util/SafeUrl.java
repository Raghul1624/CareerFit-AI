package com.careerfit.util;

import java.net.URI;
import java.net.URISyntaxException;

public final class SafeUrl {
    private SafeUrl() {}

    /** Returns the URL only if it is an absolute http(s) URL with a host; otherwise null. */
    public static String sanitize(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) return null;
            if (uri.getHost() == null) return null;
            return uri.toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
