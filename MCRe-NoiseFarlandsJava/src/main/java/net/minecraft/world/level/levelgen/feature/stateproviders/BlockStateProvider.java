package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class BlockStateProvider {
    public static final Codec<BlockStateProvider> CODEC = BuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE
        .byNameCodec()
        .dispatch(BlockStateProvider::type, BlockStateProviderType::codec);

    public static SimpleStateProvider simple(final BlockState state) {
        return new SimpleStateProvider(state);
    }

    public static SimpleStateProvider simple(final Block block) {
        return new SimpleStateProvider(block.defaultBlockState());
    }

    protected abstract BlockStateProviderType<?> type();

    public abstract BlockState getState(final WorldGenLevel level, final RandomSource random, final BlockPos pos);

    public @Nullable BlockState getOptionalState(final WorldGenLevel level, final RandomSource random, final BlockPos pos) {
        return this.getState(level, random, pos);
    }
}