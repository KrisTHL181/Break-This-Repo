package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class MegaJungleTrunkPlacer extends GiantTrunkPlacer {
   public static final MapCodec<MegaJungleTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> trunkPlacerParts(i).apply(i, MegaJungleTrunkPlacer::new));

   public MegaJungleTrunkPlacer(final int baseHeight, final int heightRandA, final int heightRandB) {
      super(baseHeight, heightRandA, heightRandB);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.MEGA_JUNGLE_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      final WorldGenLevel level,
      final BiConsumer<BlockPos, BlockState> trunkSetter,
      final RandomSource random,
      final int treeHeight,
      final BlockPos origin,
      final TreeFeature tree
   ) {
      List<FoliagePlacer.FoliageAttachment> attachments = Lists.newArrayList();
      attachments.addAll(super.placeTrunk(level, trunkSetter, random, treeHeight, origin, tree));

      for (int branchHeight = treeHeight - 2 - random.nextInt(4); branchHeight > treeHeight / 2; branchHeight -= 2 + random.nextInt(4)) {
         float angle = random.nextFloat() * (float) (Math.PI * 2);
         int bx = 0;
         int bz = 0;

         for (int b = 0; b < 5; b++) {
            bx = (int)(1.5F + Mth.cos(angle) * b);
            bz = (int)(1.5F + Mth.sin(angle) * b);
            BlockPos pos = origin.offset(bx, branchHeight - 3 + b / 2, bz);
            this.placeLog(level, trunkSetter, random, pos, tree);
         }

         attachments.add(new FoliagePlacer.FoliageAttachment(origin.offset(bx, branchHeight, bz), -2, false));
      }

      return attachments;
   }
}
