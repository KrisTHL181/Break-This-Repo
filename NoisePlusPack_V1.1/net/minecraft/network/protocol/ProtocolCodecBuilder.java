package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.codec.IdDispatchCodec;
import net.minecraft.network.codec.StreamCodec;

public class ProtocolCodecBuilder<B extends ByteBuf, L extends PacketListener> {
   private final IdDispatchCodec.Builder<B, Packet<? super L>, PacketType<? extends Packet<? super L>>> dispatchBuilder = IdDispatchCodec.builder(Packet::type);
   private final PacketFlow flow;

   public ProtocolCodecBuilder(PacketFlow p_334440_) {
      this.flow = p_334440_;
   }

   public <T extends Packet<? super L>> ProtocolCodecBuilder<B, L> add(PacketType<T> p_331162_, StreamCodec<? super B, T> p_335909_) {
      if (p_331162_.flow() != this.flow) {
         throw new IllegalArgumentException("Invalid packet flow for packet " + p_331162_ + ", expected " + this.flow.name());
      }

      this.dispatchBuilder.add(p_331162_, p_335909_);
      return this;
   }

   public StreamCodec<B, Packet<? super L>> build() {
      return this.dispatchBuilder.build();
   }
}
