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

   public ServerboundJigsawGeneratePacket(BlockPos p_134078_, int p_134079_, boolean p_134080_) {
      this.pos = p_134078_;
      this.levels = p_134079_;
      this.keepJigsaws = p_134080_;
   }

   private ServerboundJigsawGeneratePacket(FriendlyByteBuf p_179669_) {
      this.pos = p_179669_.readBlockPos();
      this.levels = p_179669_.readVarInt();
      this.keepJigsaws = p_179669_.readBoolean();
   }

   private void write(FriendlyByteBuf p_134089_) {
      p_134089_.writeBlockPos(this.pos);
      p_134089_.writeVarInt(this.levels);
      p_134089_.writeBoolean(this.keepJigsaws);
   }

   @Override
   public PacketType<ServerboundJigsawGeneratePacket> type() {
      return GamePacketTypes.SERVERBOUND_JIGSAW_GENERATE;
   }

   public void handle(ServerGamePacketListener p_134086_) {
      p_134086_.handleJigsawGenerate(this);
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
