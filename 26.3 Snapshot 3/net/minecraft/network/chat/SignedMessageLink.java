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
      i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("index").forGetter(SignedMessageLink::index),
            UUIDUtil.CODEC.fieldOf("sender").forGetter(SignedMessageLink::sender),
            UUIDUtil.CODEC.fieldOf("session_id").forGetter(SignedMessageLink::sessionId)
         )
         .apply(i, SignedMessageLink::new)
   );

   public static SignedMessageLink unsigned(final UUID sender) {
      return root(sender, Util.NIL_UUID);
   }

   public static SignedMessageLink root(final UUID sender, final UUID sessionId) {
      return new SignedMessageLink(0, sender, sessionId);
   }

   public void updateSignature(final SignatureUpdater.Output output) throws SignatureException {
      output.update(UUIDUtil.uuidToByteArray(this.sender));
      output.update(UUIDUtil.uuidToByteArray(this.sessionId));
      output.update(Ints.toByteArray(this.index));
   }

   public boolean isDescendantOf(final SignedMessageLink link) {
      return this.index > link.index() && this.sender.equals(link.sender()) && this.sessionId.equals(link.sessionId());
   }

   public @Nullable SignedMessageLink advance() {
      return this.index == Integer.MAX_VALUE ? null : new SignedMessageLink(this.index + 1, this.sender, this.sessionId);
   }
}
