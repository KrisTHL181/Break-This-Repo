package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class SculkSensorBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<SculkSensorBlock> CODEC = simpleCodec(SculkSensorBlock::new);
    public static final int ACTIVE_TICKS = 30;
    public static final int COOLDOWN_TICKS = 10;
    public static final EnumProperty<SculkSensorPhase> PHASE = BlockStateProperties.SCULK_SENSOR_PHASE;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 8.0);
    private static final float[] RESONANCE_PITCH_BEND = Util.make(new float[16], arr -> {
        int[] toneMap = new int[]{0, 0, 2, 4, 6, 7, 9, 10, 12, 14, 15, 18, 19, 21, 22, 24};

        for (int i = 0; i < 16; i++) {
            arr[i] = NoteBlock.getPitchFromNote(toneMap[i]);
        }
    });

    @Override
    public MapCodec<? extends SculkSensorBlock> codec() {
        return CODEC;
    }

    public SculkSensorBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PHASE, SculkSensorPhase.INACTIVE).setValue(POWER, 0).setValue(WATERLOGGED, false));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        FluidState replacedFluidState = context.getLevel().getFluidState(pos);
        return this.defaultBlockState().setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
    }

    @Override
    protected FluidState getFluidState(final BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (getPhase(state) != SculkSensorPhase.ACTIVE) {
            if (getPhase(state) == SculkSensorPhase.COOLDOWN) {
                level.setBlock(pos, state.setValue(PHASE, SculkSensorPhase.INACTIVE), 3);
                if (!state.getValue(WATERLOGGED)) {
                    level.playSound(null, pos, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.2F + 0.8F);
                }
            }
        } else {
            deactivate(level, pos, state);
        }
    }

    @Override
    public void stepOn(final Level level, final BlockPos pos, final BlockState onState, final Entity entity) {
        if (!level.isClientSide()
            && canActivate(onState)
            && !entity.is(EntityTypes.WARDEN)
            && level.getBlockEntity(pos) instanceof SculkSensorBlockEntity sculkSensor
            && level instanceof ServerLevel serverLevel
            && sculkSensor.getVibrationUser().canReceiveVibration(serverLevel, pos, GameEvent.STEP, GameEvent.Context.of(onState))) {
            sculkSensor.getListener().forceScheduleVibration(serverLevel, GameEvent.STEP, GameEvent.Context.of(entity), entity.position());
        }

        super.stepOn(level, pos, onState, entity);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            if (state.getValue(POWER) > 0 && !level.getBlockTicks().hasScheduledTick(pos, this)) {
                level.setBlock(pos, state.setValue(POWER, 0), 18);
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        if (getPhase(state) == SculkSensorPhase.ACTIVE) {
            updateNeighbours(level, pos, state);
        }
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
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    private static void updateNeighbours(final Level level, final BlockPos pos, final BlockState state) {
        Block block = state.getBlock();
        level.updateNeighborsAt(pos, block);
        level.updateNeighborsAt(pos.below(), block);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        return new SculkSensorBlockEntity(worldPosition, blockState);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
        return !level.isClientSide()
            ? createTickerHelper(
                type,
                BlockEntityTypes.SCULK_SENSOR,
                (innerLevel, pos, state, entity) -> VibrationSystem.Ticker.tick(innerLevel, entity.getVibrationData(), entity.getVibrationUser())
            )
            : null;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.getValue(POWER);
    }

    @Override
    public int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
        return direction == Direction.UP ? state.getSignal(level, pos, direction) : 0;
    }

    public static SculkSensorPhase getPhase(final BlockState state) {
        return state.getValue(PHASE);
    }

    public static boolean canActivate(final BlockState state) {
        return getPhase(state) == SculkSensorPhase.INACTIVE;
    }

    public static void deactivate(final Level level, final BlockPos pos, final BlockState state) {
        level.setBlock(pos, state.setValue(PHASE, SculkSensorPhase.COOLDOWN).setValue(POWER, 0), 3);
        level.scheduleTick(pos, state.getBlock(), 10);
        updateNeighbours(level, pos, state);
    }

    @VisibleForTesting
    public int getActiveTicks() {
        return 30;
    }

    public void activate(
        final @Nullable Entity sourceEntity,
        final Level level,
        final BlockPos pos,
        final BlockState state,
        final int calculatedPower,
        final int vibrationFrequency
    ) {
        level.setBlock(pos, state.setValue(PHASE, SculkSensorPhase.ACTIVE).setValue(POWER, calculatedPower), 3);
        level.scheduleTick(pos, state.getBlock(), this.getActiveTicks());
        updateNeighbours(level, pos, state);
        tryResonateVibration(sourceEntity, level, pos, vibrationFrequency);
        level.gameEvent(sourceEntity, GameEvent.SCULK_SENSOR_TENDRILS_CLICKING, pos);
        if (!state.getValue(WATERLOGGED)) {
            level.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                SoundEvents.SCULK_CLICKING,
                SoundSource.BLOCKS,
                1.0F,
                level.getRandom().nextFloat() * 0.2F + 0.8F
            );
        }
    }

    public static void tryResonateVibration(final @Nullable Entity sourceEntity, final Level level, final BlockPos pos, final int vibrationFrequency) {
        for (Direction direction : Direction.values()) {
            BlockPos relativePos = pos.relative(direction);
            BlockState blockState = level.getBlockState(relativePos);
            if (blockState.is(BlockTags.VIBRATION_RESONATORS)) {
                level.gameEvent(VibrationSystem.getResonanceEventByFrequency(vibrationFrequency), relativePos, GameEvent.Context.of(sourceEntity, blockState));
                float pitch = RESONANCE_PITCH_BEND[vibrationFrequency];
                level.playSound(null, relativePos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, pitch);
            }
        }
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (getPhase(state) == SculkSensorPhase.ACTIVE) {
            Direction dir = Direction.getRandom(random);
            if (dir != Direction.UP && dir != Direction.DOWN) {
                double x = pos.getX() + 0.5 + (dir.getStepX() == 0 ? 0.5 - random.nextDouble() : dir.getStepX() * 0.6);
                double y = pos.getY() + 0.25;
                double z = pos.getZ() + 0.5 + (dir.getStepZ() == 0 ? 0.5 - random.nextDouble() : dir.getStepZ() * 0.6);
                double ya = random.nextFloat() * 0.04;
                level.addParticle(DustColorTransitionOptions.SCULK_TO_REDSTONE, x, y, z, 0.0, ya, 0.0);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE, POWER, WATERLOGGED);
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
        if (level.getBlockEntity(pos) instanceof SculkSensorBlockEntity sculk) {
            return getPhase(state) == SculkSensorPhase.ACTIVE ? sculk.getLastVibrationFrequency() : 0;
        } else {
            return 0;
        }
    }

    @Override
    protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return false;
    }

    @Override
    protected boolean useShapeForLightOcclusion(final BlockState state) {
        return true;
    }

    @Override
    protected void spawnAfterBreak(final BlockState state, final ServerLevel level, final BlockPos pos, final ItemStack tool, final boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (dropExperience) {
            this.tryDropExperience(level, pos, tool, ConstantInt.of(5));
        }
    }
}