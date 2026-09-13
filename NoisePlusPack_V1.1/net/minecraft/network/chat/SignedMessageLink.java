package net.minecraft.network.chat;

import com.google.common.primitives.Ints;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.security.SignatureException;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.SignatureUpdater;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public record SignedMessageLink(int index, UUID sender, UUID sessionId) {
   public static final Codec<SignedMessageLink> CODEC = RecordCodecBuilder.create(
      p_253768_ -> p_253768_.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("index").forGetter(SignedMessageLink::index),
            UUIDUtil.CODEC.fieldOf("sender").forGetter(SignedMessageLink::sender),
            UUIDUtil.CODEC.fieldOf("session_id").forGetter(SignedMessageLink::sessionId)
         )
         .apply(p_253768_, SignedMessageLink::new)
   );

   public static SignedMessageLink unsigned(UUID p_251496_) {
      return root(p_251496_, Util.NIL_UUID);
   }

   public static SignedMessageLink root(UUID p_249990_, UUID p_248913_) {
      return new SignedMessageLink(0, p_249990_, p_248913_);
   }

   public void updateSignature(SignatureUpdater.Output p_249261_) throws SignatureException {
      p_249261_.update(UUIDUtil.uuidToByteArray(this.sender));
      p_249261_.update(UUIDUtil.uuidToByteArray(this.sessionId));
      p_249261_.update(Ints.toByteArray(this.index));
   }

   public boolean isDescendantOf(SignedMessageLink p_250977_) {
      return this.index > p_250977_.index() && this.sender.equals(p_250977_.sender()) && this.sessionId.equals(p_250977_.sessionId());
   }

   public @Nullable SignedMessageLink advance() {
      return this.index == Integer.MAX_VALUE ? null : new SignedMessageLink(this.index + 1, this.sender, this.sessionId);
   }
}
