package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import java.util.BitSet;
import java.util.List;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

public class ClientboundLightUpdatePacketData {
   private static final StreamCodec<ByteBuf, byte[]> DATA_LAYER_STREAM_CODEC = ByteBufCodecs.byteArray(2048);
   private final BitSet skyYMask;
   private final BitSet blockYMask;
   private final BitSet emptySkyYMask;
   private final BitSet emptyBlockYMask;
   private final List<byte[]> skyUpdates;
   private final List<byte[]> blockUpdates;

   public ClientboundLightUpdatePacketData(
      final ChunkPos chunkPos,
      final LevelLightEngine lightEngine,
      final @Nullable BitSet skyChangedLightSectionFilter,
      final @Nullable BitSet blockChangedLightSectionFilter
   ) {
      this.skyYMask = new BitSet();
      this.blockYMask = new BitSet();
      this.emptySkyYMask = new BitSet();
      this.emptyBlockYMask = new BitSet();
      this.skyUpdates = Lists.newArrayList();
      this.blockUpdates = Lists.newArrayList();

      for (int sectionIndex = 0; sectionIndex < lightEngine.getLightSectionCount(); sectionIndex++) {
         if (skyChangedLightSectionFilter == null || skyChangedLightSectionFilter.get(sectionIndex)) {
            this.prepareSectionData(chunkPos, lightEngine, LightLayer.SKY, sectionIndex, this.skyYMask, this.emptySkyYMask, this.skyUpdates);
         }

         if (blockChangedLightSectionFilter == null || blockChangedLightSectionFilter.get(sectionIndex)) {
            this.prepareSectionData(chunkPos, lightEngine, LightLayer.BLOCK, sectionIndex, this.blockYMask, this.emptyBlockYMask, this.blockUpdates);
         }
      }
   }

   public ClientboundLightUpdatePacketData(final FriendlyByteBuf input, final int x, final int z) {
      this.skyYMask = input.readBitSet();
      this.blockYMask = input.readBitSet();
      this.emptySkyYMask = input.readBitSet();
      this.emptyBlockYMask = input.readBitSet();
      this.skyUpdates = input.readList(DATA_LAYER_STREAM_CODEC);
      this.blockUpdates = input.readList(DATA_LAYER_STREAM_CODEC);
   }

   public void write(final FriendlyByteBuf output) {
      output.writeBitSet(this.skyYMask);
      output.writeBitSet(this.blockYMask);
      output.writeBitSet(this.emptySkyYMask);
      output.writeBitSet(this.emptyBlockYMask);
      output.writeCollection(this.skyUpdates, DATA_LAYER_STREAM_CODEC);
      output.writeCollection(this.blockUpdates, DATA_LAYER_STREAM_CODEC);
   }

   private void prepareSectionData(
      final ChunkPos pos,
      final LevelLightEngine lightEngine,
      final LightLayer layer,
      final int sectionIndex,
      final BitSet mask,
      final BitSet emptyMask,
      final List<byte[]> updates
   ) {
      DataLayer data = lightEngine.getLayerListener(layer).getDataLayerData(SectionPos.of(pos, lightEngine.getMinLightSection() + sectionIndex));
      if (data != null) {
         if (data.isEmpty()) {
            emptyMask.set(sectionIndex);
         } else {
            mask.set(sectionIndex);
            updates.add(data.copy().getData());
         }
      }
   }

   public BitSet getSkyYMask() {
      return this.skyYMask;
   }

   public BitSet getEmptySkyYMask() {
      return this.emptySkyYMask;
   }

   public List<byte[]> getSkyUpdates() {
      return this.skyUpdates;
   }

   public BitSet getBlockYMask() {
      return this.blockYMask;
   }

   public BitSet getEmptyBlockYMask() {
      return this.emptyBlockYMask;
   }

   public List<byte[]> getBlockUpdates() {
      return this.blockUpdates;
   }
}
