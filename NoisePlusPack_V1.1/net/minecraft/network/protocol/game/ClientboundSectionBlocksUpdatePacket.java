package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class ClientboundSectionBlocksUpdatePacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSectionBlocksUpdatePacket> STREAM_CODEC = Packet.codec(
      ClientboundSectionBlocksUpdatePacket::write, ClientboundSectionBlocksUpdatePacket::new
   );
   private static final int POS_IN_SECTION_BITS = 12;
   private final SectionPos sectionPos;
   private final short[] positions;
   private final BlockState[] states;

   public ClientboundSectionBlocksUpdatePacket(SectionPos p_284963_, ShortSet p_285027_, LevelChunkSection p_285414_) {
      this.sectionPos = p_284963_;
      int i = p_285027_.size();
      this.positions = new short[i];
      this.states = new BlockState[i];
      int j = 0;

      for (ShortIterator var6 = p_285027_.iterator(); var6.hasNext(); j++) {
         short short1 = (Short)var6.next();
         this.positions[j] = short1;
         this.states[j] = p_285414_.getBlockState(SectionPos.sectionRelativeX(short1), SectionPos.sectionRelativeY(short1), SectionPos.sectionRelativeZ(short1));
      }
   }

   private ClientboundSectionBlocksUpdatePacket(FriendlyByteBuf p_179196_) {
      this.sectionPos = SectionPos.STREAM_CODEC.decode(p_179196_);
      int i = p_179196_.readVarInt();
      this.positions = new short[i];
      this.states = new BlockState[i];

      for (int j = 0; j < i; j++) {
         long k = p_179196_.readVarLong();
         this.positions[j] = (short)(k & 4095L);
         this.states[j] = Block.BLOCK_STATE_REGISTRY.byId((int)(k >>> 12));
      }
   }

   private void write(FriendlyByteBuf p_133002_) {
      SectionPos.STREAM_CODEC.encode(p_133002_, this.sectionPos);
      p_133002_.writeVarInt(this.positions.length);

      for (int i = 0; i < this.positions.length; i++) {
         p_133002_.writeVarLong((long)Block.getId(this.states[i]) << 12 | this.positions[i]);
      }
   }

   @Override
   public PacketType<ClientboundSectionBlocksUpdatePacket> type() {
      return GamePacketTypes.CLIENTBOUND_SECTION_BLOCKS_UPDATE;
   }

   public void handle(ClientGamePacketListener p_132999_) {
      p_132999_.handleChunkBlocksUpdate(this);
   }

   public void runUpdates(BiConsumer<BlockPos, BlockState> p_132993_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (int i = 0; i < this.positions.length; i++) {
         short short1 = this.positions[i];
         blockpos$mutableblockpos.set(
            this.sectionPos.relativeToBlockX(short1), this.sectionPos.relativeToBlockY(short1), this.sectionPos.relativeToBlockZ(short1)
         );
         p_132993_.accept(blockpos$mutableblockpos, this.states[i]);
      }
   }
}
