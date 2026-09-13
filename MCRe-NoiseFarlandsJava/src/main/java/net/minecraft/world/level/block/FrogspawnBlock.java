package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FrogspawnBlock extends Block {
    public static final MapCodec<FrogspawnBlock> CODEC = simpleCodec(FrogspawnBlock::new);
    private static final int MIN_TADPOLES_SPAWN = 2;
    private static final int MAX_TADPOLES_SPAWN = 5;
    private static final int DEFAULT_MIN_HATCH_TICK_DELAY = 3600;
    private static final int DEFAULT_MAX_HATCH_TICK_DELAY = 12000;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 1.5);
    private static int minHatchTickDelay = 3600;
    private static int maxHatchTickDelay = 12000;

    @Override
    public MapCodec<FrogspawnBlock> codec() {
        return CODEC;
    }

    public FrogspawnBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        return mayPlaceOn(level, pos.below());
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        level.scheduleTick(pos, this, getFrogspawnHatchDelay(level.getRandom()));
    }

    private static int getFrogspawnHatchDelay(final RandomSource random) {
        return random.nextInt(minHatchTickDelay, maxHatchTickDelay);
    }

    @Override
    protected BlockState updateShape(
        final BlockState state,
        final LevelReader level,
        final ScheduledTickAccess ticks,
        final BlockPos pos,
        final Direction directionToNeighbour,
        final BlockPos neighbourPos,
        final BlockState neighbourState,
        final RandomSource random
    ) {
        return !this.canSurvive(state, level, pos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (!this.canSurvive(state, level, pos)) {
            this.destroyBlock(level, pos);
        } else {
            this.hatchFrogspawn(level, pos, random);
        }
    }

    @Override
    protected void entityInside(
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Entity entity,
        final InsideBlockEffectApplier effectApplier,
        final boolean isPrecise
    ) {
        if (entity.is(EntityTypes.FALLING_BLOCK)) {
            this.destroyBlock(level, pos);
        }
    }

    private static boolean mayPlaceOn(final BlockGetter level, final BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        FluidState fluidAbove = level.getFluidState(pos.above());
        return (fluidState.is(FluidTags.SUPPORTS_FROGSPAWN) || level.getBlockState(pos).is(BlockTags.SUPPORTS_FROGSPAWN)) && fluidAbove.is(Fluids.EMPTY);
    }

    private void hatchFrogspawn(final ServerLevel level, final BlockPos pos, final RandomSource random) {
        this.destroyBlock(level, pos);
        level.playSound(null, pos, SoundEvents.FROGSPAWN_HATCH, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.spawnTadpoles(level, pos, random);
    }

    private void destroyBlock(final Level level, final BlockPos pos) {
        level.destroyBlock(pos, false);
    }

    private void spawnTadpoles(final ServerLevel level, final BlockPos pos, final RandomSource random) {
        int tadpoleAmount = random.nextInt(2, 6);

        for (int i = 1; i <= tadpoleAmount; i++) {
            Tadpole tadpole = EntityTypes.TADPOLE.create(level, EntitySpawnReason.BREEDING);
            if (tadpole != null) {
                double xPos = pos.getX() + this.getRandomTadpolePositionOffset(random);
                double zPos = pos.getZ() + this.getRandomTadpolePositionOffset(random);
                int yRot = random.nextInt(1, 361);
                tadpole.snapTo(xPos, pos.getY() - 0.5, zPos, yRot, 0.0F);
                tadpole.setPersistenceRequired();
                level.addFreshEntity(tadpole);
            }
        }
    }

    private double getRandomTadpolePositionOffset(final RandomSource random) {
        double tadpoleHitboxCenter = 0.2F;
        return Mth.clamp(random.nextDouble(), 0.2F, 0.7999999970197678);
    }

    @VisibleForTesting
    public static void setHatchDelay(final int minDelay, final int maxDelay) {
        minHatchTickDelay = minDelay;
        maxHatchTickDelay = maxDelay;
    }

    @VisibleForTesting
    public static void setDefaultHatchDelay() {
        minHatchTickDelay = 3600;
        maxHatchTickDelay = 12000;
    }
}