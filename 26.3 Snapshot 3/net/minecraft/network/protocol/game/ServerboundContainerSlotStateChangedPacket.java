package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundContainerSlotStateChangedPacket(int slotId, int containerId, boolean newState) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundContainerSlotStateChangedPacket> STREAM_CODEC = Packet.codec(
      ServerboundContainerSlotStateChangedPacket::write, ServerboundContainerSlotStateChangedPacket::new
   );

   private ServerboundContainerSlotStateChangedPacket(final FriendlyByteBuf input) {
      this(input.readVarInt(), input.readContainerId(), input.readBoolean());
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.slotId);
      output.writeContainerId(this.containerId);
      output.writeBoolean(this.newState);
   }

   @Override
   public PacketType<ServerboundContainerSlotStateChangedPacket> type() {
      return GamePacketTypes.SERVERBOUND_CONTAINER_SLOT_STATE_CHANGED;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleContainerSlotStateChanged(this);
   }
}
