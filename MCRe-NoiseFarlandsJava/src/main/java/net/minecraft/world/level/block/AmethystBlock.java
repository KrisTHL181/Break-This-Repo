package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AmethystBlock extends Block {
    public static final MapCodec<AmethystBlock> CODEC = simpleCodec(AmethystBlock::new);

    @Override
    public MapCodec<? extends AmethystBlock> codec() {
        return CODEC;
    }

    public AmethystBlock(final BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected void onProjectileHit(final Level level, final BlockState state, final BlockHitResult hitResult, final Projectile projectile) {
        if (!level.isClientSide()) {
            BlockPos hitPos = hitResult.getBlockPos();
            level.playSound(null, hitPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.5F + level.getRandom().nextFloat() * 1.2F);
        }
    }
}