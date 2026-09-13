package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CoralPlantBlock extends BaseCoralPlantTypeBlock {
    public static final MapCodec<CoralPlantBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(CoralBlock.DEAD_CORAL_FIELD.forGetter(b -> b.deadBlock), propertiesCodec()).apply(i, CoralPlantBlock::new)
    );
    private final Block deadBlock;
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 15.0);

    @Override
    public MapCodec<CoralPlantBlock> codec() {
        return CODEC;
    }

    protected CoralPlantBlock(final Block deadBlock, final BlockBehaviour.Properties properties) {
        super(properties);
        this.deadBlock = deadBlock;
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        this.tryScheduleDieTick(state, level, level, level.getRandom(), pos);
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (!scanForWater(state, level, pos)) {
            level.setBlock(pos, this.deadBlock.defaultBlockState().setValue(WATERLOGGED, false), 2);
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
        if (directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }

        this.tryScheduleDieTick(state, level, ticks, random, pos);
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }
}