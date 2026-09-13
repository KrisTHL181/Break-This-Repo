package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamMemberEncoder;

public interface Packet<T extends PacketListener> {
   PacketType<? extends Packet<T>> type();

   void handle(T listener);

   default boolean isSkippable() {
      return false;
   }

   default boolean isTerminal() {
      return false;
   }

   static <B extends ByteBuf, T extends Packet<?>> StreamCodec<B, T> codec(final StreamMemberEncoder<B, T> writer, final StreamDecoder<B, T> reader) {
      return StreamCodec.ofMember(writer, reader);
   }
}
