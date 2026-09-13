package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.LargeDripstoneConfiguration;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LargeDripstoneFeature extends Feature<LargeDripstoneConfiguration> {
   public LargeDripstoneFeature(Codec<LargeDripstoneConfiguration> p_159960_) {
      super(p_159960_);
   }

   @Override
   public boolean place(FeaturePlaceContext<LargeDripstoneConfiguration> p_159967_) {
      WorldGenLevel worldgenlevel = p_159967_.level();
      BlockPos blockpos = p_159967_.origin();
      LargeDripstoneConfiguration largedripstoneconfiguration = p_159967_.config();
      RandomSource randomsource = p_159967_.random();
      if (!DripstoneUtils.isEmptyOrWater(worldgenlevel, blockpos)) {
         return false;
      }

      Optional<Column> optional = Column.scan(
         worldgenlevel, blockpos, largedripstoneconfiguration.floorToCeilingSearchRange, DripstoneUtils::isEmptyOrWater, DripstoneUtils::isDripstoneBaseOrLava
      );
      if (!optional.isEmpty() && optional.get() instanceof Column.Range) {
         Column.Range column$range = (Column.Range)optional.get();
         if (column$range.height() < 4) {
            return false;
         }

         int i = (int)(column$range.height() * largedripstoneconfiguration.maxColumnRadiusToCaveHeightRatio);
         int j = Mth.clamp(i, largedripstoneconfiguration.columnRadius.getMinValue(), largedripstoneconfiguration.columnRadius.getMaxValue());
         int k = Mth.randomBetweenInclusive(randomsource, largedripstoneconfiguration.columnRadius.getMinValue(), j);
         LargeDripstoneFeature.LargeDripstone largedripstonefeature$largedripstone = makeDripstone(
            blockpos.atY(column$range.ceiling() - 1),
            false,
            randomsource,
            k,
            largedripstoneconfiguration.stalactiteBluntness,
            largedripstoneconfiguration.heightScale
         );
         LargeDripstoneFeature.LargeDripstone largedripstonefeature$largedripstone1 = makeDripstone(
            blockpos.atY(column$range.floor() + 1),
            true,
            randomsource,
            k,
            largedripstoneconfiguration.stalagmiteBluntness,
            largedripstoneconfiguration.heightScale
         );
         LargeDripstoneFeature.WindOffsetter largedripstonefeature$windoffsetter;
         if (largedripstonefeature$largedripstone.isSuitableForWind(largedripstoneconfiguration)
            && largedripstonefeature$largedripstone1.isSuitableForWind(largedripstoneconfiguration)) {
            largedripstonefeature$windoffsetter = new LargeDripstoneFeature.WindOffsetter(blockpos.getY(), randomsource, largedripstoneconfiguration.windSpeed);
         } else {
            largedripstonefeature$windoffsetter = LargeDripstoneFeature.WindOffsetter.noWind();
         }

         boolean flag = largedripstonefeature$largedripstone.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(
            worldgenlevel, largedripstonefeature$windoffsetter
         );
         boolean flag1 = largedripstonefeature$largedripstone1.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(
            worldgenlevel, largedripstonefeature$windoffsetter
         );
         if (flag) {
            largedripstonefeature$largedripstone.placeBlocks(worldgenlevel, randomsource, largedripstonefeature$windoffsetter);
         }

         if (flag1) {
            largedripstonefeature$largedripstone1.placeBlocks(worldgenlevel, randomsource, largedripstonefeature$windoffsetter);
         }

         if (SharedConstants.DEBUG_LARGE_DRIPSTONE) {
            this.placeDebugMarkers(worldgenlevel, blockpos, column$range, largedripstonefeature$windoffsetter);
         }

         return true;
      } else {
         return false;
      }
   }

   private static LargeDripstoneFeature.LargeDripstone makeDripstone(
      BlockPos p_225139_, boolean p_225140_, RandomSource p_225141_, int p_225142_, FloatProvider p_225143_, FloatProvider p_225144_
   ) {
      return new LargeDripstoneFeature.LargeDripstone(p_225139_, p_225140_, p_225142_, p_225143_.sample(p_225141_), p_225144_.sample(p_225141_));
   }

   private void placeDebugMarkers(WorldGenLevel p_159962_, BlockPos p_159963_, Column.Range p_159964_, LargeDripstoneFeature.WindOffsetter p_159965_) {
      p_159962_.setBlock(p_159965_.offset(p_159963_.atY(p_159964_.ceiling() - 1)), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
      p_159962_.setBlock(p_159965_.offset(p_159963_.atY(p_159964_.floor() + 1)), Blocks.GOLD_BLOCK.defaultBlockState(), 2);

      for (BlockPos.MutableBlockPos blockpos$mutableblockpos = p_159963_.atY(p_159964_.floor() + 2).mutable();
         blockpos$mutableblockpos.getY() < p_159964_.ceiling() - 1;
         blockpos$mutableblockpos.move(Direction.UP)
      ) {
         BlockPos blockpos = p_159965_.offset(blockpos$mutableblockpos);
         if (DripstoneUtils.isEmptyOrWater(p_159962_, blockpos) || p_159962_.getBlockState(blockpos).is(Blocks.DRIPSTONE_BLOCK)) {
            p_159962_.setBlock(blockpos, Blocks.CREEPER_HEAD.defaultBlockState(), 2);
         }
      }
   }

   static final class LargeDripstone {
      private BlockPos root;
      private final boolean pointingUp;
      private int radius;
      private final double bluntness;
      private final double scale;

      LargeDripstone(BlockPos p_197116_, boolean p_197117_, int p_197118_, double p_197119_, double p_197120_) {
         this.root = p_197116_;
         this.pointingUp = p_197117_;
         this.radius = p_197118_;
         this.bluntness = p_197119_;
         this.scale = p_197120_;
      }

      private int getHeight() {
         return this.getHeightAtRadius(0.0F);
      }

      private int getMinY() {
         return this.pointingUp ? this.root.getY() : this.root.getY() - this.getHeight();
      }

      private int getMaxY() {
         return !this.pointingUp ? this.root.getY() : this.root.getY() + this.getHeight();
      }

      boolean moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(WorldGenLevel p_159990_, LargeDripstoneFeature.WindOffsetter p_159991_) {
         while (this.radius > 1) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = this.root.mutable();
            int i = Math.min(10, this.getHeight());

            for (int j = 0; j < i; j++) {
               if (p_159990_.getBlockState(blockpos$mutableblockpos).is(Blocks.LAVA)) {
                  return false;
               }

               if (DripstoneUtils.isCircleMostlyEmbeddedInStone(p_159990_, p_159991_.offset(blockpos$mutableblockpos), this.radius)) {
                  this.root = blockpos$mutableblockpos;
                  return true;
               }

               blockpos$mutableblockpos.move(this.pointingUp ? Direction.DOWN : Direction.UP);
            }

            this.radius /= 2;
         }

         return false;
      }

      private int getHeightAtRadius(float p_159988_) {
         return (int)DripstoneUtils.getDripstoneHeight(p_159988_, this.radius, this.scale, this.bluntness);
      }

      void placeBlocks(WorldGenLevel p_225146_, RandomSource p_225147_, LargeDripstoneFeature.WindOffsetter p_225148_) {
         for (int i = -this.radius; i <= this.radius; i++) {
            for (int j = -this.radius; j <= this.radius; j++) {
               float f = Mth.sqrt(i * i + j * j);
               if (!(f > this.radius)) {
                  int k = this.getHeightAtRadius(f);
                  if (k > 0) {
                     if (p_225147_.nextFloat() < 0.2) {
                        k = (int)(k * Mth.randomBetween(p_225147_, 0.8F, 1.0F));
                     }

                     BlockPos.MutableBlockPos blockpos$mutableblockpos = this.root.offset(i, 0, j).mutable();
                     boolean flag = false;
                     int l = this.pointingUp
                        ? p_225146_.getHeight(Heightmap.Types.WORLD_SURFACE_WG, blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getZ())
                        : Integer.MAX_VALUE;

                     for (int i1 = 0; i1 < k && blockpos$mutableblockpos.getY() < l; i1++) {
                        BlockPos blockpos = p_225148_.offset(blockpos$mutableblockpos);
                        if (DripstoneUtils.isEmptyOrWaterOrLava(p_225146_, blockpos)) {
                           flag = true;
                           Block block = SharedConstants.DEBUG_LARGE_DRIPSTONE ? Blocks.GLASS : Blocks.DRIPSTONE_BLOCK;
                           p_225146_.setBlock(blockpos, block.defaultBlockState(), 2);
                        } else if (flag && p_225146_.getBlockState(blockpos).is(BlockTags.BASE_STONE_OVERWORLD)) {
                           break;
                        }

                        blockpos$mutableblockpos.move(this.pointingUp ? Direction.UP : Direction.DOWN);
                     }
                  }
               }
            }
         }
      }

      boolean isSuitableForWind(LargeDripstoneConfiguration p_159997_) {
         return this.radius >= p_159997_.minRadiusForWind && this.bluntness >= p_159997_.minBluntnessForWind;
      }
   }

   static final class WindOffsetter {
      private final int originY;
      private final @Nullable Vec3 windSpeed;

      WindOffsetter(int p_225150_, RandomSource p_225151_, FloatProvider p_225152_) {
         this.originY = p_225150_;
         float f = p_225152_.sample(p_225151_);
         float f1 = Mth.randomBetween(p_225151_, 0.0F, (float) Math.PI);
         this.windSpeed = new Vec3(Mth.cos(f1) * f, 0.0, Mth.sin(f1) * f);
      }

      private WindOffsetter() {
         this.originY = 0;
         this.windSpeed = null;
      }

      static LargeDripstoneFeature.WindOffsetter noWind() {
         return new LargeDripstoneFeature.WindOffsetter();
      }

      BlockPos offset(BlockPos p_160009_) {
         if (this.windSpeed == null) {
            return p_160009_;
         }

         int i = this.originY - p_160009_.getY();
         Vec3 vec3 = this.windSpeed.scale(i);
         return p_160009_.offset(Mth.floor(vec3.x), 0, Mth.floor(vec3.z));
      }
   }
}
