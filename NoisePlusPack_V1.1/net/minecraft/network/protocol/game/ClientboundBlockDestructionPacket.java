package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundBlockDestructionPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundBlockDestructionPacket> STREAM_CODEC = Packet.codec(
      ClientboundBlockDestructionPacket::write, ClientboundBlockDestructionPacket::new
   );
   private final int id;
   private final BlockPos pos;
   private final int progress;

   public ClientboundBlockDestructionPacket(int p_131676_, BlockPos p_131677_, int p_131678_) {
      this.id = p_131676_;
      this.pos = p_131677_;
      this.progress = p_131678_;
   }

   private ClientboundBlockDestructionPacket(FriendlyByteBuf p_178606_) {
      this.id = p_178606_.readVarInt();
      this.pos = p_178606_.readBlockPos();
      this.progress = p_178606_.readUnsignedByte();
   }

   private void write(FriendlyByteBuf p_131687_) {
      p_131687_.writeVarInt(this.id);
      p_131687_.writeBlockPos(this.pos);
      p_131687_.writeByte(this.progress);
   }

   @Override
   public PacketType<ClientboundBlockDestructionPacket> type() {
      return GamePacketTypes.CLIENTBOUND_BLOCK_DESTRUCTION;
   }

   public void handle(ClientGamePacketListener p_131684_) {
      p_131684_.handleBlockDestruction(this);
   }

   public int getId() {
      return this.id;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public int getProgress() {
      return this.progress;
   }
}
