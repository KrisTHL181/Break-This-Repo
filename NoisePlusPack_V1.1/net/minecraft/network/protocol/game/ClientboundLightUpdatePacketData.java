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

   public ClientboundLightUpdatePacketData(ChunkPos p_285385_, LevelLightEngine p_285143_, @Nullable BitSet p_285253_, @Nullable BitSet p_285051_) {
      this.skyYMask = new BitSet();
      this.blockYMask = new BitSet();
      this.emptySkyYMask = new BitSet();
      this.emptyBlockYMask = new BitSet();
      this.skyUpdates = Lists.newArrayList();
      this.blockUpdates = Lists.newArrayList();

      for (int i = 0; i < p_285143_.getLightSectionCount(); i++) {
         if (p_285253_ == null || p_285253_.get(i)) {
            this.prepareSectionData(p_285385_, p_285143_, LightLayer.SKY, i, this.skyYMask, this.emptySkyYMask, this.skyUpdates);
         }

         if (p_285051_ == null || p_285051_.get(i)) {
            this.prepareSectionData(p_285385_, p_285143_, LightLayer.BLOCK, i, this.blockYMask, this.emptyBlockYMask, this.blockUpdates);
         }
      }
   }

   public ClientboundLightUpdatePacketData(FriendlyByteBuf p_195737_, int p_195738_, int p_195739_) {
      this.skyYMask = p_195737_.readBitSet();
      this.blockYMask = p_195737_.readBitSet();
      this.emptySkyYMask = p_195737_.readBitSet();
      this.emptyBlockYMask = p_195737_.readBitSet();
      this.skyUpdates = p_195737_.readList(DATA_LAYER_STREAM_CODEC);
      this.blockUpdates = p_195737_.readList(DATA_LAYER_STREAM_CODEC);
   }

   public void write(FriendlyByteBuf p_195750_) {
      p_195750_.writeBitSet(this.skyYMask);
      p_195750_.writeBitSet(this.blockYMask);
      p_195750_.writeBitSet(this.emptySkyYMask);
      p_195750_.writeBitSet(this.emptyBlockYMask);
      p_195750_.writeCollection(this.skyUpdates, DATA_LAYER_STREAM_CODEC);
      p_195750_.writeCollection(this.blockUpdates, DATA_LAYER_STREAM_CODEC);
   }

   private void prepareSectionData(
      ChunkPos p_195742_, LevelLightEngine p_195743_, LightLayer p_195744_, int p_195745_, BitSet p_195746_, BitSet p_195747_, List<byte[]> p_195748_
   ) {
      DataLayer datalayer = p_195743_.getLayerListener(p_195744_).getDataLayerData(SectionPos.of(p_195742_, p_195743_.getMinLightSection() + p_195745_));
      if (datalayer != null) {
         if (datalayer.isEmpty()) {
            p_195747_.set(p_195745_);
         } else {
            p_195746_.set(p_195745_);
            p_195748_.add(datalayer.copy().getData());
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
