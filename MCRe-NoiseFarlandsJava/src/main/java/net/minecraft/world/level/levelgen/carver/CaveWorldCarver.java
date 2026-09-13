package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class CaveWorldCarver extends WorldCarver<CaveCarverConfiguration> {
    public CaveWorldCarver(final Codec<CaveCarverConfiguration> configurationFactory) {
        super(configurationFactory);
    }

    public boolean isStartChunk(final CaveCarverConfiguration configuration, final RandomSource random) {
        return random.nextFloat() <= configuration.probability;
    }

    public boolean carve(
        final CarvingContext context,
        final CaveCarverConfiguration configuration,
        final ChunkAccess chunk,
        final Function<BlockPos, Holder<Biome>> biomeGetter,
        final RandomSource random,
        final Aquifer aquifer,
        final ChunkPos sourceChunkPos,
        final CarvingMask mask
    ) {
        int maxDistance = SectionPos.sectionToBlockCoord(this.getRange() * 2 - 1);
        int caveCount = random.nextInt(random.nextInt(random.nextInt(this.getCaveBound()) + 1) + 1);

        for (int cave = 0; cave < caveCount; cave++) {
            double x = sourceChunkPos.getBlockX(random.nextInt(16));
            double y = configuration.y.sample(random, context);
            double z = sourceChunkPos.getBlockZ(random.nextInt(16));
            double horizontalRadiusMultiplier = configuration.horizontalRadiusMultiplier.sample(random);
            double verticalRadiusMultiplier = configuration.verticalRadiusMultiplier.sample(random);
            double floorLevel = configuration.floorLevel.sample(random);
            WorldCarver.CarveSkipChecker skipChecker = (c, xd, yd, zd, worldY) -> shouldSkip(xd, yd, zd, floorLevel);
            int tunnels = 1;
            if (random.nextInt(4) == 0) {
                double yScale = configuration.yScale.sample(random);
                float thickness = 1.0F + random.nextFloat() * 6.0F;
                this.createRoom(context, configuration, chunk, biomeGetter, aquifer, x, y, z, thickness, yScale, mask, skipChecker);
                tunnels += random.nextInt(4);
            }

            for (int i = 0; i < tunnels; i++) {
                float horizontalRotation = random.nextFloat() * (float) (Math.PI * 2);
                float verticalRotation = (random.nextFloat() - 0.5F) / 4.0F;
                float thickness = this.getThickness(random);
                int distance = maxDistance - random.nextInt(maxDistance / 4);
                int initialStep = 0;
                this.createTunnel(
                    context,
                    configuration,
                    chunk,
                    biomeGetter,
                    random.nextLong(),
                    aquifer,
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
                    this.getYScale(),
                    mask,
                    skipChecker
                );
            }
        }

        return true;
    }

    protected int getCaveBound() {
        return 15;
    }

    protected float getThickness(final RandomSource random) {
        float thickness = random.nextFloat() * 2.0F + random.nextFloat();
        if (random.nextInt(10) == 0) {
            thickness *= random.nextFloat() * random.nextFloat() * 3.0F + 1.0F;
        }

        return thickness;
    }

    protected double getYScale() {
        return 1.0;
    }

    protected void createRoom(
        final CarvingContext context,
        final CaveCarverConfiguration configuration,
        final ChunkAccess chunk,
        final Function<BlockPos, Holder<Biome>> biomeGetter,
        final Aquifer aquifer,
        final double x,
        final double y,
        final double z,
        final float thickness,
        final double yScale,
        final CarvingMask mask,
        final WorldCarver.CarveSkipChecker skipChecker
    ) {
        double horizontalRadius = 1.5 + Mth.sin((float) (Math.PI / 2)) * thickness;
        double verticalRadius = horizontalRadius * yScale;
        this.carveEllipsoid(context, configuration, chunk, biomeGetter, aquifer, x + 1.0, y, z, horizontalRadius, verticalRadius, mask, skipChecker);
    }

    protected void createTunnel(
        final CarvingContext context,
        final CaveCarverConfiguration configuration,
        final ChunkAccess chunk,
        final Function<BlockPos, Holder<Biome>> biomeGetter,
        final long tunnelSeed,
        final Aquifer aquifer,
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
        final CarvingMask mask,
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
                    context,
                    configuration,
                    chunk,
                    biomeGetter,
                    random.nextLong(),
                    aquifer,
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
                    mask,
                    skipChecker
                );
                this.createTunnel(
                    context,
                    configuration,
                    chunk,
                    biomeGetter,
                    random.nextLong(),
                    aquifer,
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
                    mask,
                    skipChecker
                );
                return;
            }

            if (random.nextInt(4) != 0) {
                if (!canReach(chunk.getPos(), x, z, currentStep, dist, thickness)) {
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
                    horizontalRadius * horizontalRadiusMultiplier,
                    verticalRadius * verticalRadiusMultiplier,
                    mask,
                    skipChecker
                );
            }
        }
    }

    private static boolean shouldSkip(final double xd, final double yd, final double zd, final double floorLevel) {
        return yd <= floorLevel ? true : xd * xd + yd * yd + zd * zd >= 1.0;
    }
}