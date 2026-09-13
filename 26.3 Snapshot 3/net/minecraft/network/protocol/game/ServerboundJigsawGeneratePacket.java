package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundJigsawGeneratePacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundJigsawGeneratePacket> STREAM_CODEC = Packet.codec(
      ServerboundJigsawGeneratePacket::write, ServerboundJigsawGeneratePacket::new
   );
   private final BlockPos pos;
   private final int levels;
   private final boolean keepJigsaws;

   public ServerboundJigsawGeneratePacket(final BlockPos blockPos, final int levels, final boolean keepJigsaws) {
      this.pos = blockPos;
      this.levels = levels;
      this.keepJigsaws = keepJigsaws;
   }

   private ServerboundJigsawGeneratePacket(final FriendlyByteBuf input) {
      this.pos = input.readBlockPos();
      this.levels = input.readVarInt();
      this.keepJigsaws = input.readBoolean();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeBlockPos(this.pos);
      output.writeVarInt(this.levels);
      output.writeBoolean(this.keepJigsaws);
   }

   @Override
   public PacketType<ServerboundJigsawGeneratePacket> type() {
      return GamePacketTypes.SERVERBOUND_JIGSAW_GENERATE;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleJigsawGenerate(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public int levels() {
      return this.levels;
   }

   public boolean keepJigsaws() {
      return this.keepJigsaws;
   }
}
