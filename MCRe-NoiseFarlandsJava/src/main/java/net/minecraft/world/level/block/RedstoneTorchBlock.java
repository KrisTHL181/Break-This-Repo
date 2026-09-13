package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class RedstoneTorchBlock extends BaseTorchBlock {
    public static final MapCodec<RedstoneTorchBlock> CODEC = simpleCodec(RedstoneTorchBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final Map<BlockGetter, List<RedstoneTorchBlock.Toggle>> RECENT_TOGGLES = new WeakHashMap<>();
    public static final int RECENT_TOGGLE_TIMER = 60;
    public static final int MAX_RECENT_TOGGLES = 8;
    public static final int RESTART_DELAY = 160;
    private static final int TOGGLE_DELAY = 2;

    @Override
    public MapCodec<? extends RedstoneTorchBlock> codec() {
        return CODEC;
    }

    protected RedstoneTorchBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        this.notifyNeighbors(level, pos, state);
    }

    private void notifyNeighbors(final Level level, final BlockPos pos, final BlockState state) {
        Orientation orientation = this.randomOrientation(level, state);

        for (Direction direction : Direction.values()) {
            level.updateNeighborsAt(pos.relative(direction), this, ExperimentalRedstoneUtils.withFront(orientation, direction));
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        if (!movedByPiston) {
            this.notifyNeighbors(level, pos, state);
        }
    }

    protected boolean hasNeighborSignal(final Level level, final BlockPos pos, final BlockState state) {
        return level.hasSignal(pos.below(), Direction.DOWN);
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        boolean neighborSignal = this.hasNeighborSignal(level, pos, state);
        List<RedstoneTorchBlock.Toggle> toggles = RECENT_TOGGLES.get(level);

        while (toggles != null && !toggles.isEmpty() && level.getGameTime() - toggles.get(0).when > 60L) {
            toggles.remove(0);
        }

        if (state.getValue(LIT)) {
            if (neighborSignal) {
                level.setBlock(pos, state.setValue(LIT, false), 3);
                if (isToggledTooFrequently(level, pos, true)) {
                    level.levelEvent(1502, pos, 0);
                    level.scheduleTick(pos, level.getBlockState(pos).getBlock(), 160);
                }
            }
        } else if (!neighborSignal && !isToggledTooFrequently(level, pos, false)) {
            level.setBlock(pos, state.setValue(LIT, true), 3);
        }
    }

    @Override
    protected void neighborChanged(
        final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
    ) {
        if (state.getValue(LIT) == this.hasNeighborSignal(level, pos, state) && !level.getBlockTicks().willTickThisTick(pos, this)) {
            level.scheduleTick(pos, this, 2);
        }
    }

    @Override
    protected int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
        return direction == Direction.DOWN ? state.getSignal(level, pos, direction) : 0;
    }

    @Override
    protected boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override
    protected int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
        return Direction.UP != direction ? this.ownSignal(state, level, pos) : 0;
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (state.getValue(LIT)) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double y = pos.getY() + 0.7 + (random.nextDouble() - 0.5) * 0.2;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            level.addParticle(DustParticleOptions.REDSTONE, x, y, z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    private static boolean isToggledTooFrequently(final Level level, final BlockPos pos, final boolean add) {
        List<RedstoneTorchBlock.Toggle> toggles = RECENT_TOGGLES.computeIfAbsent(level, k -> Lists.newArrayList());
        if (add) {
            toggles.add(new RedstoneTorchBlock.Toggle(pos.immutable(), level.getGameTime()));
        }

        int count = 0;

        for (RedstoneTorchBlock.Toggle toggle : toggles) {
            if (toggle.pos.equals(pos)) {
                if (++count >= 8) {
                    return true;
                }
            }
        }

        return false;
    }

    protected @Nullable Orientation randomOrientation(final Level level, final BlockState state) {
        return ExperimentalRedstoneUtils.initialOrientation(level, null, Direction.UP);
    }

    public static class Toggle {
        private final BlockPos pos;
        private final long when;

        public Toggle(final BlockPos pos, final long when) {
            this.pos = pos;
            this.when = when;
        }
    }
}