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

   public ClientboundBlockDestructionPacket(final int id, final BlockPos pos, final int progress) {
      this.id = id;
      this.pos = pos;
      this.progress = progress;
   }

   private ClientboundBlockDestructionPacket(final FriendlyByteBuf input) {
      this.id = input.readVarInt();
      this.pos = input.readBlockPos();
      this.progress = input.readUnsignedByte();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.id);
      output.writeBlockPos(this.pos);
      output.writeByte(this.progress);
   }

   @Override
   public PacketType<ClientboundBlockDestructionPacket> type() {
      return GamePacketTypes.CLIENTBOUND_BLOCK_DESTRUCTION;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleBlockDestruction(this);
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
