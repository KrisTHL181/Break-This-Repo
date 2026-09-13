package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class CanyonWorldCarver extends WorldCarver<CanyonCarverConfiguration> {
    public CanyonWorldCarver(final Codec<CanyonCarverConfiguration> configurationFactory) {
        super(configurationFactory);
    }

    public boolean isStartChunk(final CanyonCarverConfiguration configuration, final RandomSource random) {
        return random.nextFloat() <= configuration.probability;
    }

    public boolean carve(
        final CarvingContext context,
        final CanyonCarverConfiguration configuration,
        final ChunkAccess chunk,
        final Function<BlockPos, Holder<Biome>> biomeGetter,
        final RandomSource random,
        final Aquifer aquifer,
        final ChunkPos sourceChunkPos,
        final CarvingMask mask
    ) {
        int maxDistance = (this.getRange() * 2 - 1) * 16;
        double x = sourceChunkPos.getBlockX(random.nextInt(16));
        int y = configuration.y.sample(random, context);
        double z = sourceChunkPos.getBlockZ(random.nextInt(16));
        float horizontalRotation = random.nextFloat() * (float) (Math.PI * 2);
        float verticalRotation = configuration.verticalRotation.sample(random);
        double yScale = configuration.yScale.sample(random);
        float thickness = configuration.shape.thickness.sample(random);
        int distance = (int)(maxDistance * configuration.shape.distanceFactor.sample(random));
        int initialStep = 0;
        this.doCarve(
            context,
            configuration,
            chunk,
            biomeGetter,
            random.nextLong(),
            aquifer,
            x,
            y,
            z,
            thickness,
            horizontalRotation,
            verticalRotation,
            0,
            distance,
            yScale,
            mask
        );
        return true;
    }

    private void doCarve(
        final CarvingContext context,
        final CanyonCarverConfiguration configuration,
        final ChunkAccess chunk,
        final Function<BlockPos, Holder<Biome>> biomeGetter,
        final long tunnelSeed,
        final Aquifer aquifer,
        double x,
        double y,
        double z,
        final float thickness,
        float horizontalRotation,
        float verticalRotation,
        final int step,
        final int distance,
        final double yScale,
        final CarvingMask mask
    ) {
        RandomSource random = RandomSource.createThreadLocalInstance(tunnelSeed);
        float[] widthFactorPerHeight = this.initWidthFactors(context, configuration, random);
        float yRota = 0.0F;
        float xRota = 0.0F;

        for (int currentStep = step; currentStep < distance; currentStep++) {
            double horizontalRadius = 1.5 + Mth.sin(currentStep * (float) Math.PI / distance) * thickness;
            double verticalRadius = horizontalRadius * yScale;
            horizontalRadius *= configuration.shape.horizontalRadiusFactor.sample(random);
            verticalRadius = this.updateVerticalRadius(configuration, random, verticalRadius, distance, currentStep);
            float xc = Mth.cos(verticalRotation);
            float xs = Mth.sin(verticalRotation);
            x += Mth.cos(horizontalRotation) * xc;
            y += xs;
            z += Mth.sin(horizontalRotation) * xc;
            verticalRotation *= 0.7F;
            verticalRotation += xRota * 0.05F;
            horizontalRotation += yRota * 0.05F;
            xRota *= 0.8F;
            yRota *= 0.5F;
            xRota += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yRota += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
            if (random.nextInt(4) != 0) {
                if (!canReach(chunk.getPos(), x, z, currentStep, distance, thickness)) {
                    return;
                }

                this.carveEllipsoid(
                    context,
                    configuration,
                    chunk,
                    biomeGetter,
                    aquifer,
                    x,
                    y,
                    z,
                    horizontalRadius,
                    verticalRadius,
                    mask,
                    (context1, xd, yd, zd, y1) -> this.shouldSkip(context1, widthFactorPerHeight, xd, yd, zd, y1)
                );
            }
        }
    }

    private float[] initWidthFactors(final CarvingContext context, final CanyonCarverConfiguration configuration, final RandomSource random) {
        int depth = context.getGenDepth();
        float[] widthFactorPerHeight = new float[depth];
        float widthFactor = 1.0F;

        for (int yIndex = 0; yIndex < depth; yIndex++) {
            if (yIndex == 0 || random.nextInt(configuration.shape.widthSmoothness) == 0) {
                widthFactor = 1.0F + random.nextFloat() * random.nextFloat();
            }

            widthFactorPerHeight[yIndex] = widthFactor * widthFactor;
        }

        return widthFactorPerHeight;
    }

    private double updateVerticalRadius(
        final CanyonCarverConfiguration configuration, final RandomSource random, final double verticalRadius, final float distance, final float currentStep
    ) {
        float verticalMultiplier = 1.0F - Mth.abs(0.5F - currentStep / distance) * 2.0F;
        float factor = configuration.shape.verticalRadiusDefaultFactor + configuration.shape.verticalRadiusCenterFactor * verticalMultiplier;
        return factor * verticalRadius * Mth.randomBetween(random, 0.75F, 1.0F);
    }

    private boolean shouldSkip(final CarvingContext context, final float[] widthFactorPerHeight, final double xd, final double yd, final double zd, final int y) {
        int yIndex = y - context.getMinGenY();
        return (xd * xd + zd * zd) * widthFactorPerHeight[yIndex - 1] + yd * yd / 6.0 >= 1.0;
    }
}