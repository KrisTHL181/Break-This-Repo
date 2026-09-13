package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;

public abstract class MultifaceSpreadeableBlock extends MultifaceBlock {
    public MultifaceSpreadeableBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public abstract MapCodec<? extends MultifaceSpreadeableBlock> codec();

    public abstract MultifaceSpreader getSpreader();
}