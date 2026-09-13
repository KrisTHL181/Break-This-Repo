package net.minecraft.world.level.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import org.jspecify.annotations.Nullable;

public abstract class BiomeSource implements BiomeResolver {
   public static final Codec<BiomeSource> CODEC = BuiltInRegistries.BIOME_SOURCE.byNameCodec().dispatchStable(BiomeSource::codec, Function.identity());
   private final Supplier<Set<Holder<Biome>>> possibleBiomes = Suppliers.memoize(
      () -> this.collectPossibleBiomes().distinct().collect(ImmutableSet.toImmutableSet())
   );

   protected BiomeSource() {
   }

   protected abstract MapCodec<? extends BiomeSource> codec();

   protected abstract Stream<Holder<Biome>> collectPossibleBiomes();

   public Set<Holder<Biome>> possibleBiomes() {
      return this.possibleBiomes.get();
   }

   public Set<Holder<Biome>> getBiomesWithin(final int x, final int y, final int z, final int r, final Climate.Sampler sampler) {
      int x0 = QuartPos.fromBlock(x - r);
      int y0 = QuartPos.fromBlock(y - r);
      int z0 = QuartPos.fromBlock(z - r);
      int x1 = QuartPos.fromBlock(x + r);
      int y1 = QuartPos.fromBlock(y + r);
      int z1 = QuartPos.fromBlock(z + r);
      int w = x1 - x0 + 1;
      int d = y1 - y0 + 1;
      int h = z1 - z0 + 1;
      Set<Holder<Biome>> biomeSet = Sets.newHashSet();

      for (int row = 0; row < h; row++) {
         for (int column = 0; column < w; column++) {
            for (int depth = 0; depth < d; depth++) {
               int noiseX = x0 + column;
               int noiseY = y0 + depth;
               int noiseZ = z0 + row;
               biomeSet.add(this.getNoiseBiome(noiseX, noiseY, noiseZ, sampler));
            }
         }
      }

      return biomeSet;
   }

   public @Nullable Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(
      final int x,
      final int y,
      final int z,
      final int searchRadius,
      final Predicate<Holder<Biome>> allowed,
      final RandomSource random,
      final Climate.Sampler sampler
   ) {
      return this.findBiomeHorizontal(x, y, z, searchRadius, 1, allowed, random, false, sampler);
   }

   public @Nullable Pair<BlockPos, Holder<Biome>> findClosestBiome3d(
      final BlockPos origin,
      final int searchRadius,
      final int sampleResolutionHorizontal,
      final int sampleResolutionVertical,
      final Predicate<Holder<Biome>> allowed,
      final Climate.Sampler sampler,
      final LevelReader level
   ) {
      Set<Holder<Biome>> candidateBiomes = this.possibleBiomes().stream().filter(allowed).collect(Collectors.toUnmodifiableSet());
      if (candidateBiomes.isEmpty()) {
         return null;
      }

      int sampleRadius = Math.floorDiv(searchRadius, sampleResolutionHorizontal);
      int[] sampleYs = Mth.outFromOrigin(origin.getY(), level.getMinY() + 1, level.getMaxY() + 1, sampleResolutionVertical).toArray();

      for (BlockPos.MutableBlockPos sampleColumn : BlockPos.spiralAround(BlockPos.ZERO, sampleRadius, Direction.EAST, Direction.SOUTH)) {
         int blockX = origin.getX() + sampleColumn.getX() * sampleResolutionHorizontal;
         int blockZ = origin.getZ() + sampleColumn.getZ() * sampleResolutionHorizontal;
         int noiseX = QuartPos.fromBlock(blockX);
         int noiseZ = QuartPos.fromBlock(blockZ);

         for (int blockY : sampleYs) {
            int noiseY = QuartPos.fromBlock(blockY);
            Holder<Biome> biome = this.getNoiseBiome(noiseX, noiseY, noiseZ, sampler);
            if (candidateBiomes.contains(biome)) {
               return Pair.of(new BlockPos(blockX, blockY, blockZ), biome);
            }
         }
      }

      return null;
   }

   public @Nullable Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(
      final int originX,
      final int originY,
      final int originZ,
      final int searchRadius,
      final int skipSteps,
      final Predicate<Holder<Biome>> allowed,
      final RandomSource random,
      final boolean findClosest,
      final Climate.Sampler sampler
   ) {
      int noiseCenterX = QuartPos.fromBlock(originX);
      int noiseCenterZ = QuartPos.fromBlock(originZ);
      int noiseRadius = QuartPos.fromBlock(searchRadius);
      int noiseY = QuartPos.fromBlock(originY);
      Pair<BlockPos, Holder<Biome>> result = null;
      int found = 0;
      int startRadius = findClosest ? 0 : noiseRadius;
      int currentRadius = startRadius;

      while (currentRadius <= noiseRadius) {
         for (int z = !SharedConstants.DEBUG_ONLY_GENERATE_HALF_THE_WORLD && !SharedConstants.debugGenerateSquareTerrainWithoutNoise ? -currentRadius : 0;
            z <= currentRadius;
            z += skipSteps
         ) {
            boolean zEdge = Math.abs(z) == currentRadius;

            for (int x = -currentRadius; x <= currentRadius; x += skipSteps) {
               if (findClosest) {
                  boolean xEdge = Math.abs(x) == currentRadius;
                  if (!xEdge && !zEdge) {
                     continue;
                  }
               }

               int noiseX = noiseCenterX + x;
               int noiseZ = noiseCenterZ + z;
               Holder<Biome> biome = this.getNoiseBiome(noiseX, noiseY, noiseZ, sampler);
               if (allowed.test(biome)) {
                  if (result == null || random.nextInt(found + 1) == 0) {
                     BlockPos resultPos = new BlockPos(QuartPos.toBlock(noiseX), originY, QuartPos.toBlock(noiseZ));
                     if (findClosest) {
                        return Pair.of(resultPos, biome);
                     }

                     result = Pair.of(resultPos, biome);
                  }

                  found++;
               }
            }
         }

         currentRadius += skipSteps;
      }

      return result;
   }

   @Override
   public abstract Holder<Biome> getNoiseBiome(final int quartX, final int quartY, final int quartZ, final Climate.Sampler sampler);

   public void addDebugInfo(final List<String> result, final BlockPos feetPos, final Climate.Sampler sampler) {
   }
}
