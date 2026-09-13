package com.mojang.realmsclient.client;

import com.mojang.realmsclient.exception.RealmsHttpException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class Request<T extends Request<T>> {
    protected HttpURLConnection connection;
    private boolean connected;
    protected String url;
    private static final int DEFAULT_READ_TIMEOUT = 60000;
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final String NETWORK_PROTOCOL_KEY = "X-NetworkProtocolVersion";
    private static final String IS_SNAPSHOT_KEY = "Is-Prerelease";
    private static final String COOKIE_KEY = "Cookie";

    public Request(final String url, final int connectTimeout, final int readTimeout) {
        try {
            this.url = url;
            Proxy proxy = RealmsClientConfig.getProxy();
            if (proxy != null) {
                this.connection = (HttpURLConnection)new URL(url).openConnection(proxy);
            } else {
                this.connection = (HttpURLConnection)new URL(url).openConnection();
            }

            this.connection.setConnectTimeout(connectTimeout);
            this.connection.setReadTimeout(readTimeout);
        } catch (MalformedURLException e) {
            throw new RealmsHttpException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RealmsHttpException(e.getMessage(), e);
        }
    }

    public void cookie(final String key, final String value) {
        cookie(this.connection, key, value);
    }

    public static void cookie(final HttpURLConnection connection, final String key, final String value) {
        String cookie = connection.getRequestProperty("Cookie");
        if (cookie == null) {
            connection.setRequestProperty("Cookie", key + "=" + value);
        } else {
            connection.setRequestProperty("Cookie", cookie + ";" + key + "=" + value);
        }
    }

    public void addVersionHeaders(final int networkProtocol, final boolean isSnapshot) {
        this.connection.addRequestProperty("X-NetworkProtocolVersion", String.valueOf(networkProtocol));
        this.connection.addRequestProperty("Is-Prerelease", String.valueOf(isSnapshot));
    }

    public int getRetryAfterHeader() {
        return getRetryAfterHeader(this.connection);
    }

    public static int getRetryAfterHeader(final HttpURLConnection connection) {
        String pauseTime = connection.getHeaderField("Retry-After");

        try {
            return Integer.valueOf(pauseTime);
        } catch (Exception ignored) {
            return 5;
        }
    }

    public int responseCode() {
        try {
            this.connect();
            return this.connection.getResponseCode();
        } catch (Exception e) {
            throw new RealmsHttpException(e.getMessage(), e);
        }
    }

    public String text() {
        try {
            this.connect();
            String result;
            if (this.responseCode() >= 400) {
                result = this.read(this.connection.getErrorStream());
            } else {
                result = this.read(this.connection.getInputStream());
            }

            this.dispose();
            return result;
        } catch (IOException e) {
            throw new RealmsHttpException(e.getMessage(), e);
        }
    }

    private String read(final @Nullable InputStream in) throws IOException {
        if (in == null) {
            return "";
        }

        InputStreamReader streamReader = new InputStreamReader(in, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();

        for (int x = streamReader.read(); x != -1; x = streamReader.read()) {
            sb.append((char)x);
        }

        return sb.toString();
    }

    private void dispose() {
        byte[] bytes = new byte[1024];

        try {
            InputStream in = this.connection.getInputStream();

            while (in.read(bytes) > 0) {
            }

            in.close();
            return;
        } catch (Exception ignore) {
            try {
                InputStream errorStream = this.connection.getErrorStream();
                if (errorStream != null) {
                    while (errorStream.read(bytes) > 0) {
                    }

                    errorStream.close();
                    return;
                }
            } catch (IOException var8) {
                return;
            }
        } finally {
            if (this.connection != null) {
                this.connection.disconnect();
            }
        }
    }

    protected T connect() {
        if (this.connected) {
            return (T)this;
        }

        T t = this.doConnect();
        this.connected = true;
        return t;
    }

    protected abstract T doConnect();

    public static Request<?> get(final String url) {
        return new Request.Get(url, 5000, 60000);
    }

    public static Request<?> get(final String url, final int connectTimeoutMillis, final int readTimeoutMillis) {
        return new Request.Get(url, connectTimeoutMillis, readTimeoutMillis);
    }

    public static Request<?> post(final String uri, final String content) {
        return new Request.Post(uri, content, 5000, 60000);
    }

    public static Request<?> post(final String uri, final String content, final int connectTimeoutMillis, final int readTimeoutMillis) {
        return new Request.Post(uri, content, connectTimeoutMillis, readTimeoutMillis);
    }

    public static Request<?> delete(final String url) {
        return new Request.Delete(url, 5000, 60000);
    }

    public static Request<?> put(final String url, final String content) {
        return new Request.Put(url, content, 5000, 60000);
    }

    public static Request<?> put(final String url, final String content, final int connectTimeoutMillis, final int readTimeoutMillis) {
        return new Request.Put(url, content, connectTimeoutMillis, readTimeoutMillis);
    }

    public String getHeader(final String header) {
        return getHeader(this.connection, header);
    }

    public static String getHeader(final HttpURLConnection connection, final String header) {
        try {
            return connection.getHeaderField(header);
        } catch (Exception ignored) {
            return "";
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Delete extends Request<Request.Delete> {
        public Delete(final String uri, final int connectTimeout, final int readTimeout) {
            super(uri, connectTimeout, readTimeout);
        }

        public Request.Delete doConnect() {
            try {
                this.connection.setDoOutput(true);
                this.connection.setRequestMethod("DELETE");
                this.connection.connect();
                return this;
            } catch (Exception e) {
                throw new RealmsHttpException(e.getMessage(), e);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Get extends Request<Request.Get> {
        public Get(final String uri, final int connectTimeout, final int readTimeout) {
            super(uri, connectTimeout, readTimeout);
        }

        public Request.Get doConnect() {
            try {
                this.connection.setDoInput(true);
                this.connection.setDoOutput(true);
                this.connection.setUseCaches(false);
                this.connection.setRequestMethod("GET");
                return this;
            } catch (Exception e) {
                throw new RealmsHttpException(e.getMessage(), e);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Post extends Request<Request.Post> {
        private final String content;

        public Post(final String uri, final String content, final int connectTimeout, final int readTimeout) {
            super(uri, connectTimeout, readTimeout);
            this.content = content;
        }

        public Request.Post doConnect() {
            try {
                if (this.content != null) {
                    this.connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                }

                this.connection.setDoInput(true);
                this.connection.setDoOutput(true);
                this.connection.setUseCaches(false);
                this.connection.setRequestMethod("POST");
                OutputStream out = this.connection.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                writer.write(this.content);
                writer.close();
                out.flush();
                return this;
            } catch (Exception e) {
                throw new RealmsHttpException(e.getMessage(), e);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Put extends Request<Request.Put> {
        private final String content;

        public Put(final String uri, final String content, final int connectTimeout, final int readTimeout) {
            super(uri, connectTimeout, readTimeout);
            this.content = content;
        }

        public Request.Put doConnect() {
            try {
                if (this.content != null) {
                    this.connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                }

                this.connection.setDoOutput(true);
                this.connection.setDoInput(true);
                this.connection.setRequestMethod("PUT");
                OutputStream out = this.connection.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                writer.write(this.content);
                writer.close();
                out.flush();
                return this;
            } catch (Exception e) {
                throw new RealmsHttpException(e.getMessage(), e);
            }
        }
    }
}