package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.CarverOutput;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public interface WorldCarver {
   Codec<WorldCarver> DIRECT_CODEC = BuiltInRegistries.CARVER_TYPE.byNameCodec().dispatch(WorldCarver::codec, Function.identity());
   Codec<Holder<WorldCarver>> CODEC = RegistryFileCodec.create(Registries.CARVER, DIRECT_CODEC);
   Codec<HolderSet<WorldCarver>> LIST_CODEC = RegistryCodecs.homogeneousList(Registries.CARVER, DIRECT_CODEC);

   default int getRange() {
      return 4;
   }

   static void carveEllipsoid(
      final ChunkPos chunkPos,
      final double x,
      final double y,
      final double z,
      final double horizontalRadius,
      final double verticalRadius,
      final CarverOutput output,
      final WorldCarver.CarveSkipChecker skipChecker
   ) {
      double centerX = chunkPos.getMiddleBlockX();
      double centerZ = chunkPos.getMiddleBlockZ();
      double maxDelta = 16.0 + horizontalRadius * 2.0;
      if (!(Math.abs(x - centerX) > maxDelta) && !(Math.abs(z - centerZ) > maxDelta)) {
         int chunkMinX = chunkPos.getMinBlockX();
         int chunkMinZ = chunkPos.getMinBlockZ();
         int minXIndex = Math.max(Mth.floor(x - horizontalRadius) - chunkMinX - 1, 0);
         int maxXIndex = Math.min(Mth.floor(x + horizontalRadius) - chunkMinX, 15);
         int minY = Math.max(Mth.floor(y - verticalRadius) - 1, output.minY());
         int maxY = Math.min(Mth.floor(y + verticalRadius) + 1, output.maxY());
         int minZIndex = Math.max(Mth.floor(z - horizontalRadius) - chunkMinZ - 1, 0);
         int maxZIndex = Math.min(Mth.floor(z + horizontalRadius) - chunkMinZ, 15);

         for (int xIndex = minXIndex; xIndex <= maxXIndex; xIndex++) {
            int worldX = chunkPos.getBlockX(xIndex);
            double xd = (worldX + 0.5 - x) / horizontalRadius;

            for (int zIndex = minZIndex; zIndex <= maxZIndex; zIndex++) {
               int worldZ = chunkPos.getBlockZ(zIndex);
               double zd = (worldZ + 0.5 - z) / horizontalRadius;
               if (!(xd * xd + zd * zd >= 1.0)) {
                  for (int worldY = maxY; worldY > minY; worldY--) {
                     double yd = (worldY - 0.5 - y) / verticalRadius;
                     if (!skipChecker.shouldSkip(xd, yd, zd, worldY)) {
                        output.carve(xIndex, worldY, zIndex);
                     }
                  }
               }
            }
         }
      }
   }

   boolean carve(WorldGenerationContext context, RandomSource random, ChunkPos chunkPos, ChunkPos sourceChunkPos, CarverOutput output);

   boolean isStartChunk(RandomSource random);

   MapCodec<? extends WorldCarver> codec();

   static boolean canReach(final ChunkPos chunkPos, final double x, final double z, final int currentStep, final int totalSteps, final float thickness) {
      double xMid = chunkPos.getMiddleBlockX();
      double zMid = chunkPos.getMiddleBlockZ();
      double xd = x - xMid;
      double zd = z - zMid;
      double remaining = totalSteps - currentStep;
      double rr = thickness + 2.0F + 16.0F;
      return xd * xd + zd * zd - remaining * remaining <= rr * rr;
   }

   interface CarveSkipChecker {
      boolean shouldSkip(double xd, double yd, double zd, int y);
   }
}
