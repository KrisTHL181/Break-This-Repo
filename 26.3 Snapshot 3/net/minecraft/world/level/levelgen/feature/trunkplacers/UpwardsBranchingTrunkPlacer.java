package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class UpwardsBranchingTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<UpwardsBranchingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      i -> trunkPlacerParts(i)
         .and(
            i.group(
               IntProviders.POSITIVE_CODEC.fieldOf("extra_branch_steps").forGetter(p -> p.extraBranchSteps),
               Codec.floatRange(0.0F, 1.0F).fieldOf("place_branch_per_log_probability").forGetter(p -> p.placeBranchPerLogProbability),
               IntProviders.NON_NEGATIVE_CODEC.fieldOf("extra_branch_length").forGetter(c -> c.extraBranchLength),
               RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("can_grow_through").forGetter(t -> t.canGrowThrough)
            )
         )
         .apply(i, UpwardsBranchingTrunkPlacer::new)
   );
   private final IntProvider extraBranchSteps;
   private final float placeBranchPerLogProbability;
   private final IntProvider extraBranchLength;
   private final HolderSet<Block> canGrowThrough;

   public UpwardsBranchingTrunkPlacer(
      final int baseHeight,
      final int heightRandA,
      final int heightRandB,
      final IntProvider extraBranchSteps,
      final float placeBranchPerLogProbability,
      final IntProvider extraBranchLength,
      final HolderSet<Block> canGrowThrough
   ) {
      super(baseHeight, heightRandA, heightRandB);
      this.extraBranchSteps = extraBranchSteps;
      this.placeBranchPerLogProbability = placeBranchPerLogProbability;
      this.extraBranchLength = extraBranchLength;
      this.canGrowThrough = canGrowThrough;
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.UPWARDS_BRANCHING_TRUNK_PLACER;
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
      BlockPos.MutableBlockPos logPos = new BlockPos.MutableBlockPos();

      for (int heightPos = 0; heightPos < treeHeight; heightPos++) {
         int currentHeight = origin.getY() + heightPos;
         if (this.placeLog(level, trunkSetter, random, logPos.set(origin.getX(), currentHeight, origin.getZ()), tree)
            && heightPos < treeHeight - 1
            && random.nextFloat() < this.placeBranchPerLogProbability) {
            Direction branchDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int branchLen = this.extraBranchLength.sample(random);
            int branchPos = Math.max(0, branchLen - this.extraBranchLength.sample(random) - 1);
            int branchSteps = this.extraBranchSteps.sample(random);
            this.placeBranch(level, trunkSetter, random, treeHeight, tree, attachments, logPos, currentHeight, branchDir, branchPos, branchSteps);
         }

         if (heightPos == treeHeight - 1) {
            attachments.add(new FoliagePlacer.FoliageAttachment(logPos.set(origin.getX(), currentHeight + 1, origin.getZ()), 0, false));
         }
      }

      return attachments;
   }

   private void placeBranch(
      final WorldGenLevel level,
      final BiConsumer<BlockPos, BlockState> trunkSetter,
      final RandomSource random,
      final int treeHeight,
      final TreeFeature tree,
      final List<FoliagePlacer.FoliageAttachment> attachments,
      final BlockPos.MutableBlockPos logPos,
      final int currentHeight,
      final Direction branchDir,
      final int branchPos,
      int branchSteps
   ) {
      int heightAlongBranch = currentHeight + branchPos;
      int logX = logPos.getX();
      int logZ = logPos.getZ();
      int branchPlacementIndex = branchPos;

      while (branchPlacementIndex < treeHeight && branchSteps > 0) {
         if (branchPlacementIndex >= 1) {
            int placementHeight = currentHeight + branchPlacementIndex;
            logX += branchDir.getStepX();
            logZ += branchDir.getStepZ();
            heightAlongBranch = placementHeight;
            if (this.placeLog(level, trunkSetter, random, logPos.set(logX, placementHeight, logZ), tree)) {
               heightAlongBranch++;
            }

            attachments.add(new FoliagePlacer.FoliageAttachment(logPos.immutable(), 0, false));
         }

         branchPlacementIndex++;
         branchSteps--;
      }

      if (heightAlongBranch - currentHeight > 1) {
         BlockPos foliagePos = new BlockPos(logX, heightAlongBranch, logZ);
         attachments.add(new FoliagePlacer.FoliageAttachment(foliagePos, 0, false));
         attachments.add(new FoliagePlacer.FoliageAttachment(foliagePos.below(2), 0, false));
      }
   }

   @Override
   protected boolean validTreePos(final WorldGenLevel level, final BlockPos pos) {
      return super.validTreePos(level, pos) || level.isStateAtPosition(pos, s -> s.is(this.canGrowThrough));
   }
}
