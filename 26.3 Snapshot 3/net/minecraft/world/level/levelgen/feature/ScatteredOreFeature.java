package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public class ScatteredOreFeature extends AbstractOreFeature {
   private static final int MAX_DIST_FROM_ORIGIN = 7;
   public static final MapCodec<ScatteredOreFeature> CODEC = makeCodec(ScatteredOreFeature::new);

   public ScatteredOreFeature(final List<BlockReplacement> targetStates, final int size, final float discardChanceOnAirExposure) {
      super(targetStates, size, discardChanceOnAirExposure);
   }

   public ScatteredOreFeature(final RuleTest target, final BlockState state, final int size, final float discardChanceOnAirExposure) {
      this(List.of(BlockReplacement.replace(target, state)), size, discardChanceOnAirExposure);
   }

   @Override
   public MapCodec<ScatteredOreFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      int numberOfTries = random.nextInt(this.size + 1);
      BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();

      for (int i = 0; i < numberOfTries; i++) {
         this.offsetTargetPos(targetPos, random, origin, Math.min(i, 7));
         BlockState blockState = level.getBlockState(targetPos);

         for (BlockReplacement targetState : this.targetStates) {
            if (this.canPlaceOre(blockState, level::getBlockState, random, targetState, targetPos)) {
               level.setBlock(targetPos, targetState.state(), 2);
               break;
            }
         }
      }

      return true;
   }

   private void offsetTargetPos(
      final BlockPos.MutableBlockPos targetPos, final RandomSource random, final BlockPos origin, final int maxDistFromOriginForThisTry
   ) {
      int xd = this.getRandomPlacementInOneAxisRelativeToOrigin(random, maxDistFromOriginForThisTry);
      int yd = this.getRandomPlacementInOneAxisRelativeToOrigin(random, maxDistFromOriginForThisTry);
      int zd = this.getRandomPlacementInOneAxisRelativeToOrigin(random, maxDistFromOriginForThisTry);
      targetPos.setWithOffset(origin, xd, yd, zd);
   }

   private int getRandomPlacementInOneAxisRelativeToOrigin(final RandomSource random, final int maxDistanceFromOrigin) {
      return Math.round((random.nextFloat() - random.nextFloat()) * maxDistanceFromOrigin);
   }
}
