package com.mojang.authlib.minecraft.client;

import com.google.common.net.HttpHeaders;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientException.ErrorType;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.yggdrasil.response.ErrorResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 *
 * Client to use when communicating with Minecraft service API.
 *
 *
 */
public class MinecraftClient {
    public static final int CONNECT_TIMEOUT_MS = 5000;
    public static final int READ_TIMEOUT_MS = 5000;
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftClient.class);
    private static final int HTTP_NOT_MODIFIED = 304;

    @Nullable
    private final String accessToken;
    private final Proxy proxy;
    private final ObjectMapper objectMapper = ObjectMapper.create();

    public MinecraftClient(@Nullable final String accessToken, final Proxy proxy) {
        this.accessToken = accessToken;
        this.proxy = Validate.notNull(proxy);
    }

    public static MinecraftClient unauthenticated(final Proxy proxy) {
        return new MinecraftClient(null, proxy);
    }

    @Nullable
    private static Duration parseRetryAfter(@Nullable final String headerValue) {
        if (headerValue == null) {
            return null;
        }
        try {
            final long seconds = Long.parseLong(headerValue.trim());
            return seconds > 0 ? Duration.ofSeconds(seconds) : null;
        } catch (final NumberFormatException e) {
            LOGGER.debug("Ignoring malformed {} header: {}", HttpHeaders.RETRY_AFTER, headerValue);
            return null;
        }
    }

    @Nullable
    public <T> T get(final URL url, final Class<T> responseClass) {
        return getWithEtag(url, responseClass, null).body();
    }

    /**
     * Performs a conditional GET, sending {@code If-None-Match: cachedEtag} when a cached ETag is
     * provided. When the server responds with 304 Not Modified the returned
     * {@link ServiceResponse#body()} is {@code null} — the caller should keep using its cached
     * value. The {@link ServiceResponse#etag()} always reflects the latest ETag (or the supplied
     * {@code cachedEtag} on a 304).
     *
     * @param cachedEtag the ETag value from a previous successful response, or {@code null}
     */
    public <T> ServiceResponse<T> getWithEtag(final URL url, final Class<T> responseClass, @Nullable final String cachedEtag) {
        Validate.notNull(url);
        Validate.notNull(responseClass);
        final HttpURLConnection connection = prepareRequest(url, cachedEtag);
        return readServiceResponse(url, responseClass, connection, cachedEtag);
    }

    @Nullable
    public <T> T post(final URL url, final Class<T> responseClass) {
        Validate.notNull(url);
        Validate.notNull(responseClass);
        final HttpURLConnection connection = withBody(prepareRequest(url, null), "POST", new byte[0]);
        return readServiceResponse(url, responseClass, connection, null).body();
    }

    @Nullable
    public <T> T post(final URL url, final Object body, final Class<T> responseClass) {
        return postWithEtag(url, body, responseClass, null).body();
    }

    public <T> ServiceResponse<T> postWithEtag(final URL url, final Object body, final Class<T> responseClass, @Nullable final String cachedEtag) {
        Validate.notNull(url);
        Validate.notNull(body);
        Validate.notNull(responseClass);
        final HttpURLConnection connection = withBody(prepareRequest(url, cachedEtag), "POST", serialize(body));
        return readServiceResponse(url, responseClass, connection, cachedEtag);
    }

    @Nullable
    public <T> T put(final URL url, final Object body, final Class<T> responseClass) {
        Validate.notNull(url);
        Validate.notNull(body);
        Validate.notNull(responseClass);
        final HttpURLConnection connection = withBody(prepareRequest(url, null), "PUT", serialize(body));
        return readServiceResponse(url, responseClass, connection, null).body();
    }

    @Nullable
    public <T> T delete(final URL url, final Class<T> responseClass) {
        Validate.notNull(url);
        Validate.notNull(responseClass);
        final HttpURLConnection connection = prepareRequest(url, null);
        try {
            connection.setRequestMethod("DELETE");
        } catch (final IOException io) {
            throw new MinecraftClientException(ErrorType.SERVICE_UNAVAILABLE, "Failed to DELETE " + url, io);
        }
        return readServiceResponse(url, responseClass, connection, null).body();
    }

    private <T> ServiceResponse<T> readServiceResponse(final URL url, final Class<T> clazz, final HttpURLConnection connection, @Nullable final String cachedEtag) {
        InputStream inputStream = null;
        try {
            final int status = connection.getResponseCode();
            final Duration retryAfter = parseRetryAfter(connection.getHeaderField(HttpHeaders.RETRY_AFTER));

            if (status == HTTP_NOT_MODIFIED) {
                return new ServiceResponse<>(null, cachedEtag, retryAfter);
            }

            final String responseEtag = connection.getHeaderField(HttpHeaders.ETAG);

            if (status < 400) {
                inputStream = connection.getInputStream();
                final String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                if (result.isEmpty() || clazz == Void.class) {
                    return new ServiceResponse<>(null, responseEtag, retryAfter);
                }
                return new ServiceResponse<>(objectMapper.readValue(result, clazz), responseEtag, retryAfter);
            } else {
                throw buildHttpException(url, connection, status, retryAfter);
            }
        } catch (final IOException e) {
            throw new MinecraftClientException(
                    ErrorType.SERVICE_UNAVAILABLE, "Failed to read from " + url + " due to " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private MinecraftClientHttpException buildHttpException(final URL url, final HttpURLConnection connection, final int status, @Nullable final Duration retryAfter) throws IOException {
        final InputStream errorStream = connection.getErrorStream();
        if (errorStream == null) {
            return new MinecraftClientHttpException(status, null, retryAfter);
        }
        try {
            final String contentType = connection.getContentType();
            final String result = IOUtils.toString(errorStream, StandardCharsets.UTF_8);
            if (contentType != null && contentType.startsWith("text/html")) {
                LOGGER.error("Got an error with a html body connecting to {}: {}", url, result);
                return new MinecraftClientHttpException(status, null, retryAfter);
            }
            final ErrorResponse errorResponse = objectMapper.readValue(result, ErrorResponse.class);
            return new MinecraftClientHttpException(status, errorResponse, retryAfter);
        } finally {
            IOUtils.closeQuietly(errorStream);
        }
    }

    private HttpURLConnection prepareRequest(final URL url, @Nullable final String cachedEtag) {
        final HttpURLConnection connection = createUrlConnection(url);
        if (accessToken != null) {
            connection.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        if (cachedEtag != null) {
            connection.setRequestProperty(HttpHeaders.IF_NONE_MATCH, cachedEtag);
        }
        return connection;
    }

    private HttpURLConnection withBody(final HttpURLConnection connection, final String method, final byte[] body) {
        OutputStream outputStream = null;
        try {
            connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
            connection.setRequestProperty(HttpHeaders.CONTENT_LENGTH, Integer.toString(body.length));
            connection.setRequestMethod(method);
            connection.setDoOutput(true);
            outputStream = connection.getOutputStream();
            IOUtils.write(body, outputStream);
        } catch (final IOException io) {
            throw new MinecraftClientException(ErrorType.SERVICE_UNAVAILABLE, "Failed to " + method + " " + connection.getURL(), io);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
        return connection;
    }

    private byte[] serialize(final Object body) {
        return objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
    }

    private HttpURLConnection createUrlConnection(final URL url) {
        try {
            LOGGER.debug("Connecting to {}", url);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            return connection;
        } catch (final IOException io) {
            throw new MinecraftClientException(ErrorType.SERVICE_UNAVAILABLE, "Failed connecting to " + url, io);
        }
    }

    /**
     * Wraps a parsed response together with the ETag and Retry-After returned by the server.
     * {@code etag} is {@code null} when the server did not send one.
     * {@code body} is {@code null} when the server replied 304 Not Modified
     * (the caller should reuse its cached value).
     * {@code retryAfter} is {@code null} when the server did not send a {@code Retry-After} header
     * (the caller should fall back to its own default cadence).
     */
    public record ServiceResponse<T>(
            @Nullable T body,
            @Nullable String etag,
            @Nullable Duration retryAfter
    ) {
    }
}
