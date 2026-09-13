package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BigDripleafStemBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, BonemealableBlock {
    public static final MapCodec<BigDripleafStemBlock> CODEC = simpleCodec(BigDripleafStemBlock::new);
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.column(6.0, 0.0, 16.0).move(0.0, 0.0, 0.25).optimize());

    @Override
    public MapCodec<BigDripleafStemBlock> codec() {
        return CODEC;
    }

    protected BigDripleafStemBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, FACING);
    }

    @Override
    protected FluidState getFluidState(final BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        BlockState aboveState = level.getBlockState(pos.above());
        return (belowState.is(this) || belowState.is(BlockTags.SUPPORTS_BIG_DRIPLEAF)) && (aboveState.is(this) || aboveState.is(Blocks.BIG_DRIPLEAF));
    }

    protected static boolean place(final LevelAccessor level, final BlockPos pos, final FluidState fluidState, final Direction facing) {
        BlockState newState = Blocks.BIG_DRIPLEAF_STEM
            .defaultBlockState()
            .setValue(WATERLOGGED, fluidState.isSourceOfType(Fluids.WATER))
            .setValue(FACING, facing);
        return level.setBlock(pos, newState, 3);
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
        if ((directionToNeighbour == Direction.DOWN || directionToNeighbour == Direction.UP) && !state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }

        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
        Optional<BlockPos> headPos = BlockUtil.getTopConnectedBlock(level, pos, state.getBlock(), Direction.UP, Blocks.BIG_DRIPLEAF);
        return headPos.filter(blockPos -> BigDripleafBlock.canPlaceAt(level, blockPos.above())).isPresent();
    }

    @Override
    public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
        Optional<BlockPos> forwardPos = BlockUtil.getTopConnectedBlock(level, pos, state.getBlock(), Direction.UP, Blocks.BIG_DRIPLEAF);
        if (!forwardPos.isEmpty()) {
            BlockPos headPos = forwardPos.get();
            BlockPos placeHeadPos = headPos.above();
            Direction facing = state.getValue(FACING);
            place(level, headPos, level.getFluidState(headPos), facing);
            BigDripleafBlock.place(level, placeHeadPos, level.getFluidState(placeHeadPos), facing);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state, final boolean includeData) {
        return new ItemStack(Blocks.BIG_DRIPLEAF);
    }
}