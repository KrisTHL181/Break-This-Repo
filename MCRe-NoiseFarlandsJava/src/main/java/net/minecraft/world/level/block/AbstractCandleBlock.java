package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractCandleBlock extends Block {
    public static final int LIGHT_PER_CANDLE = 3;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    @Override
    protected abstract MapCodec<? extends AbstractCandleBlock> codec();

    protected AbstractCandleBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    protected abstract Iterable<Vec3> getParticleOffsets(final BlockState state);

    public static boolean isLit(final BlockState state) {
        return state.hasProperty(LIT) && (state.is(BlockTags.CANDLES) || state.is(BlockTags.CANDLE_CAKES)) && state.getValue(LIT);
    }

    @Override
    protected void onProjectileHit(final Level level, final BlockState state, final BlockHitResult blockHit, final Projectile projectile) {
        if (!level.isClientSide() && projectile.isOnFire() && this.canBeLit(state)) {
            setLit(level, state, blockHit.getBlockPos(), true);
        }
    }

    protected boolean canBeLit(final BlockState state) {
        return !state.getValue(LIT);
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (state.getValue(LIT)) {
            this.getParticleOffsets(state).forEach(particlePos -> addParticlesAndSound(level, particlePos.add(pos.getX(), pos.getY(), pos.getZ()), random));
        }
    }

    private static void addParticlesAndSound(final Level level, final Vec3 pos, final RandomSource random) {
        float chance = random.nextFloat();
        if (chance < 0.3F) {
            level.addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
            if (chance < 0.17F) {
                level.playLocalSound(
                    pos.x + 0.5,
                    pos.y + 0.5,
                    pos.z + 0.5,
                    SoundEvents.CANDLE_AMBIENT,
                    SoundSource.BLOCKS,
                    1.0F + random.nextFloat(),
                    random.nextFloat() * 0.7F + 0.3F,
                    false
                );
            }
        }

        level.addParticle(ParticleTypes.SMALL_FLAME, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
    }

    public static void extinguish(final @Nullable Player player, final BlockState state, final LevelAccessor level, final BlockPos pos) {
        setLit(level, state, pos, false);
        if (state.getBlock() instanceof AbstractCandleBlock) {
            ((AbstractCandleBlock)state.getBlock())
                .getParticleOffsets(state)
                .forEach(
                    particlePos -> level.addParticle(
                        ParticleTypes.SMOKE, pos.getX() + particlePos.x(), pos.getY() + particlePos.y(), pos.getZ() + particlePos.z(), 0.0, 0.1F, 0.0
                    )
                );
        }

        level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }

    private static void setLit(final LevelAccessor level, final BlockState state, final BlockPos pos, final boolean lit) {
        level.setBlock(pos, state.setValue(LIT, lit), 11);
    }

    @Override
    protected void onExplosionHit(
        final BlockState state, final ServerLevel level, final BlockPos pos, final Explosion explosion, final BiConsumer<ItemStack, BlockPos> onHit
    ) {
        if (explosion.canTriggerBlocks() && state.getValue(LIT)) {
            extinguish(null, state, level, pos);
        }

        super.onExplosionHit(state, level, pos, explosion, onHit);
    }
}