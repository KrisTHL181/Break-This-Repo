package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.CarverOutput;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public record CaveWorldCarver(
   float probability,
   HeightProvider y,
   IntProvider count,
   FloatProvider thickness,
   boolean weirdThicknessBias,
   FloatProvider roomVerticalRadiusMultiplier,
   FloatProvider horizontalRadiusMultiplier,
   FloatProvider verticalRadiusMultiplier,
   FloatProvider startVerticalRadiusMultiplier,
   FloatProvider floorLevel
) implements WorldCarver {
   public static final MapCodec<CaveWorldCarver> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(c -> c.probability),
            HeightProvider.CODEC.fieldOf("y").forGetter(c -> c.y),
            IntProviders.NON_NEGATIVE_CODEC.fieldOf("count").forGetter(c -> c.count),
            FloatProviders.codec(0.0F).fieldOf("thickness").forGetter(c -> c.thickness),
            Codec.BOOL.optionalFieldOf("weird_thickness_bias", false).forGetter(c -> c.weirdThicknessBias),
            FloatProviders.CODEC.fieldOf("room_vertical_radius_multiplier").forGetter(c -> c.roomVerticalRadiusMultiplier),
            FloatProviders.CODEC.fieldOf("horizontal_radius_multiplier").forGetter(c -> c.horizontalRadiusMultiplier),
            FloatProviders.CODEC.fieldOf("vertical_radius_multiplier").forGetter(c -> c.verticalRadiusMultiplier),
            FloatProviders.CODEC.optionalFieldOf("start_vertical_radius_multiplier", ConstantFloat.of(1.0F)).forGetter(c -> c.startVerticalRadiusMultiplier),
            FloatProviders.codec(-1.0F, 1.0F).fieldOf("floor_level").forGetter(c -> c.floorLevel)
         )
         .apply(i, CaveWorldCarver::new)
   );

   @Override
   public boolean isStartChunk(final RandomSource random) {
      return random.nextFloat() <= this.probability;
   }

   @Override
   public boolean carve(
      final WorldGenerationContext context, final RandomSource random, final ChunkPos chunkPos, final ChunkPos sourceChunkPos, final CarverOutput output
   ) {
      int maxDistance = SectionPos.sectionToBlockCoord(this.getRange() * 2 - 1);
      int caveCount = this.count.sample(random);

      for (int cave = 0; cave < caveCount; cave++) {
         double x = sourceChunkPos.getBlockX(random.nextInt(16));
         double y = this.y.sample(random, context);
         double z = sourceChunkPos.getBlockZ(random.nextInt(16));
         double horizontalRadiusMultiplier = this.horizontalRadiusMultiplier.sample(random);
         double verticalRadiusMultiplier = this.verticalRadiusMultiplier.sample(random);
         double startVerticalRadiusMultiplier = this.startVerticalRadiusMultiplier.sample(random);
         double floorLevel = this.floorLevel.sample(random);
         WorldCarver.CarveSkipChecker skipChecker = (xd, yd, zd, worldY) -> shouldSkip(xd, yd, zd, floorLevel);
         int tunnels = 1;
         if (random.nextInt(4) == 0) {
            double yScale = this.roomVerticalRadiusMultiplier.sample(random);
            float thickness = 1.0F + random.nextFloat() * 6.0F;
            this.createRoom(chunkPos, x, y, z, thickness, yScale, output, skipChecker);
            tunnels += random.nextInt(4);
         }

         for (int i = 0; i < tunnels; i++) {
            float horizontalRotation = random.nextFloat() * (float) (Math.PI * 2);
            float verticalRotation = (random.nextFloat() - 0.5F) / 4.0F;
            float thickness = this.getThickness(random);
            int distance = maxDistance - random.nextInt(maxDistance / 4);
            int initialStep = 0;
            this.createTunnel(
               chunkPos,
               random.nextLong(),
               x,
               y,
               z,
               horizontalRadiusMultiplier,
               verticalRadiusMultiplier,
               thickness,
               horizontalRotation,
               verticalRotation,
               0,
               distance,
               startVerticalRadiusMultiplier,
               output,
               skipChecker
            );
         }
      }

      return true;
   }

   private float getThickness(final RandomSource random) {
      float thickness = this.thickness.sample(random);
      if (this.weirdThicknessBias && random.nextInt(10) == 0) {
         thickness *= random.nextFloat() * random.nextFloat() * 3.0F + 1.0F;
      }

      return thickness;
   }

   private void createRoom(
      final ChunkPos chunkPos,
      final double x,
      final double y,
      final double z,
      final float thickness,
      final double yScale,
      final CarverOutput output,
      final WorldCarver.CarveSkipChecker skipChecker
   ) {
      double horizontalRadius = 1.5 + Mth.sin((float) (Math.PI / 2)) * thickness;
      double verticalRadius = horizontalRadius * yScale;
      WorldCarver.carveEllipsoid(chunkPos, x + 1.0, y, z, horizontalRadius, verticalRadius, output, skipChecker);
   }

   private void createTunnel(
      final ChunkPos chunkPos,
      final long tunnelSeed,
      double x,
      double y,
      double z,
      final double horizontalRadiusMultiplier,
      final double verticalRadiusMultiplier,
      final float thickness,
      float horizontalRotation,
      float verticalRotation,
      final int step,
      final int dist,
      final double yScale,
      final CarverOutput output,
      final WorldCarver.CarveSkipChecker skipChecker
   ) {
      RandomSource random = RandomSource.createThreadLocalInstance(tunnelSeed);
      int splitPoint = random.nextInt(dist / 2) + dist / 4;
      boolean steep = random.nextInt(6) == 0;
      float yRota = 0.0F;
      float xRota = 0.0F;

      for (int currentStep = step; currentStep < dist; currentStep++) {
         double horizontalRadius = 1.5 + Mth.sin((float) Math.PI * currentStep / dist) * thickness;
         double verticalRadius = horizontalRadius * yScale;
         float cosX = Mth.cos(verticalRotation);
         x += Mth.cos(horizontalRotation) * cosX;
         y += Mth.sin(verticalRotation);
         z += Mth.sin(horizontalRotation) * cosX;
         verticalRotation *= steep ? 0.92F : 0.7F;
         verticalRotation += xRota * 0.1F;
         horizontalRotation += yRota * 0.1F;
         xRota *= 0.9F;
         yRota *= 0.75F;
         xRota += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
         yRota += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
         if (currentStep == splitPoint && thickness > 1.0F) {
            this.createTunnel(
               chunkPos,
               random.nextLong(),
               x,
               y,
               z,
               horizontalRadiusMultiplier,
               verticalRadiusMultiplier,
               random.nextFloat() * 0.5F + 0.5F,
               horizontalRotation - (float) (Math.PI / 2),
               verticalRotation / 3.0F,
               currentStep,
               dist,
               1.0,
               output,
               skipChecker
            );
            this.createTunnel(
               chunkPos,
               random.nextLong(),
               x,
               y,
               z,
               horizontalRadiusMultiplier,
               verticalRadiusMultiplier,
               random.nextFloat() * 0.5F + 0.5F,
               horizontalRotation + (float) (Math.PI / 2),
               verticalRotation / 3.0F,
               currentStep,
               dist,
               1.0,
               output,
               skipChecker
            );
            return;
         }

         if (random.nextInt(4) != 0) {
            if (!WorldCarver.canReach(chunkPos, x, z, currentStep, dist, thickness)) {
               return;
            }

            WorldCarver.carveEllipsoid(
               chunkPos, x, y, z, horizontalRadius * horizontalRadiusMultiplier, verticalRadius * verticalRadiusMultiplier, output, skipChecker
            );
         }
      }
   }

   private static boolean shouldSkip(final double xd, final double yd, final double zd, final double floorLevel) {
      return yd <= floorLevel ? true : xd * xd + yd * yd + zd * zd >= 1.0;
   }

   @Override
   public MapCodec<CaveWorldCarver> codec() {
      return MAP_CODEC;
   }
}
