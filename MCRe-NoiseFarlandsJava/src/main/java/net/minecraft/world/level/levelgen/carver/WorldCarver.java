package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

public abstract class WorldCarver<C extends CarverConfiguration> {
    public static final WorldCarver<CaveCarverConfiguration> CAVE = register("cave", new CaveWorldCarver(CaveCarverConfiguration.CODEC));
    public static final WorldCarver<CaveCarverConfiguration> NETHER_CAVE = register("nether_cave", new NetherWorldCarver(CaveCarverConfiguration.CODEC));
    public static final WorldCarver<CanyonCarverConfiguration> CANYON = register("canyon", new CanyonWorldCarver(CanyonCarverConfiguration.CODEC));
    protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
    protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
    protected static final FluidState WATER = Fluids.WATER.defaultFluidState();
    protected static final FluidState LAVA = Fluids.LAVA.defaultFluidState();
    protected Set<Fluid> liquids = ImmutableSet.of(Fluids.WATER);
    private final MapCodec<ConfiguredWorldCarver<C>> configuredCodec;

    private static <C extends CarverConfiguration, F extends WorldCarver<C>> F register(final String name, final F carver) {
        return Registry.register(BuiltInRegistries.CARVER, name, carver);
    }

    public WorldCarver(final Codec<C> codec) {
        this.configuredCodec = codec.fieldOf("config").xmap(this::configured, ConfiguredWorldCarver::config);
    }

    public ConfiguredWorldCarver<C> configured(final C configuration) {
        return new ConfiguredWorldCarver<>(this, configuration);
    }

    public MapCodec<ConfiguredWorldCarver<C>> configuredCodec() {
        return this.configuredCodec;
    }

    public int getRange() {
        return 4;
    }

    protected boolean carveEllipsoid(
        final CarvingContext context,
        final C configuration,
        final ChunkAccess chunk,
        final Function<BlockPos, Holder<Biome>> biomeGetter,
        final Aquifer aquifer,
        final double x,
        final double y,
        final double z,
        final double horizontalRadius,
        final double verticalRadius,
        final CarvingMask mask,
        final WorldCarver.CarveSkipChecker skipChecker
    ) {
        ChunkPos chunkPos = chunk.getPos();
        double centerX = (int)chunkPos.getMiddleBlockX();
        double centerZ = (int)chunkPos.getMiddleBlockZ();
        double maxDelta = 16.0 + horizontalRadius * 2.0;
        if (!(Math.abs(x - centerX) > maxDelta) && !(Math.abs(z - centerZ) > maxDelta)) {
            int chunkMinX = (int)chunkPos.getMinBlockX();
            int chunkMinZ = (int)chunkPos.getMinBlockZ();
            int minXIndex = Math.max(Mth.floor(x - horizontalRadius) - chunkMinX - 1, 0);
            int maxXIndex = Math.min(Mth.floor(x + horizontalRadius) - chunkMinX, 15);
            int minY = Math.max(Mth.floor(y - verticalRadius) - 1, context.getMinGenY() + 1);
            int protectedBlocksOnTop = chunk.isUpgrading() ? 0 : 7;
            int maxY = Math.min(Mth.floor(y + verticalRadius) + 1, context.getMinGenY() + context.getGenDepth() - 1 - protectedBlocksOnTop);
            int minZIndex = Math.max(Mth.floor(z - horizontalRadius) - chunkMinZ - 1, 0);
            int maxZIndex = Math.min(Mth.floor(z + horizontalRadius) - chunkMinZ, 15);
            boolean carved = false;
            BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos helperPos = new BlockPos.MutableBlockPos();

            for (int xIndex = minXIndex; xIndex <= maxXIndex; xIndex++) {
                int worldX = (int)chunkPos.getBlockX(xIndex);
                double xd = (worldX + 0.5 - x) / horizontalRadius;

                for (int zIndex = minZIndex; zIndex <= maxZIndex; zIndex++) {
                    int worldZ = (int)chunkPos.getBlockZ(zIndex);
                    double zd = (worldZ + 0.5 - z) / horizontalRadius;
                    if (!(xd * xd + zd * zd >= 1.0)) {
                        MutableBoolean hasGrass = new MutableBoolean(false);

                        for (int worldY = maxY; worldY > minY; worldY--) {
                            double yd = (worldY - 0.5 - y) / verticalRadius;
                            if (!skipChecker.shouldSkip(context, xd, yd, zd, worldY) && (!mask.get(xIndex, worldY, zIndex) || isDebugEnabled(configuration))) {
                                mask.set(xIndex, worldY, zIndex);
                                blockPos.set(worldX, worldY, worldZ);
                                carved |= this.carveBlock(context, configuration, chunk, biomeGetter, mask, blockPos, helperPos, aquifer, hasGrass);
                            }
                        }
                    }
                }
            }

            return carved;
        } else {
            return false;
        }
    }

