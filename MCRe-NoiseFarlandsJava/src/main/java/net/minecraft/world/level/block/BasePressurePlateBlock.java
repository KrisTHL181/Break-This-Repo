package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BasePressurePlateBlock extends Block {
    private static final VoxelShape SHAPE_PRESSED = Block.column(14.0, 0.0, 0.5);
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 1.0);
    protected static final AABB TOUCH_AABB = Block.column(14.0, 0.0, 4.0).toAabbs().getFirst();
    protected final BlockSetType type;

    protected BasePressurePlateBlock(final BlockBehaviour.Properties properties, final BlockSetType type) {
        super(properties.sound(type.soundType()));
        this.type = type;
    }

    @Override
    protected abstract MapCodec<? extends BasePressurePlateBlock> codec();

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return this.getSignalForState(state) > 0 ? SHAPE_PRESSED : SHAPE;
    }

    protected int getPressedTime() {
        return 20;
    }

    @Override
    public boolean isPossibleToRespawnInThis(final BlockState state) {
        return true;
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
        return directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        BlockPos below = pos.below();
        return canSupportRigidBlock(level, below) || canSupportCenter(level, below, Direction.UP);
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        int signal = this.getSignalForState(state);
        if (signal > 0) {
            this.checkPressed(null, level, pos, state, signal);
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
        if (!level.isClientSide()) {
            int signal = this.getSignalForState(state);
            if (signal == 0) {
                this.checkPressed(entity, level, pos, state, signal);
            }
        }
    }

    private void checkPressed(final @Nullable Entity sourceEntity, final Level level, final BlockPos pos, final BlockState state, final int oldSignal) {
        int signal = this.getSignalStrength(level, pos);
        boolean wasPressed = oldSignal > 0;
        boolean isPressed = signal > 0;
        if (oldSignal != signal) {
            BlockState newState = this.setSignalForState(state, signal);
            level.setBlock(pos, newState, 2);
            this.updateNeighbours(level, pos);
            level.setBlocksDirty(pos, state, newState);
        }

        if (!isPressed && wasPressed) {
            level.playSound(null, pos, this.type.pressurePlateClickOff(), SoundSource.BLOCKS);
            level.gameEvent(sourceEntity, GameEvent.BLOCK_DEACTIVATE, pos);
        } else if (isPressed && !wasPressed) {
            level.playSound(null, pos, this.type.pressurePlateClickOn(), SoundSource.BLOCKS);
            level.gameEvent(sourceEntity, GameEvent.BLOCK_ACTIVATE, pos);
        }

        if (isPressed) {
            level.scheduleTick(new BlockPos(pos), this, this.getPressedTime());
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        if (!movedByPiston && this.getSignalForState(state) > 0) {
            this.updateNeighbours(level, pos);
        }
    }

    protected void updateNeighbours(final Level level, final BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.below(), this);
    }

    @Override
    protected int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
        return direction == Direction.UP ? this.getSignalForState(state) : 0;
    }

    @Override
    protected boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return this.getSignalForState(state);
    }

    protected static int getEntityCount(final Level level, final AABB entityDetectionBox, final Class<? extends Entity> entityClass) {
        return level.getEntitiesOfClass(entityClass, entityDetectionBox, EntitySelector.NO_SPECTATORS.and(e -> !e.isIgnoringBlockTriggers())).size();
    }

    protected abstract int getSignalStrength(Level level, BlockPos pos);

    protected abstract int getSignalForState(BlockState state);

    protected abstract BlockState setSignalForState(BlockState state, int signal);
}