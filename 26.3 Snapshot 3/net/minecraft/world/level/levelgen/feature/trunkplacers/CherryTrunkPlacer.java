package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class CherryTrunkPlacer extends TrunkPlacer {
   private static final Codec<UniformInt> BRANCH_START_CODEC = UniformInt.MAP_CODEC
      .codec()
      .validate(
         u -> u.maxInclusive() - u.minInclusive() < 1
            ? DataResult.error(() -> "Need at least 2 blocks variation for the branch starts to fit both branches")
            : DataResult.success(u)
      );
   public static final MapCodec<CherryTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      i -> trunkPlacerParts(i)
         .and(
            i.group(
               IntProviders.codec(1, 3).fieldOf("branch_count").forGetter(t -> t.branchCount),
               IntProviders.codec(2, 16).fieldOf("branch_horizontal_length").forGetter(t -> t.branchHorizontalLength),
               IntProviders.validateCodec(-16, 0, BRANCH_START_CODEC).fieldOf("branch_start_offset_from_top").forGetter(t -> t.branchStartOffsetFromTop),
               IntProviders.codec(-16, 16).fieldOf("branch_end_offset_from_top").forGetter(t -> t.branchEndOffsetFromTop)
            )
         )
         .apply(i, CherryTrunkPlacer::new)
   );
   private final IntProvider branchCount;
   private final IntProvider branchHorizontalLength;
   private final UniformInt branchStartOffsetFromTop;
   private final UniformInt secondBranchStartOffsetFromTop;
   private final IntProvider branchEndOffsetFromTop;

   public CherryTrunkPlacer(
      final int baseHeight,
      final int heightRandA,
      final int heightRandB,
      final IntProvider branchCount,
      final IntProvider branchHorizontalLength,
      final UniformInt branchStartOffsetFromTop,
      final IntProvider branchEndOffsetFromTop
   ) {
      super(baseHeight, heightRandA, heightRandB);
      this.branchCount = branchCount;
      this.branchHorizontalLength = branchHorizontalLength;
      this.branchStartOffsetFromTop = branchStartOffsetFromTop;
      this.secondBranchStartOffsetFromTop = UniformInt.of(branchStartOffsetFromTop.minInclusive(), branchStartOffsetFromTop.maxInclusive() - 1);
      this.branchEndOffsetFromTop = branchEndOffsetFromTop;
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.CHERRY_TRUNK_PLACER;
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
      placeBelowTrunkBlock(level, trunkSetter, random, origin.below(), tree);
      int firstBranchOffsetFromOrigin = Math.max(0, treeHeight - 1 + this.branchStartOffsetFromTop.sample(random));
      int secondBranchOffsetFromOrigin = Math.max(0, treeHeight - 1 + this.secondBranchStartOffsetFromTop.sample(random));
      if (secondBranchOffsetFromOrigin >= firstBranchOffsetFromOrigin) {
         secondBranchOffsetFromOrigin++;
      }

      int branchCount = this.branchCount.sample(random);
      boolean hasMiddleBranch = branchCount == 3;
      boolean hasBothSideBranches = branchCount >= 2;
      int trunkHeight;
      if (hasMiddleBranch) {
         trunkHeight = treeHeight;
      } else if (hasBothSideBranches) {
         trunkHeight = Math.max(firstBranchOffsetFromOrigin, secondBranchOffsetFromOrigin) + 1;
      } else {
         trunkHeight = firstBranchOffsetFromOrigin + 1;
      }

      for (int y = 0; y < trunkHeight; y++) {
         this.placeLog(level, trunkSetter, random, origin.above(y), tree);
      }

      List<FoliagePlacer.FoliageAttachment> attachments = new ArrayList<>();
      if (hasMiddleBranch) {
         attachments.add(new FoliagePlacer.FoliageAttachment(origin.above(trunkHeight), 0, false));
      }

      BlockPos.MutableBlockPos logPos = new BlockPos.MutableBlockPos();
      Direction treeDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);
      Function<BlockState, BlockState> sidewaysStateModifier = state -> state.trySetValue(RotatedPillarBlock.AXIS, treeDirection.getAxis());
      attachments.add(
         this.generateBranch(
            level,
            trunkSetter,
            random,
            treeHeight,
            origin,
            tree,
            sidewaysStateModifier,
            treeDirection,
            firstBranchOffsetFromOrigin,
            firstBranchOffsetFromOrigin < trunkHeight - 1,
            logPos
         )
      );
      if (hasBothSideBranches) {
         attachments.add(
            this.generateBranch(
               level,
               trunkSetter,
               random,
               treeHeight,
               origin,
               tree,
               sidewaysStateModifier,
               treeDirection.getOpposite(),
               secondBranchOffsetFromOrigin,
               secondBranchOffsetFromOrigin < trunkHeight - 1,
               logPos
            )
         );
      }

      return attachments;
   }

   private FoliagePlacer.FoliageAttachment generateBranch(
      final WorldGenLevel level,
      final BiConsumer<BlockPos, BlockState> trunkSetter,
      final RandomSource random,
      final int treeHeight,
      final BlockPos origin,
      final TreeFeature tree,
      final Function<BlockState, BlockState> sidewaysStateModifier,
      final Direction branchDirection,
      final int offsetFromOrigin,
      final boolean middleContinuesUpwards,
      final BlockPos.MutableBlockPos logPos
   ) {
      logPos.set(origin).move(Direction.UP, offsetFromOrigin);
      int branchEndPosOffsetFromOrigin = treeHeight - 1 + this.branchEndOffsetFromTop.sample(random);
      boolean extendBranchAwayFromTrunk = middleContinuesUpwards || branchEndPosOffsetFromOrigin < offsetFromOrigin;
      int distanceToTrunk = this.branchHorizontalLength.sample(random) + (extendBranchAwayFromTrunk ? 1 : 0);
      BlockPos branchEndPos = origin.relative(branchDirection, distanceToTrunk).above(branchEndPosOffsetFromOrigin);
      int stepsHorizontally = extendBranchAwayFromTrunk ? 2 : 1;

      for (int i = 0; i < stepsHorizontally; i++) {
         this.placeLog(level, trunkSetter, random, logPos.move(branchDirection), tree, sidewaysStateModifier);
      }

      Direction verticalDirection = branchEndPos.getY() > logPos.getY() ? Direction.UP : Direction.DOWN;

      while (true) {
         int distance = logPos.distManhattan(branchEndPos);
         if (distance == 0) {
            return new FoliagePlacer.FoliageAttachment(branchEndPos.above(), 0, false);
         }

         float chanceToGrowVertically = (float)Math.abs(branchEndPos.getY() - logPos.getY()) / distance;
         boolean growVertically = random.nextFloat() < chanceToGrowVertically;
         logPos.move(growVertically ? verticalDirection : branchDirection);
         this.placeLog(level, trunkSetter, random, logPos, tree, growVertically ? Function.identity() : sidewaysStateModifier);
      }
   }
}
