package net.minecraft.network.chat;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.SignatureUpdater;

public record SignedMessageBody(String content, Instant timeStamp, long salt, LastSeenMessages lastSeen) {
   public static final MapCodec<SignedMessageBody> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.STRING.fieldOf("content").forGetter(SignedMessageBody::content),
            ExtraCodecs.INSTANT_ISO8601.fieldOf("time_stamp").forGetter(SignedMessageBody::timeStamp),
            Codec.LONG.fieldOf("salt").forGetter(SignedMessageBody::salt),
            LastSeenMessages.CODEC.optionalFieldOf("last_seen", LastSeenMessages.EMPTY).forGetter(SignedMessageBody::lastSeen)
         )
         .apply(i, SignedMessageBody::new)
   );

   public static SignedMessageBody unsigned(final String content) {
      return new SignedMessageBody(content, Instant.now(), 0L, LastSeenMessages.EMPTY);
   }

   public void updateSignature(final SignatureUpdater.Output output) throws SignatureException {
      output.update(Longs.toByteArray(this.salt));
      output.update(Longs.toByteArray(this.timeStamp.getEpochSecond()));
      byte[] contentBytes = this.content.getBytes(StandardCharsets.UTF_8);
      output.update(Ints.toByteArray(contentBytes.length));
      output.update(contentBytes);
      this.lastSeen.updateSignature(output);
   }

   public SignedMessageBody.Packed pack(final MessageSignatureCache cache) {
      return new SignedMessageBody.Packed(this.content, this.timeStamp, this.salt, this.lastSeen.pack(cache));
   }

   public record Packed(String content, Instant timeStamp, long salt, LastSeenMessages.Packed lastSeen) {
      public Packed(final FriendlyByteBuf input) {
         this(input.readUtf(256), input.readInstant(), input.readLong(), new LastSeenMessages.Packed(input));
      }

      public void write(final FriendlyByteBuf output) {
         output.writeUtf(this.content, 256);
         output.writeInstant(this.timeStamp);
         output.writeLong(this.salt);
         this.lastSeen.write(output);
      }

      public Optional<SignedMessageBody> unpack(final MessageSignatureCache cache) {
         return this.lastSeen.unpack(cache).map(lastSeen -> new SignedMessageBody(this.content, this.timeStamp, this.salt, lastSeen));
      }
   }
}
