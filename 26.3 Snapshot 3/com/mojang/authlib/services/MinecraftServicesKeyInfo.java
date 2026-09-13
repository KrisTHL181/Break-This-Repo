package com.mojang.authlib.services;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;
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
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftServicesKeyInfo implements ServicesKeyInfo {
   private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftServicesKeyInfo.class);
   private static final int KEY_SIZE_BITS = 4096;
   private static final String KEY_ALGORITHM = "RSA";
   private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
   private final PublicKey publicKey;

   private MinecraftServicesKeyInfo(PublicKey publicKey) {
      this.publicKey = publicKey;
      String algorithm = publicKey.getAlgorithm();
      if (!algorithm.equals("RSA")) {
         throw new IllegalArgumentException("Expected RSA key, got " + algorithm);
      }
   }

   public static ServicesKeyInfo parse(byte[] keyBytes) {
      try {
         X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
         KeyFactory keyFactory = KeyFactory.getInstance("RSA");
         PublicKey publicKey = keyFactory.generatePublic(spec);
         return new MinecraftServicesKeyInfo(publicKey);
      } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
         throw new IllegalArgumentException("Invalid Minecraft Services public key!", e);
      }
   }

   private static List<ServicesKeyInfo> parseList(@Nullable List<MinecraftServicesKeyInfo.KeyData> keys) {
      return keys == null ? List.of() : keys.stream().map(data -> parse(data.publicKey.array())).toList();
   }

   public static ServicesKeySet get(Supplier<URL> urlSupplier, MinecraftClient client) {
      return ServicesKeySet.lazy(
         RetryableFetch.fetch(urlSupplier, MinecraftServicesDiscoveryService.DISCOVERY_EXECUTOR, url -> fetch(url, client), ServicesKeySet.EMPTY, 24, 5)
      );
   }

   private static Optional<ServicesKeySet> fetch(URL url, MinecraftClient client) {
      MinecraftServicesKeyInfo.KeySetResponse response;
      try {
         response = client.get(url, MinecraftServicesKeyInfo.KeySetResponse.class);
      } catch (MinecraftClientException e) {
         LOGGER.error("Failed to request Minecraft Services public key", e);
         return Optional.empty();
      }

      if (response == null) {
         return Optional.empty();
      }

      try {
         List<ServicesKeyInfo> profilePropertyKeys = parseList(response.profilePropertyKeys);
         List<ServicesKeyInfo> playerCertificateKeys = parseList(response.playerCertificateKeys);
         return Optional.of(type -> {
            return switch (type) {
               case PROFILE_PROPERTY -> profilePropertyKeys;
               case PROFILE_KEY -> playerCertificateKeys;
            };
         });
      } catch (Exception e) {
         LOGGER.error("Received malformed Minecraft Services public key data", e);
         return Optional.empty();
      }
   }

   @Override
   public Signature signature() {
      try {
         Signature signature = Signature.getInstance("SHA1withRSA");
         signature.initVerify(this.publicKey);
         return signature;
      } catch (NoSuchAlgorithmException | InvalidKeyException e) {
         throw new AssertionError("Failed to create signature", e);
      }
   }

   @Override
   public int keyBitCount() {
      return 4096;
   }

   @Override
   public boolean validateProperty(Property property) {
      Signature signature = this.signature();

      byte[] expected;
      try {
         expected = Base64.getDecoder().decode(property.signature());
      } catch (IllegalArgumentException e) {
         LOGGER.error("Malformed signature encoding on property {}", property, e);
         return false;
      }

      try {
         signature.update(property.value().getBytes());
         return signature.verify(expected);
      } catch (SignatureException e) {
         LOGGER.error("Failed to verify signature on property {}", property, e);
         return false;
      }
   }

   private record KeyData(@SerializedName("publicKey") ByteBuffer publicKey) {
   }

   private record KeySetResponse(
      @Nullable @SerializedName("profilePropertyKeys") List<MinecraftServicesKeyInfo.KeyData> profilePropertyKeys,
      @Nullable @SerializedName("playerCertificateKeys") List<MinecraftServicesKeyInfo.KeyData> playerCertificateKeys
   ) {
   }
}
