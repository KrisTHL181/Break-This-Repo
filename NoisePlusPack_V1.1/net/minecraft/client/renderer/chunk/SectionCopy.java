package net.minecraft.client.renderer.chunk;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
class SectionCopy {
   private final Map<BlockPos, BlockEntity> blockEntities;
   private final @Nullable PalettedContainer<BlockState> section;
   private final boolean debug;
   private final LevelHeightAccessor levelHeightAccessor;

   SectionCopy(LevelChunk p_407646_, int p_409362_) {
      this.levelHeightAccessor = p_407646_;
      this.debug = p_407646_.getLevel().isDebug();
      this.blockEntities = ImmutableMap.copyOf(p_407646_.getBlockEntities());
      if (p_407646_ instanceof EmptyLevelChunk) {
         this.section = null;
      } else {
         LevelChunkSection[] alevelchunksection = p_407646_.getSections();
         if (p_409362_ >= 0 && p_409362_ < alevelchunksection.length) {
            LevelChunkSection levelchunksection = alevelchunksection[p_409362_];
            this.section = levelchunksection.hasOnlyAir() ? null : levelchunksection.getStates().copy();
         } else {
            this.section = null;
         }
      }
   }

   public @Nullable BlockEntity getBlockEntity(BlockPos p_407168_) {
      return this.blockEntities.get(p_407168_);
   }

   public BlockState getBlockState(BlockPos p_407641_) {
      int i = p_407641_.getX();
      int j = p_407641_.getY();
      int k = p_407641_.getZ();
      if (this.debug) {
         BlockState blockstate = null;
         if (j == 60) {
            blockstate = Blocks.BARRIER.defaultBlockState();
         }

         if (j == 70) {
            blockstate = DebugLevelSource.getBlockStateFor(i, k);
         }

         return blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
      } else {
         if (this.section == null) {
            return Blocks.AIR.defaultBlockState();
         }

         try {
            return this.section.get(i & 15, j & 15, k & 15);
         } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting block state");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
            crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this.levelHeightAccessor, i, j, k));
            throw new ReportedException(crashreport);
         }
      }
   }
}
