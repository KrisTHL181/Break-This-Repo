package com.mojang.authlib.yggdrasil;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class YggdrasilServicesKeyInfo implements ServicesKeyInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(YggdrasilServicesKeyInfo.class);

    private static final ScheduledExecutorService FETCHER_EXECUTOR = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder()
        .setNameFormat("Yggdrasil Key Fetcher")
        .setDaemon(true)
        .build()
    );

    // Don't expose those directly to avoid library clients inlining it
    private static final int KEY_SIZE_BITS = 4096;
    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    private static final int REFRESH_INTERVAL_HOURS = 24;
    private static final int BASE_FAILURE_INTERVAL_MINUTES = 5;
    private static final int MAX_BACKOFF_EXPONENT = 6;

    private final PublicKey publicKey;

    private YggdrasilServicesKeyInfo(final PublicKey publicKey) {
        this.publicKey = publicKey;
        final String algorithm = publicKey.getAlgorithm();
        if (!algorithm.equals(KEY_ALGORITHM)) {
            throw new IllegalArgumentException("Expected " + KEY_ALGORITHM + " key, got " + algorithm);
        }
    }

    public static ServicesKeyInfo parse(final byte[] keyBytes) {
        try {
            final X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            final KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            final PublicKey publicKey = keyFactory.generatePublic(spec);
            return new YggdrasilServicesKeyInfo(publicKey);
        } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid yggdrasil public key!", e);
        }
    }

    private static List<ServicesKeyInfo> parseList(@Nullable final List<KeyData> keys) {
        if (keys == null) {
            return List.of();
        }
        return keys.stream()
            .map(data -> parse(data.publicKey.array()))
            .toList();
    }

    public static ServicesKeySet get(final URL url, final MinecraftClient client) {
        final CompletableFuture<?> ready = new CompletableFuture<>();
        final AtomicReference<ServicesKeySet> keySet = new AtomicReference<>();
        FETCHER_EXECUTOR.execute(new Runnable() {
            private final AtomicInteger failureCount = new AtomicInteger();

            @Override
            public void run() {
                fetch(url, client).ifPresent(keySet::set);
                ready.complete(null);
                reschedule();
            }

            private void reschedule() {
                if (keySet.get() == null) {
                    final int backoffExponent = Math.min(failureCount.getAndIncrement(), MAX_BACKOFF_EXPONENT);
                    final int delayMinutes = BASE_FAILURE_INTERVAL_MINUTES * (1 << backoffExponent);
                    FETCHER_EXECUTOR.schedule(this, delayMinutes, TimeUnit.MINUTES);
                    return;
                }
                FETCHER_EXECUTOR.schedule(this, REFRESH_INTERVAL_HOURS, TimeUnit.HOURS);
            }
        });

        return ServicesKeySet.lazy(() -> {
            ready.join();
            return Objects.requireNonNullElse(keySet.get(), ServicesKeySet.EMPTY);
        });
    }

    private static Optional<ServicesKeySet> fetch(final URL url, final MinecraftClient client) {
        final KeySetResponse response;
        try {
            response = client.get(url, KeySetResponse.class);
        } catch (final MinecraftClientException e) {
            LOGGER.error("Failed to request yggdrasil public key", e);
            return Optional.empty();
        }

        if (response == null) {
            return Optional.empty();
        }

        try {
            final List<ServicesKeyInfo> profilePropertyKeys = parseList(response.profilePropertyKeys);
            final List<ServicesKeyInfo> playerCertificateKeys = parseList(response.playerCertificateKeys);
            return Optional.of(type -> switch (type) {
                case PROFILE_PROPERTY -> profilePropertyKeys;
                case PROFILE_KEY -> playerCertificateKeys;
            });
        } catch (final Exception e) {
            LOGGER.error("Received malformed yggdrasil public key data", e);
            return Optional.empty();
        }
    }

    @Override
    public Signature signature() {
        try {
            final Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            return signature;
        } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
            throw new AssertionError("Failed to create signature", e);
        }
    }

    @Override
    public int keyBitCount() {
        return KEY_SIZE_BITS;
    }

    @Override
    public boolean validateProperty(final Property property) {
        final Signature signature = signature();
        final byte[] expected;
        try {
            expected = Base64.getDecoder().decode(property.signature());
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Malformed signature encoding on property {}", property, e);
            return false;
        }
        try {
            signature.update(property.value().getBytes());
            return signature.verify(expected);
        } catch (final SignatureException e) {
            LOGGER.error("Failed to verify signature on property {}", property, e);
        }
        return false;
    }

    private record KeySetResponse(
        @SerializedName("profilePropertyKeys")
        @Nullable List<KeyData> profilePropertyKeys,
        @SerializedName("playerCertificateKeys")
        @Nullable List<KeyData> playerCertificateKeys
    ) {
    }

    private record KeyData(
        @SerializedName("publicKey")
        ByteBuffer publicKey
    ) {
    }
}
