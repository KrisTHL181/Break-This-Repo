package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.NetherFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.lighting.LightEngine;

public class NyliumBlock extends Block implements BonemealableBlock {
    public static final MapCodec<NyliumBlock> CODEC = simpleCodec(NyliumBlock::new);

    @Override
    public MapCodec<NyliumBlock> codec() {
        return CODEC;
    }

    protected NyliumBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    private static boolean canBeNylium(final BlockState state, final LevelReader level, final BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        int lightDampeningTopFace = LightEngine.getLightDampeningInto(state, aboveState, Direction.UP, aboveState.getLightDampening());
        return lightDampeningTopFace < 15;
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (!canBeNylium(state, level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.NETHERRACK.defaultBlockState());
        }
    }

    @Override
    public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
        return level.getBlockState(pos.above()).isAir() && level.isInsideBuildHeight(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
        BlockState blockState = level.getBlockState(pos);
        BlockPos abovePos = pos.above();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        Registry<ConfiguredFeature<?, ?>> configuredFeatures = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        if (blockState.is(Blocks.CRIMSON_NYLIUM)) {
            this.place(configuredFeatures, NetherFeatures.CRIMSON_FOREST_VEGETATION_BONEMEAL, level, generator, random, abovePos);
        } else if (blockState.is(Blocks.WARPED_NYLIUM)) {
            this.place(configuredFeatures, NetherFeatures.WARPED_FOREST_VEGETATION_BONEMEAL, level, generator, random, abovePos);
            this.place(configuredFeatures, NetherFeatures.NETHER_SPROUTS_BONEMEAL, level, generator, random, abovePos);
            if (random.nextInt(8) == 0) {
                this.place(configuredFeatures, NetherFeatures.TWISTING_VINES_BONEMEAL, level, generator, random, abovePos);
            }
        }
    }

    private void place(
        final Registry<ConfiguredFeature<?, ?>> configuredFeatures,
        final ResourceKey<ConfiguredFeature<?, ?>> id,
        final ServerLevel level,
        final ChunkGenerator generator,
        final RandomSource random,
        final BlockPos pos
    ) {
        if (level.isInsideBuildHeight(pos)) {
            configuredFeatures.get(id).ifPresent(h -> h.value().place(level, generator, random, pos));
        }
    }

    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.NEIGHBOR_SPREADER;
    }
}