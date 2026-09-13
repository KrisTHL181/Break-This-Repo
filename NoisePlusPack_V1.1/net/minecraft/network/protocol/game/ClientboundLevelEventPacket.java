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

   public ClientboundLevelEventPacket(int p_132264_, BlockPos p_132265_, int p_132266_, boolean p_132267_) {
      this.type = p_132264_;
      this.pos = p_132265_.immutable();
      this.data = p_132266_;
      this.globalEvent = p_132267_;
   }

   private ClientboundLevelEventPacket(FriendlyByteBuf p_178908_) {
      this.type = p_178908_.readInt();
      this.pos = p_178908_.readBlockPos();
      this.data = p_178908_.readInt();
      this.globalEvent = p_178908_.readBoolean();
   }

   private void write(FriendlyByteBuf p_132276_) {
      p_132276_.writeInt(this.type);
      p_132276_.writeBlockPos(this.pos);
      p_132276_.writeInt(this.data);
      p_132276_.writeBoolean(this.globalEvent);
   }

   @Override
   public PacketType<ClientboundLevelEventPacket> type() {
      return GamePacketTypes.CLIENTBOUND_LEVEL_EVENT;
   }

   public void handle(ClientGamePacketListener p_132273_) {
      p_132273_.handleLevelEvent(this);
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