    protected boolean carveBlock(
        final CarvingContext context,
        final C configuration,
        final ChunkAccess chunk,
        final Function<BlockPos, Holder<Biome>> biomeGetter,
        final CarvingMask mask,
        final BlockPos.MutableBlockPos blockPos,
        final BlockPos.MutableBlockPos helperPos,
        final Aquifer aquifer,
        final MutableBoolean hasGrass
    ) {
        BlockState blockState = chunk.getBlockState(blockPos);
        if (blockState.is(Blocks.GRASS_BLOCK) || blockState.is(Blocks.MYCELIUM)) {
            hasGrass.setTrue();
        }

        if (!this.canReplaceBlock(configuration, blockState) && !isDebugEnabled(configuration)) {
            return false;
        }

        BlockState state = this.getCarveState(context, configuration, blockPos, aquifer);
        if (state == null) {
            return false;
        }

        chunk.setBlockState(blockPos, state);
        if (aquifer.shouldScheduleFluidUpdate() && !state.getFluidState().isEmpty()) {
            chunk.markPosForPostProcessing(blockPos);
        }

        if (hasGrass.isTrue()) {
            helperPos.setWithOffset(blockPos, Direction.DOWN);
            if (chunk.getBlockState(helperPos).is(Blocks.DIRT)) {
                context.topMaterial(biomeGetter, chunk, helperPos, !state.getFluidState().isEmpty()).ifPresent(topMaterial -> {
                    chunk.setBlockState(helperPos, topMaterial);
                    if (!topMaterial.getFluidState().isEmpty()) {
                        chunk.markPosForPostProcessing(helperPos);
                    }
                });
            }
        }

        return true;
    }

    private @Nullable BlockState getCarveState(final CarvingContext context, final C configuration, final BlockPos blockPos, final Aquifer aquifer) {
        if (blockPos.getY() <= configuration.lavaLevel.resolveY(context)) {
            return LAVA.createLegacyBlock();
        } else {
            BlockState state = aquifer.computeSubstance(new DensityFunction.SinglePointContext(blockPos.getX(), blockPos.getY(), blockPos.getZ()), 0.0);
            if (state == null) {
                return isDebugEnabled(configuration) ? configuration.debugSettings.getBarrierState() : null;
            } else {
                return isDebugEnabled(configuration) ? getDebugState(configuration, state) : state;
            }
        }
    }

    private static BlockState getDebugState(final CarverConfiguration configuration, final BlockState state) {
        if (state.is(Blocks.AIR)) {
            return configuration.debugSettings.getAirState();
        } else if (state.is(Blocks.WATER)) {
            BlockState debugState = configuration.debugSettings.getWaterState();
            return debugState.hasProperty(BlockStateProperties.WATERLOGGED) ? debugState.setValue(BlockStateProperties.WATERLOGGED, true) : debugState;
        } else {
            return state.is(Blocks.LAVA) ? configuration.debugSettings.getLavaState() : state;
        }
    }

    public abstract boolean carve(
        final CarvingContext context,
        final C configuration,
        final ChunkAccess chunk,
        final Function<BlockPos, Holder<Biome>> biomeGetter,
        final RandomSource random,
        final Aquifer aquifer,
        final ChunkPos sourceChunkPos,
        CarvingMask mask
    );

    public abstract boolean isStartChunk(final C configuration, final RandomSource random);

    protected boolean canReplaceBlock(final C configuration, final BlockState state) {
        return state.is(configuration.replaceable);
    }

    protected static boolean canReach(
        final ChunkPos chunkPos, final double x, final double z, final int currentStep, final int totalSteps, final float thickness
    ) {
        double xMid = (int)chunkPos.getMiddleBlockX();
        double zMid = (int)chunkPos.getMiddleBlockZ();
        double xd = x - xMid;
        double zd = z - zMid;
        double remaining = totalSteps - currentStep;
        double rr = thickness + 2.0F + 16.0F;
        return xd * xd + zd * zd - remaining * remaining <= rr * rr;
    }

    private static boolean isDebugEnabled(final CarverConfiguration configuration) {
        return SharedConstants.DEBUG_CARVERS || configuration.debugSettings.isDebugMode();
    }

    public interface CarveSkipChecker {
        boolean shouldSkip(CarvingContext context, double xd, double yd, double zd, int y);
    }
}