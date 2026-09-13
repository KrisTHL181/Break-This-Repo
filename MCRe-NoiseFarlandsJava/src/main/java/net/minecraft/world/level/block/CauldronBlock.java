package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class CauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<CauldronBlock> CODEC = simpleCodec(CauldronBlock::new);
    private static final float RAIN_FILL_CHANCE = 0.05F;
    private static final float POWDER_SNOW_FILL_CHANCE = 0.1F;

    @Override
    public MapCodec<CauldronBlock> codec() {
        return CODEC;
    }

    public CauldronBlock(final BlockBehaviour.Properties properties) {
        super(properties, CauldronInteractions.EMPTY);
    }

    @Override
    public boolean isFull(final BlockState state) {
        return false;
    }

    protected static boolean shouldHandlePrecipitation(final Level level, final Biome.Precipitation precipitation) {
        if (precipitation == Biome.Precipitation.RAIN) {
            return level.getRandom().nextFloat() < 0.05F;
        } else {
            return precipitation == Biome.Precipitation.SNOW ? level.getRandom().nextFloat() < 0.1F : false;
        }
    }

    @Override
    public void handlePrecipitation(final BlockState state, final Level level, final BlockPos pos, final Biome.Precipitation precipitation) {
        if (shouldHandlePrecipitation(level, precipitation)) {
            if (precipitation == Biome.Precipitation.RAIN) {
                level.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState());
                level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
            } else if (precipitation == Biome.Precipitation.SNOW) {
                level.setBlockAndUpdate(pos, Blocks.POWDER_SNOW_CAULDRON.defaultBlockState());
                level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
            }
        }
    }

    @Override
    protected boolean canReceiveStalactiteDrip(final Fluid fluid) {
        return true;
    }

    @Override
    protected void receiveStalactiteDrip(final BlockState state, final Level level, final BlockPos pos, final Fluid fluid) {
        if (fluid == Fluids.WATER) {
            BlockState newState = Blocks.WATER_CAULDRON.defaultBlockState();
            level.setBlockAndUpdate(pos, newState);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));
            level.levelEvent(1047, pos, 0);
        } else if (fluid == Fluids.LAVA) {
            BlockState newState = Blocks.LAVA_CAULDRON.defaultBlockState();
            level.setBlockAndUpdate(pos, newState);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));
            level.levelEvent(1046, pos, 0);
        }
    }
}