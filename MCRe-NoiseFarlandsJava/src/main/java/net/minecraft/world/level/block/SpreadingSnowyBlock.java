package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;

public abstract class SpreadingSnowyBlock extends SnowyBlock {
    private final ResourceKey<Block> baseBlock;

    protected SpreadingSnowyBlock(final BlockBehaviour.Properties properties, final ResourceKey<Block> baseBlock) {
        super(properties);
        this.baseBlock = baseBlock;
    }

    @Override
    protected abstract MapCodec<? extends SpreadingSnowyBlock> codec();

    private static boolean canStayAlive(final BlockState state, final LevelReader level, final BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.is(Blocks.SNOW) && aboveState.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }

        if (aboveState.getFluidState().isFull()) {
            return false;
        }

        int lightDampeningTopFace = LightEngine.getLightDampeningInto(state, aboveState, Direction.UP, aboveState.getLightDampening());
        return lightDampeningTopFace < 15;
    }

    private static boolean canPropagate(final BlockState state, final LevelReader level, final BlockPos pos) {
        BlockPos above = pos.above();
        return canStayAlive(state, level, pos) && !level.getFluidState(above).is(FluidTags.WATER);
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        Registry<Block> blocks = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        Optional<Block> baseBlock = blocks.getOptional(this.baseBlock);
        if (!baseBlock.isEmpty()) {
            if (!canStayAlive(state, level, pos)) {
                level.setBlockAndUpdate(pos, baseBlock.get().defaultBlockState());
            } else {
                if (level.getMaxLocalRawBrightness(pos.above()) >= 9) {
                    BlockState defaultBlockState = this.defaultBlockState();

                    for (int i = 0; i < 4; i++) {
                        BlockPos testPos = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
                        if (level.getBlockState(testPos).is(baseBlock.get()) && canPropagate(defaultBlockState, level, testPos)) {
                            level.setBlockAndUpdate(testPos, defaultBlockState.setValue(SNOWY, isSnowySetting(level.getBlockState(testPos.above()))));
                        }
                    }
                }
            }
        }
    }
}