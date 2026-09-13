package net.minecraft.world.level.biome;

import com.google.common.hash.Hashing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;

public class BiomeManager {
   public static final int CHUNK_CENTER_QUART = QuartPos.fromBlock(8);
   private static final int ZOOM_BITS = 2;
   private static final int ZOOM = 4;
   private static final int ZOOM_MASK = 3;
   private final BiomeManager.NoiseBiomeSource noiseBiomeSource;
   private final long biomeZoomSeed;

   public BiomeManager(final BiomeManager.NoiseBiomeSource noiseBiomeSource, final long seed) {
      this.noiseBiomeSource = noiseBiomeSource;
      this.biomeZoomSeed = seed;
   }

   public static long obfuscateSeed(final long seed) {
      return Hashing.sha256().hashLong(seed).asLong();
   }

   public BiomeManager withDifferentSource(final BiomeManager.NoiseBiomeSource biomeSource) {
      return new BiomeManager(biomeSource, this.biomeZoomSeed);
   }

   public Holder<Biome> getBiome(final BlockPos pos) {
      int absX = pos.getX() - 2;
      int absY = pos.getY() - 2;
      int absZ = pos.getZ() - 2;
      int parentX = absX >> 2;
      int parentY = absY >> 2;
      int parentZ = absZ >> 2;
      double fractX = (absX & 3) / 4.0;
      double fractY = (absY & 3) / 4.0;
      double fractZ = (absZ & 3) / 4.0;
      int minI = 0;
      double minFiddledDistance = Double.POSITIVE_INFINITY;

      for (int i = 0; i < 8; i++) {
         boolean xEven = (i & 4) == 0;
         boolean yEven = (i & 2) == 0;
         boolean zEven = (i & 1) == 0;
         int cornerX = xEven ? parentX : parentX + 1;
         int cornerY = yEven ? parentY : parentY + 1;
         int cornerZ = zEven ? parentZ : parentZ + 1;
         double distanceX = xEven ? fractX : fractX - 1.0;
         double distanceY = yEven ? fractY : fractY - 1.0;
         double distanceZ = zEven ? fractZ : fractZ - 1.0;
         double next = getFiddledDistance(this.biomeZoomSeed, cornerX, cornerY, cornerZ, distanceX, distanceY, distanceZ);
         if (minFiddledDistance > next) {
            minI = i;
            minFiddledDistance = next;
         }
      }

      int biomeX = (minI & 4) == 0 ? parentX : parentX + 1;
      int biomeY = (minI & 2) == 0 ? parentY : parentY + 1;
      int biomeZ = (minI & 1) == 0 ? parentZ : parentZ + 1;
      return this.noiseBiomeSource.getNoiseBiome(biomeX, biomeY, biomeZ);
   }

   public Holder<Biome> getNoiseBiomeAtPosition(final double x, final double y, final double z) {
      int quartX = QuartPos.fromBlock(Mth.floor(x));
      int quartY = QuartPos.fromBlock(Mth.floor(y));
      int quartZ = QuartPos.fromBlock(Mth.floor(z));
      return this.getNoiseBiomeAtQuart(quartX, quartY, quartZ);
   }

   public Holder<Biome> getNoiseBiomeAtPosition(final BlockPos blockPos) {
      int quartX = QuartPos.fromBlock(blockPos.getX());
      int quartY = QuartPos.fromBlock(blockPos.getY());
      int quartZ = QuartPos.fromBlock(blockPos.getZ());
      return this.getNoiseBiomeAtQuart(quartX, quartY, quartZ);
   }

   public Holder<Biome> getNoiseBiomeAtQuart(final int quartX, final int quartY, final int quartZ) {
      return this.noiseBiomeSource.getNoiseBiome(quartX, quartY, quartZ);
   }

   private static double getFiddledDistance(
      final long seed, final int xRandom, final int yRandom, final int zRandom, final double distanceX, final double distanceY, final double distanceZ
   ) {
      long rval = seed;
      rval = LinearCongruentialGenerator.next(rval, xRandom);
      rval = LinearCongruentialGenerator.next(rval, yRandom);
      rval = LinearCongruentialGenerator.next(rval, zRandom);
      rval = LinearCongruentialGenerator.next(rval, xRandom);
      rval = LinearCongruentialGenerator.next(rval, yRandom);
      rval = LinearCongruentialGenerator.next(rval, zRandom);
      double fiddleX = getFiddle(rval);
      rval = LinearCongruentialGenerator.next(rval, seed);
      double fiddleY = getFiddle(rval);
      rval = LinearCongruentialGenerator.next(rval, seed);
      double fiddleZ = getFiddle(rval);
      return Mth.square(distanceZ + fiddleZ) + Mth.square(distanceY + fiddleY) + Mth.square(distanceX + fiddleX);
   }

   private static double getFiddle(final long rval) {
      double uniform = Math.floorMod(rval >> 24, 1024) / 1024.0;
      return (uniform - 0.5) * 0.9;
   }

   public interface NoiseBiomeSource {
      Holder<Biome> getNoiseBiome(final int quartX, final int quartY, final int quartZ);
   }
}
