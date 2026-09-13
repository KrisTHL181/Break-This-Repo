package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import org.jspecify.annotations.Nullable;

public class LevelLightEngine implements LightEventListener {
   public static final int LIGHT_SECTION_PADDING = 1;
   public static final LevelLightEngine EMPTY = new LevelLightEngine();
   protected final LevelHeightAccessor levelHeightAccessor;
   private final @Nullable LightEngine<?, ?> blockEngine;
   private final @Nullable LightEngine<?, ?> skyEngine;

   public LevelLightEngine(LightChunkGetter p_75805_, boolean p_75806_, boolean p_75807_) {
      this.levelHeightAccessor = p_75805_.getLevel();
      this.blockEngine = p_75806_ ? new BlockLightEngine(p_75805_) : null;
      this.skyEngine = p_75807_ ? new SkyLightEngine(p_75805_) : null;
   }

   private LevelLightEngine() {
      this.levelHeightAccessor = LevelHeightAccessor.create(0, 0);
      this.blockEngine = null;
      this.skyEngine = null;
   }

   @Override
   public void checkBlock(BlockPos p_75823_) {
      if (this.blockEngine != null) {
         this.blockEngine.checkBlock(p_75823_);
      }

      if (this.skyEngine != null) {
         this.skyEngine.checkBlock(p_75823_);
      }
   }

   @Override
   public boolean hasLightWork() {
      return this.skyEngine != null && this.skyEngine.hasLightWork() ? true : this.blockEngine != null && this.blockEngine.hasLightWork();
   }

   @Override
   public int runLightUpdates() {
      int i = 0;
      if (this.blockEngine != null) {
         i += this.blockEngine.runLightUpdates();
      }

      if (this.skyEngine != null) {
         i += this.skyEngine.runLightUpdates();
      }

      return i;
   }

   @Override
   public void updateSectionStatus(SectionPos p_75827_, boolean p_75828_) {
      if (this.blockEngine != null) {
         this.blockEngine.updateSectionStatus(p_75827_, p_75828_);
      }

      if (this.skyEngine != null) {
         this.skyEngine.updateSectionStatus(p_75827_, p_75828_);
      }
   }

   @Override
   public void setLightEnabled(ChunkPos p_285439_, boolean p_285012_) {
      if (this.blockEngine != null) {
         this.blockEngine.setLightEnabled(p_285439_, p_285012_);
      }

      if (this.skyEngine != null) {
         this.skyEngine.setLightEnabled(p_285439_, p_285012_);
      }
   }

   @Override
   public void propagateLightSources(ChunkPos p_284998_) {
      if (this.blockEngine != null) {
         this.blockEngine.propagateLightSources(p_284998_);
      }

      if (this.skyEngine != null) {
         this.skyEngine.propagateLightSources(p_284998_);
      }
   }

   public LayerLightEventListener getLayerListener(LightLayer p_75815_) {
      if (p_75815_ == LightLayer.BLOCK) {
         return this.blockEngine == null ? LayerLightEventListener.DummyLightLayerEventListener.INSTANCE : this.blockEngine;
      } else {
         return this.skyEngine == null ? LayerLightEventListener.DummyLightLayerEventListener.INSTANCE : this.skyEngine;
      }
   }

   public String getDebugData(LightLayer p_75817_, SectionPos p_75818_) {
      if (p_75817_ == LightLayer.BLOCK) {
         if (this.blockEngine != null) {
            return this.blockEngine.getDebugData(p_75818_.asLong());
         }
      } else if (this.skyEngine != null) {
         return this.skyEngine.getDebugData(p_75818_.asLong());
      }

      return "n/a";
   }

   public LayerLightSectionStorage.SectionType getDebugSectionType(LightLayer p_285008_, SectionPos p_285336_) {
      if (p_285008_ == LightLayer.BLOCK) {
         if (this.blockEngine != null) {
            return this.blockEngine.getDebugSectionType(p_285336_.asLong());
         }
      } else if (this.skyEngine != null) {
         return this.skyEngine.getDebugSectionType(p_285336_.asLong());
      }

      return LayerLightSectionStorage.SectionType.EMPTY;
   }

   public void queueSectionData(LightLayer p_285328_, SectionPos p_284962_, @Nullable DataLayer p_285035_) {
      if (p_285328_ == LightLayer.BLOCK) {
         if (this.blockEngine != null) {
            this.blockEngine.queueSectionData(p_284962_.asLong(), p_285035_);
         }
      } else if (this.skyEngine != null) {
         this.skyEngine.queueSectionData(p_284962_.asLong(), p_285035_);
      }
   }

   public void retainData(ChunkPos p_75829_, boolean p_75830_) {
      if (this.blockEngine != null) {
         this.blockEngine.retainData(p_75829_, p_75830_);
      }

      if (this.skyEngine != null) {
         this.skyEngine.retainData(p_75829_, p_75830_);
      }
   }

   public int getRawBrightness(BlockPos p_75832_, int p_75833_) {
      int i = this.skyEngine == null ? 0 : this.skyEngine.getLightValue(p_75832_) - p_75833_;
      int j = this.blockEngine == null ? 0 : this.blockEngine.getLightValue(p_75832_);
      return Math.max(j, i);
   }

   public boolean lightOnInColumn(long p_369308_) {
      return this.blockEngine == null
         || this.blockEngine.storage.lightOnInColumn(p_369308_) && (this.skyEngine == null || this.skyEngine.storage.lightOnInColumn(p_369308_));
   }

   public int getLightSectionCount() {
      return this.levelHeightAccessor.getSectionsCount() + 2;
   }

   public int getMinLightSection() {
      return this.levelHeightAccessor.getMinSectionY() - 1;
   }

   public int getMaxLightSection() {
      return this.getMinLightSection() + this.getLightSectionCount();
   }
}
