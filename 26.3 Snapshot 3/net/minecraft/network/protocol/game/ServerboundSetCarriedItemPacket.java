package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundSetCarriedItemPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundSetCarriedItemPacket> STREAM_CODEC = Packet.codec(
      ServerboundSetCarriedItemPacket::write, ServerboundSetCarriedItemPacket::new
   );
   private final int slot;

   public ServerboundSetCarriedItemPacket(final int slot) {
      this.slot = slot;
   }

   private ServerboundSetCarriedItemPacket(final FriendlyByteBuf input) {
      this.slot = input.readShort();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeShort(this.slot);
   }

   @Override
   public PacketType<ServerboundSetCarriedItemPacket> type() {
      return GamePacketTypes.SERVERBOUND_SET_CARRIED_ITEM;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleSetCarriedItem(this);
   }

   public int getSlot() {
      return this.slot;
   }
}
