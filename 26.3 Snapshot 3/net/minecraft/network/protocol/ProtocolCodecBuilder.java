package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.codec.IdDispatchCodec;
import net.minecraft.network.codec.StreamCodec;

public class ProtocolCodecBuilder<B extends ByteBuf, L extends PacketListener> {
   private final IdDispatchCodec.Builder<B, Packet<? super L>, PacketType<? extends Packet<? super L>>> dispatchBuilder = IdDispatchCodec.builder(Packet::type);
   private final PacketFlow flow;

   public ProtocolCodecBuilder(final PacketFlow flow) {
      this.flow = flow;
   }

   public <T extends Packet<? super L>> ProtocolCodecBuilder<B, L> add(final PacketType<T> type, final StreamCodec<? super B, T> serializer) {
      if (type.flow() != this.flow) {
         throw new IllegalArgumentException("Invalid packet flow for packet " + type + ", expected " + this.flow.name());
      }

      this.dispatchBuilder.add(type, serializer);
      return this;
   }

   public StreamCodec<B, Packet<? super L>> build() {
      return this.dispatchBuilder.build();
   }
}
