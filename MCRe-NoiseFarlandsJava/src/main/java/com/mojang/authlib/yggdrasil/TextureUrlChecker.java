package com.mojang.authlib.yggdrasil;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TextureUrlChecker {
    private static final Set<String> ALLOWED_SCHEMES = Set.of(
        "http",
        "https"
    );

    private static final Set<String> ALLOWED_DOMAINS = Set.of(
        "textures.minecraft.net"
    );

    public static boolean isAllowedTextureDomain(final String url) {
        final URI uri;

        try {
            uri = new URI(url).normalize();
        } catch (final URISyntaxException ignored) {
            return false;
        }

        final String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme)) {
            return false;
        }

        final String domain = uri.getHost();
        if (domain == null) {
            return false;
        }
        final String decodedDomain = IDN.toUnicode(domain);
        final String lowerCaseDomain = decodedDomain.toLowerCase(Locale.ROOT);
        if (!lowerCaseDomain.equals(decodedDomain)) {
            return false;
        }
        return ALLOWED_DOMAINS.contains(decodedDomain);
    }

}
