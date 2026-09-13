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

   public ClientboundOpenSignEditorPacket(final BlockPos pos, final boolean isFrontText) {
      this.pos = pos;
      this.isFrontText = isFrontText;
   }

   private ClientboundOpenSignEditorPacket(final FriendlyByteBuf input) {
      this.pos = input.readBlockPos();
      this.isFrontText = input.readBoolean();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeBlockPos(this.pos);
      output.writeBoolean(this.isFrontText);
   }

   @Override
   public PacketType<ClientboundOpenSignEditorPacket> type() {
      return GamePacketTypes.CLIENTBOUND_OPEN_SIGN_EDITOR;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleOpenSignEditor(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public boolean isFrontText() {
      return this.isFrontText;
   }
}
