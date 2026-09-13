package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public class CoralBlock extends Block {
    public static final MapCodec<Block> DEAD_CORAL_FIELD = BuiltInRegistries.BLOCK.byNameCodec().fieldOf("dead");
    public static final MapCodec<CoralBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(DEAD_CORAL_FIELD.forGetter(b -> b.deadBlock), propertiesCodec()).apply(i, CoralBlock::new)
    );
    private final Block deadBlock;

    public CoralBlock(final Block deadBlock, final BlockBehaviour.Properties properties) {
        super(properties);
        this.deadBlock = deadBlock;
    }

    @Override
    public MapCodec<CoralBlock> codec() {
        return CODEC;
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (!this.scanForWater(level, pos)) {
            level.setBlock(pos, this.deadBlock.defaultBlockState(), 2);
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
        if (!this.scanForWater(level, pos)) {
            ticks.scheduleTick(pos, this, 60 + random.nextInt(40));
        }

        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    protected boolean scanForWater(final BlockGetter level, final BlockPos blockPos) {
        for (Direction direction : Direction.values()) {
            FluidState fluidState = level.getFluidState(blockPos.relative(direction));
            if (fluidState.is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        if (!this.scanForWater(context.getLevel(), context.getClickedPos())) {
            context.getLevel().scheduleTick(context.getClickedPos(), this, 60 + context.getLevel().getRandom().nextInt(40));
        }

        return this.defaultBlockState();
    }
}