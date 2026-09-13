package net.minecraft.world.entity.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;
import net.minecraft.util.Crypt;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.SignatureValidator;

public record ProfilePublicKey(ProfilePublicKey.Data data) {
   public static final Component EXPIRED_PROFILE_PUBLIC_KEY = Component.translatable("multiplayer.disconnect.expired_public_key");
   private static final Component INVALID_SIGNATURE = Component.translatable("multiplayer.disconnect.invalid_public_key_signature");
   public static final Duration EXPIRY_GRACE_PERIOD = Duration.ofHours(8L);
   public static final Codec<ProfilePublicKey> TRUSTED_CODEC = ProfilePublicKey.Data.CODEC.xmap(ProfilePublicKey::new, ProfilePublicKey::data);

   public static ProfilePublicKey createValidated(final SignatureValidator validator, final UUID profileId, final ProfilePublicKey.Data data) throws ProfilePublicKey.ValidationException {
      if (!data.validateSignature(validator, profileId)) {
         throw new ProfilePublicKey.ValidationException(INVALID_SIGNATURE);
      } else {
         return new ProfilePublicKey(data);
      }
   }

   public SignatureValidator createSignatureValidator() {
      return SignatureValidator.from(this.data.key, "SHA256withRSA");
   }

   public record Data(Instant expiresAt, PublicKey key, byte[] keySignature) {
      private static final int MAX_KEY_SIGNATURE_SIZE = 4096;
      public static final Codec<ProfilePublicKey.Data> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               ExtraCodecs.INSTANT_ISO8601.fieldOf("expires_at").forGetter(ProfilePublicKey.Data::expiresAt),
               Crypt.PUBLIC_KEY_CODEC.fieldOf("key").forGetter(ProfilePublicKey.Data::key),
               ExtraCodecs.BASE64_STRING.fieldOf("signature_v2").forGetter(ProfilePublicKey.Data::keySignature)
            )
            .apply(i, ProfilePublicKey.Data::new)
      );

      public Data(final FriendlyByteBuf input) {
         this(input.readInstant(), input.readPublicKey(), input.readByteArray(4096));
      }

      public void write(final FriendlyByteBuf output) {
         output.writeInstant(this.expiresAt);
         output.writePublicKey(this.key);
         output.writeByteArray(this.keySignature);
      }

      private boolean validateSignature(final SignatureValidator validator, final UUID profileId) {
         return validator.validate(this.signedPayload(profileId), this.keySignature);
      }

      private byte[] signedPayload(final UUID profileId) {
         byte[] keyBytes = this.key.getEncoded();
         byte[] signedPayload = new byte[24 + keyBytes.length];
         ByteBuffer buffer = ByteBuffer.wrap(signedPayload).order(ByteOrder.BIG_ENDIAN);
         buffer.putLong(profileId.getMostSignificantBits()).putLong(profileId.getLeastSignificantBits()).putLong(this.expiresAt.toEpochMilli()).put(keyBytes);
         return signedPayload;
      }

      public boolean hasExpired() {
         return this.expiresAt.isBefore(Instant.now());
      }

      public boolean hasExpired(final Duration gracePeriod) {
         return this.expiresAt.plus(gracePeriod).isBefore(Instant.now());
      }

      @Override
      public boolean equals(final Object o) {
         return !(o instanceof ProfilePublicKey.Data data)
            ? false
            : this.expiresAt.equals(data.expiresAt) && this.key.equals(data.key) && Arrays.equals(this.keySignature, data.keySignature);
      }
   }

   public static class ValidationException extends ThrowingComponent {
      public ValidationException(final Component component) {
         super(component);
      }
   }
}
