package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundLevelEventPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundLevelEventPacket> STREAM_CODEC = Packet.codec(
      ClientboundLevelEventPacket::write, ClientboundLevelEventPacket::new
   );
   private final int type;
   private final BlockPos pos;
   private final int data;
   private final boolean globalEvent;

   public ClientboundLevelEventPacket(final int type, final BlockPos pos, final int data, final boolean globalEvent) {
      this.type = type;
      this.pos = pos.immutable();
      this.data = data;
      this.globalEvent = globalEvent;
   }

   private ClientboundLevelEventPacket(final FriendlyByteBuf input) {
      this.type = input.readInt();
      this.pos = input.readBlockPos();
      this.data = input.readInt();
      this.globalEvent = input.readBoolean();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeInt(this.type);
      output.writeBlockPos(this.pos);
      output.writeInt(this.data);
      output.writeBoolean(this.globalEvent);
   }

   @Override
   public PacketType<ClientboundLevelEventPacket> type() {
      return GamePacketTypes.CLIENTBOUND_LEVEL_EVENT;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleLevelEvent(this);
   }

   public boolean isGlobalEvent() {
      return this.globalEvent;
   }

   public int getType() {
      return this.type;
   }

   public int getData() {
      return this.data;
   }

   public BlockPos getPos() {
      return this.pos;
   }
}
