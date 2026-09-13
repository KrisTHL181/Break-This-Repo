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

   public static ProfilePublicKey createValidated(SignatureValidator p_243373_, UUID p_243390_, ProfilePublicKey.Data p_243374_) throws ProfilePublicKey.ValidationException {
      if (!p_243374_.validateSignature(p_243373_, p_243390_)) {
         throw new ProfilePublicKey.ValidationException(INVALID_SIGNATURE);
      } else {
         return new ProfilePublicKey(p_243374_);
      }
   }

   public SignatureValidator createSignatureValidator() {
      return SignatureValidator.from(this.data.key, "SHA256withRSA");
   }

   public record Data(Instant expiresAt, PublicKey key, byte[] keySignature) {
      private static final int MAX_KEY_SIGNATURE_SIZE = 4096;
      public static final Codec<ProfilePublicKey.Data> CODEC = RecordCodecBuilder.create(
         p_219814_ -> p_219814_.group(
               ExtraCodecs.INSTANT_ISO8601.fieldOf("expires_at").forGetter(ProfilePublicKey.Data::expiresAt),
               Crypt.PUBLIC_KEY_CODEC.fieldOf("key").forGetter(ProfilePublicKey.Data::key),
               ExtraCodecs.BASE64_STRING.fieldOf("signature_v2").forGetter(ProfilePublicKey.Data::keySignature)
            )
            .apply(p_219814_, ProfilePublicKey.Data::new)
      );

      public Data(FriendlyByteBuf p_219809_) {
         this(p_219809_.readInstant(), p_219809_.readPublicKey(), p_219809_.readByteArray(4096));
      }

      public void write(FriendlyByteBuf p_219816_) {
         p_219816_.writeInstant(this.expiresAt);
         p_219816_.writePublicKey(this.key);
         p_219816_.writeByteArray(this.keySignature);
      }

      boolean validateSignature(SignatureValidator p_240296_, UUID p_240297_) {
         return p_240296_.validate(this.signedPayload(p_240297_), this.keySignature);
      }

      private byte[] signedPayload(UUID p_240267_) {
         byte[] abyte = this.key.getEncoded();
         byte[] abyte1 = new byte[24 + abyte.length];
         ByteBuffer bytebuffer = ByteBuffer.wrap(abyte1).order(ByteOrder.BIG_ENDIAN);
         bytebuffer.putLong(p_240267_.getMostSignificantBits()).putLong(p_240267_.getLeastSignificantBits()).putLong(this.expiresAt.toEpochMilli()).put(abyte);
         return abyte1;
      }

      public boolean hasExpired() {
         return this.expiresAt.isBefore(Instant.now());
      }

      public boolean hasExpired(Duration p_243376_) {
         return this.expiresAt.plus(p_243376_).isBefore(Instant.now());
      }

      @Override
      public boolean equals(Object p_219822_) {
         return p_219822_ instanceof ProfilePublicKey.Data profilepublickey$data
            ? this.expiresAt.equals(profilepublickey$data.expiresAt)
               && this.key.equals(profilepublickey$data.key)
               && Arrays.equals(this.keySignature, profilepublickey$data.keySignature)
            : false;
      }
   }

   public static class ValidationException extends ThrowingComponent {
      public ValidationException(Component p_243378_) {
         super(p_243378_);
      }
   }
}
