package net.minecraft.network.chat;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.SignatureUpdater;
import net.minecraft.util.SignatureValidator;
import org.jspecify.annotations.Nullable;

public record MessageSignature(byte[] bytes) {
   public static final Codec<MessageSignature> CODEC = ExtraCodecs.BASE64_STRING.xmap(MessageSignature::new, MessageSignature::bytes);
   public static final int BYTES = 256;

   public MessageSignature {
      Preconditions.checkState(bytes.length == 256, "Invalid message signature size");
   }

   public static MessageSignature read(final FriendlyByteBuf input) {
      byte[] bytes = new byte[256];
      input.readBytes(bytes);
      return new MessageSignature(bytes);
   }

   public static void write(final FriendlyByteBuf output, final MessageSignature signature) {
      output.writeBytes(signature.bytes);
   }

   public boolean verify(final SignatureValidator signature, final SignatureUpdater updater) {
      return signature.validate(updater, this.bytes);
   }

   public ByteBuffer asByteBuffer() {
      return ByteBuffer.wrap(this.bytes);
   }

   @Override
   public boolean equals(final Object o) {
      return this == o || o instanceof MessageSignature that && Arrays.equals(this.bytes, that.bytes);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.bytes);
   }

   @Override
   public String toString() {
      return Base64.getEncoder().encodeToString(this.bytes);
   }

   public static String describe(final @Nullable MessageSignature signature) {
      return signature == null ? "<no signature>" : signature.toString();
   }

   public MessageSignature.Packed pack(final MessageSignatureCache cache) {
      int packedId = cache.pack(this);
      return packedId != -1 ? new MessageSignature.Packed(packedId) : new MessageSignature.Packed(this);
   }

   public int checksum() {
      return Arrays.hashCode(this.bytes);
   }

   public record Packed(int id, @Nullable MessageSignature fullSignature) {
      public static final int FULL_SIGNATURE = -1;

      public Packed(final MessageSignature signature) {
         this(-1, signature);
      }

      public Packed(final int id) {
         this(id, null);
      }

      public static MessageSignature.Packed read(final FriendlyByteBuf input) {
         int id = input.readVarInt() - 1;
         return id == -1 ? new MessageSignature.Packed(MessageSignature.read(input)) : new MessageSignature.Packed(id);
      }

      public static void write(final FriendlyByteBuf output, final MessageSignature.Packed packed) {
         output.writeVarInt(packed.id() + 1);
         if (packed.fullSignature() != null) {
            MessageSignature.write(output, packed.fullSignature());
         }
      }

      public Optional<MessageSignature> unpack(final MessageSignatureCache cache) {
         return this.fullSignature != null ? Optional.of(this.fullSignature) : Optional.ofNullable(cache.unpack(this.id));
      }
   }
}
