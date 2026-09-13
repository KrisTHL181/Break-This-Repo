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
      p_253722_ -> p_253722_.group(
            Codec.STRING.fieldOf("content").forGetter(SignedMessageBody::content),
            ExtraCodecs.INSTANT_ISO8601.fieldOf("time_stamp").forGetter(SignedMessageBody::timeStamp),
            Codec.LONG.fieldOf("salt").forGetter(SignedMessageBody::salt),
            LastSeenMessages.CODEC.optionalFieldOf("last_seen", LastSeenMessages.EMPTY).forGetter(SignedMessageBody::lastSeen)
         )
         .apply(p_253722_, SignedMessageBody::new)
   );

   public static SignedMessageBody unsigned(String p_249884_) {
      return new SignedMessageBody(p_249884_, Instant.now(), 0L, LastSeenMessages.EMPTY);
   }

   public void updateSignature(SignatureUpdater.Output p_249654_) throws SignatureException {
      p_249654_.update(Longs.toByteArray(this.salt));
      p_249654_.update(Longs.toByteArray(this.timeStamp.getEpochSecond()));
      byte[] abyte = this.content.getBytes(StandardCharsets.UTF_8);
      p_249654_.update(Ints.toByteArray(abyte.length));
      p_249654_.update(abyte);
      this.lastSeen.updateSignature(p_249654_);
   }

   public SignedMessageBody.Packed pack(MessageSignatureCache p_253671_) {
      return new SignedMessageBody.Packed(this.content, this.timeStamp, this.salt, this.lastSeen.pack(p_253671_));
   }

   public record Packed(String content, Instant timeStamp, long salt, LastSeenMessages.Packed lastSeen) {
      public Packed(FriendlyByteBuf p_251620_) {
         this(p_251620_.readUtf(256), p_251620_.readInstant(), p_251620_.readLong(), new LastSeenMessages.Packed(p_251620_));
      }

      public void write(FriendlyByteBuf p_250247_) {
         p_250247_.writeUtf(this.content, 256);
         p_250247_.writeInstant(this.timeStamp);
         p_250247_.writeLong(this.salt);
         this.lastSeen.write(p_250247_);
      }

      public Optional<SignedMessageBody> unpack(MessageSignatureCache p_253919_) {
         return this.lastSeen.unpack(p_253919_).map(p_249065_ -> new SignedMessageBody(this.content, this.timeStamp, this.salt, p_249065_));
      }
   }
}
