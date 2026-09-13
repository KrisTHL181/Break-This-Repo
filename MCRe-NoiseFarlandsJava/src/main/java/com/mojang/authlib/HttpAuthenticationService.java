package com.mojang.authlib;

import org.apache.commons.lang3.Validate;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class HttpAuthenticationService implements AuthenticationService {
    private final Proxy proxy;

    protected HttpAuthenticationService(final Proxy proxy) {
        Validate.notNull(proxy);
        this.proxy = proxy;
    }

    /**
     * Gets the proxy to be used with every HTTP(S) request.
     *
     * @return Proxy to be used.
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * Creates a {@link URL} with the specified string, throwing an {@link Error} if the URL was malformed.
     * <p />
     * This is just a wrapper to allow URLs to be created in constants, where you know the URL is valid.
     *
     * @param url URL to construct
     * @return URL constructed
     */
    public static URL constantURL(final String url) {
        try {
            return new URL(url);
        } catch (final MalformedURLException ex) {
            throw new Error("Couldn't create constant for " + url, ex);
        }
    }

    /**
     * Turns the specified Map into an encoded & escaped query
     *
     * @param query Map to convert into a text based query
     * @return Resulting query.
     */
    public static String buildQuery(final Map<String, Object> query) {
        if (query == null) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();

        for (final Map.Entry<String, Object> entry : query.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }

            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));

            if (entry.getValue() != null) {
                builder.append('=');
                builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
            }
        }

        return builder.toString();
    }

    /**
     * Concatenates the given {@link URL} and query.
     *
     * @param url URL to base off
     * @param query Query to append to URL
     * @return URL constructed
     */
    public static URL concatenateURL(final URL url, final String query) {
        try {
            if (url.getQuery() != null && url.getQuery().length() > 0) {
                return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "&" + query);
            } else {
                return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "?" + query);
            }
        } catch (final MalformedURLException ex) {
            throw new IllegalArgumentException("Could not concatenate given URL with GET arguments!", ex);
        }
    }
}
