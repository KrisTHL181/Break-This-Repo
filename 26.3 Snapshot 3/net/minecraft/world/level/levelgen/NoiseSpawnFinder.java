package net.minecraft.world.level.levelgen;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;

public class NoiseSpawnFinder {
   private static final long MAX_RADIUS = 2048L;
   private NoiseSpawnFinder.Result result;

   private NoiseSpawnFinder(final List<SpawnTargetPoint.Wired> targetPoints) {
      this.result = getSpawnPositionAndFitness(targetPoints, 0, 0);
      this.radialSearch(targetPoints, 2048.0F, 512.0F);
      this.radialSearch(targetPoints, 512.0F, 32.0F);
   }

   public static BlockPos findSpawnPosition(final List<SpawnTargetPoint.Wired> targetPoints) {
      return (new NoiseSpawnFinder(targetPoints)).result.location();
   }

   private void radialSearch(final List<SpawnTargetPoint.Wired> targetPoints, final float maxRadius, final float radiusIncrement) {
      float angle = 0.0F;
      float radius = radiusIncrement;
      BlockPos searchOrigin = this.result.location();

      while (radius <= maxRadius) {
         int x = searchOrigin.getX() + (int)(Math.sin(angle) * radius);
         int z = searchOrigin.getZ() + (int)(Math.cos(angle) * radius);
         NoiseSpawnFinder.Result candidate = getSpawnPositionAndFitness(targetPoints, x, z);
         if (candidate.fitness() < this.result.fitness()) {
            this.result = candidate;
         }

         angle += radiusIncrement / radius;
         if (angle > Math.PI * 2) {
            angle = 0.0F;
            radius += radiusIncrement;
         }
      }
   }

   private static NoiseSpawnFinder.Result getSpawnPositionAndFitness(final List<SpawnTargetPoint.Wired> targetPoints, final int blockX, final int blockZ) {
      DensityFunction.SinglePointContext quartAlignedContext = new DensityFunction.SinglePointContext(
         QuartPos.toBlock(QuartPos.fromBlock(blockX)), 0, QuartPos.toBlock(QuartPos.fromBlock(blockZ))
      );
      long minFitness = Long.MAX_VALUE;

      for (SpawnTargetPoint.Wired point : targetPoints) {
         minFitness = Math.min(minFitness, point.sampleFitness(quartAlignedContext));
      }

      long distanceBiasToWorldOrigin = Mth.square((long)blockX) + Mth.square((long)blockZ);
      long fitnessWithDistance = minFitness * Mth.square(2048L) + distanceBiasToWorldOrigin;
      return new NoiseSpawnFinder.Result(new BlockPos(blockX, 0, blockZ), fitnessWithDistance);
   }

   private record Result(BlockPos location, long fitness) {
   }
}
