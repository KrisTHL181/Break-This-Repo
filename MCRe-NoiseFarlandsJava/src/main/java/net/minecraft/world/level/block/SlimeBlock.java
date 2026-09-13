package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SlimeBlock extends HalfTransparentBlock {
    public static final MapCodec<SlimeBlock> CODEC = simpleCodec(SlimeBlock::new);

    @Override
    public MapCodec<SlimeBlock> codec() {
        return CODEC;
    }

    public SlimeBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(final Level level, final BlockState state, final BlockPos pos, final Entity entity, final double fallDistance) {
        if (!entity.isSuppressingBounce()) {
            entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
        }
    }

    @Override
    public void stepOn(final Level level, final BlockPos pos, final BlockState onState, final Entity entity) {
        double absDeltaY = Math.abs(entity.getDeltaMovement().y);
        if (absDeltaY < 0.1 && !entity.isSteppingCarefully()) {
            double scale = 0.4 + absDeltaY * 0.2;
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(scale, 1.0, scale));
        }

        super.stepOn(level, pos, onState, entity);
    }
}