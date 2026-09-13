package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.references.BlockItemIds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MyceliumBlock extends SpreadingSnowyBlock {
    public static final MapCodec<MyceliumBlock> CODEC = simpleCodec(MyceliumBlock::new);

    @Override
    public MapCodec<MyceliumBlock> codec() {
        return CODEC;
    }

    public MyceliumBlock(final BlockBehaviour.Properties properties) {
        super(properties, BlockItemIds.DIRT.block());
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.MYCELIUM, pos.getX() + random.nextDouble(), pos.getY() + 1.1, pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }
}