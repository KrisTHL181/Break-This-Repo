package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseFireBlock extends Block {
    private static final int SECONDS_ON_FIRE = 8;
    private static final int MIN_FIRE_TICKS_TO_ADD = 1;
    private static final int MAX_FIRE_TICKS_TO_ADD = 3;
    private final float fireDamage;
    protected static final VoxelShape SHAPE = Block.column(16.0, 0.0, 1.0);

    public BaseFireBlock(final BlockBehaviour.Properties properties, final float fireDamage) {
        super(properties);
        this.fireDamage = fireDamage;
    }

    @Override
    protected abstract MapCodec<? extends BaseFireBlock> codec();

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return getState(context.getLevel(), context.getClickedPos());
    }

    public static BlockState getState(final BlockGetter level, final BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return SoulFireBlock.canSurviveOnBlock(belowState) ? Blocks.SOUL_FIRE.defaultBlockState() : ((FireBlock)Blocks.FIRE).getStateForPlacement(level, pos);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (random.nextInt(24) == 0) {
            level.playLocalSound(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                SoundEvents.FIRE_AMBIENT,
                SoundSource.BLOCKS,
                1.0F + random.nextFloat(),
                random.nextFloat() * 0.7F + 0.3F,
                false
            );
        }

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (!this.canBurn(belowState) && !belowState.isFaceSturdy(level, below, Direction.UP)) {
            if (this.canBurn(level.getBlockState(pos.west()))) {
                for (int i = 0; i < 2; i++) {
                    double xx = pos.getX() + random.nextDouble() * 0.1F;
                    double yy = pos.getY() + random.nextDouble();
                    double zz = pos.getZ() + random.nextDouble();
                    level.addParticle(ParticleTypes.LARGE_SMOKE, xx, yy, zz, 0.0, 0.0, 0.0);
                }
            }

            if (this.canBurn(level.getBlockState(pos.east()))) {
                for (int i = 0; i < 2; i++) {
                    double xx = pos.getX() + 1 - random.nextDouble() * 0.1F;
                    double yy = pos.getY() + random.nextDouble();
                    double zz = pos.getZ() + random.nextDouble();
                    level.addParticle(ParticleTypes.LARGE_SMOKE, xx, yy, zz, 0.0, 0.0, 0.0);
                }
            }

            if (this.canBurn(level.getBlockState(pos.north()))) {
                for (int i = 0; i < 2; i++) {
                    double xx = pos.getX() + random.nextDouble();
                    double yy = pos.getY() + random.nextDouble();
                    double zz = pos.getZ() + random.nextDouble() * 0.1F;
                    level.addParticle(ParticleTypes.LARGE_SMOKE, xx, yy, zz, 0.0, 0.0, 0.0);
                }
            }

            if (this.canBurn(level.getBlockState(pos.south()))) {
                for (int i = 0; i < 2; i++) {
                    double xx = pos.getX() + random.nextDouble();
                    double yy = pos.getY() + random.nextDouble();
                    double zz = pos.getZ() + 1 - random.nextDouble() * 0.1F;
                    level.addParticle(ParticleTypes.LARGE_SMOKE, xx, yy, zz, 0.0, 0.0, 0.0);
                }
            }

            if (this.canBurn(level.getBlockState(pos.above()))) {
                for (int i = 0; i < 2; i++) {
                    double xx = pos.getX() + random.nextDouble();
                    double yy = pos.getY() + 1 - random.nextDouble() * 0.1F;
                    double zz = pos.getZ() + random.nextDouble();
                    level.addParticle(ParticleTypes.LARGE_SMOKE, xx, yy, zz, 0.0, 0.0, 0.0);
                }
            }
        } else {
            for (int i = 0; i < 3; i++) {
                double xx = pos.getX() + random.nextDouble();
                double yy = pos.getY() + random.nextDouble() * 0.5 + 0.5;
                double zz = pos.getZ() + random.nextDouble();
                level.addParticle(ParticleTypes.LARGE_SMOKE, xx, yy, zz, 0.0, 0.0, 0.0);
            }
        }
    }

    protected abstract boolean canBurn(final BlockState state);

    @Override
    protected void entityInside(
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Entity entity,
        final InsideBlockEffectApplier effectApplier,
        final boolean isPrecise
    ) {
        effectApplier.apply(InsideBlockEffectType.CLEAR_FREEZE);
        effectApplier.apply(InsideBlockEffectType.FIRE_IGNITE);
        effectApplier.runAfter(InsideBlockEffectType.FIRE_IGNITE, e -> e.hurt(e.level().damageSources().inFire(), this.fireDamage));
    }

    public static void fireIgnite(final Entity entity) {
        if (!entity.fireImmune()) {
            if (entity.getRemainingFireTicks() < 0) {
                entity.setRemainingFireTicks(entity.getRemainingFireTicks() + 1);
            } else if (entity instanceof ServerPlayer) {
                int addedFireTicks = entity.level().getRandom().nextInt(1, 3);
                entity.setRemainingFireTicks(entity.getRemainingFireTicks() + addedFireTicks);
            }

            if (entity.getRemainingFireTicks() >= 0) {
                entity.igniteForSeconds(8.0F);
            }
        }
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        if (!oldState.is(state.getBlock())) {
            if (inPortalDimension(level)) {
                Optional<PortalShape> optionalShape = PortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X);
                if (optionalShape.isPresent()) {
                    optionalShape.get().createPortalBlocks(level);
                    return;
                }
            }

            if (!state.canSurvive(level, pos)) {
                level.removeBlock(pos, false);
            }
        }
    }

    private static boolean inPortalDimension(final Level level) {
        return level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER;
    }

    @Override
    protected void spawnDestroyParticles(final Level level, final Player player, final BlockPos pos, final BlockState state) {
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state, final Player player) {
        if (!level.isClientSide()) {
            level.levelEvent(null, 1009, pos, 0);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    public static boolean canBePlacedAt(final Level level, final BlockPos pos, final Direction forwardDirection) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() ? false : getState(level, pos).canSurvive(level, pos) || isPortal(level, pos, forwardDirection);
    }

    private static boolean isPortal(final Level level, final BlockPos pos, final Direction forwardDirection) {
        if (!inPortalDimension(level)) {
            return false;
        }

        BlockPos.MutableBlockPos testPos = pos.mutable();
        boolean hasObsidian = false;

        for (Direction face : Direction.values()) {
            if (level.getBlockState(testPos.set(pos).move(face)).is(Blocks.OBSIDIAN)) {
                hasObsidian = true;
                break;
            }
        }

        if (!hasObsidian) {
            return false;
        }

        Direction.Axis preferredAxis = forwardDirection.getAxis().isHorizontal()
            ? forwardDirection.getCounterClockWise().getAxis()
            : Direction.Plane.HORIZONTAL.getRandomAxis(level.getRandom());
        return PortalShape.findEmptyPortalShape(level, pos, preferredAxis).isPresent();
    }
}