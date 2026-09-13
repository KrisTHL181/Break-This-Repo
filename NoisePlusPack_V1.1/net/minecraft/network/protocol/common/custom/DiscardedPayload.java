package net.minecraft.network.protocol.common.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record DiscardedPayload(Identifier id) implements CustomPacketPayload {
   public static <T extends FriendlyByteBuf> StreamCodec<T, DiscardedPayload> codec(Identifier p_459293_, int p_334650_) {
      return CustomPacketPayload.codec((p_330619_, p_329210_) -> {}, p_448782_ -> {
         int i = p_448782_.readableBytes();
         if (i >= 0 && i <= p_334650_) {
            p_448782_.skipBytes(i);
            return new DiscardedPayload(p_459293_);
         } else {
            throw new IllegalArgumentException("Payload may not be larger than " + p_334650_ + " bytes");
         }
      });
   }

   @Override
   public CustomPacketPayload.Type<DiscardedPayload> type() {
      return new CustomPacketPayload.Type<>(this.id);
   }
}
