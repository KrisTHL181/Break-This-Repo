package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PoweredBlock extends Block {
    public static final MapCodec<PoweredBlock> CODEC = simpleCodec(PoweredBlock::new);

    @Override
    public MapCodec<PoweredBlock> codec() {
        return CODEC;
    }

    public PoweredBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return 15;
    }
}