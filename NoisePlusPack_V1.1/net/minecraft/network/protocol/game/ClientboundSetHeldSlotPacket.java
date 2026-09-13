package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundSetHeldSlotPacket(int slot) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<ByteBuf, ClientboundSetHeldSlotPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, ClientboundSetHeldSlotPacket::slot, ClientboundSetHeldSlotPacket::new
   );

   @Override
   public PacketType<ClientboundSetHeldSlotPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_HELD_SLOT;
   }

   public void handle(ClientGamePacketListener p_367345_) {
      p_367345_.handleSetHeldSlot(this);
   }
}
