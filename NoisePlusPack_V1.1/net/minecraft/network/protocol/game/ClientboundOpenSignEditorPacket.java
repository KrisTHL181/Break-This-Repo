package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundOpenSignEditorPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundOpenSignEditorPacket> STREAM_CODEC = Packet.codec(
      ClientboundOpenSignEditorPacket::write, ClientboundOpenSignEditorPacket::new
   );
   private final BlockPos pos;
   private final boolean isFrontText;

   public ClientboundOpenSignEditorPacket(BlockPos p_277843_, boolean p_277748_) {
      this.pos = p_277843_;
      this.isFrontText = p_277748_;
   }

   private ClientboundOpenSignEditorPacket(FriendlyByteBuf p_179013_) {
      this.pos = p_179013_.readBlockPos();
      this.isFrontText = p_179013_.readBoolean();
   }

   private void write(FriendlyByteBuf p_132642_) {
      p_132642_.writeBlockPos(this.pos);
      p_132642_.writeBoolean(this.isFrontText);
   }

   @Override
   public PacketType<ClientboundOpenSignEditorPacket> type() {
      return GamePacketTypes.CLIENTBOUND_OPEN_SIGN_EDITOR;
   }

   public void handle(ClientGamePacketListener p_132639_) {
      p_132639_.handleOpenSignEditor(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public boolean isFrontText() {
      return this.isFrontText;
   }
}
