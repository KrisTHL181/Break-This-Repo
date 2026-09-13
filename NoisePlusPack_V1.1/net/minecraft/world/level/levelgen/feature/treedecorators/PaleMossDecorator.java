package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HangingMossBlock;

public class PaleMossDecorator extends TreeDecorator {
   public static final MapCodec<PaleMossDecorator> CODEC = RecordCodecBuilder.mapCodec(
      p_367624_ -> p_367624_.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("leaves_probability").forGetter(p_368139_ -> p_368139_.leavesProbability),
            Codec.floatRange(0.0F, 1.0F).fieldOf("trunk_probability").forGetter(p_368321_ -> p_368321_.trunkProbability),
            Codec.floatRange(0.0F, 1.0F).fieldOf("ground_probability").forGetter(p_364066_ -> p_364066_.groundProbability)
         )
         .apply(p_367624_, PaleMossDecorator::new)
   );
   private final float leavesProbability;
   private final float trunkProbability;
   private final float groundProbability;

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.PALE_MOSS;
   }

   public PaleMossDecorator(float p_362537_, float p_367812_, float p_366658_) {
      this.leavesProbability = p_362537_;
      this.trunkProbability = p_367812_;
      this.groundProbability = p_366658_;
   }

   @Override
   public void place(TreeDecorator.Context p_366717_) {
      RandomSource randomsource = p_366717_.random();
      WorldGenLevel worldgenlevel = (WorldGenLevel)p_366717_.level();
      List<BlockPos> list = Util.shuffledCopy(p_366717_.logs(), randomsource);
      if (!list.isEmpty()) {
         BlockPos blockpos = Collections.min(list, Comparator.comparingInt(Vec3i::getY));
         if (randomsource.nextFloat() < this.groundProbability) {
            worldgenlevel.registryAccess()
               .lookup(Registries.CONFIGURED_FEATURE)
               .flatMap(p_375357_ -> p_375357_.get(VegetationFeatures.PALE_MOSS_PATCH))
               .ifPresent(
                  p_370079_ -> p_370079_.value().place(worldgenlevel, worldgenlevel.getLevel().getChunkSource().getGenerator(), randomsource, blockpos.above())
               );
         }

         p_366717_.logs().forEach(p_375360_ -> {
            if (randomsource.nextFloat() < this.trunkProbability) {
               BlockPos blockpos1 = p_375360_.below();
               if (p_366717_.isAir(blockpos1)) {
                  addMossHanger(blockpos1, p_366717_);
               }
            }
         });
         p_366717_.leaves().forEach(p_364665_ -> {
            if (randomsource.nextFloat() < this.leavesProbability) {
               BlockPos blockpos1 = p_364665_.below();
               if (p_366717_.isAir(blockpos1)) {
                  addMossHanger(blockpos1, p_366717_);
               }
            }
         });
      }
   }

   private static void addMossHanger(BlockPos p_368181_, TreeDecorator.Context p_365451_) {
      while (p_365451_.isAir(p_368181_.below()) && !(p_365451_.random().nextFloat() < 0.5)) {
         p_365451_.setBlock(p_368181_, Blocks.PALE_HANGING_MOSS.defaultBlockState().setValue(HangingMossBlock.TIP, false));
         p_368181_ = p_368181_.below();
      }

      p_365451_.setBlock(p_368181_, Blocks.PALE_HANGING_MOSS.defaultBlockState().setValue(HangingMossBlock.TIP, true));
   }
}
