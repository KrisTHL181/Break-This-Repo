package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PitcherCropBlock extends DoublePlantBlock implements BonemealableBlock {
    public static final MapCodec<PitcherCropBlock> CODEC = simpleCodec(PitcherCropBlock::new);
    public static final int MAX_AGE = 4;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
    public static final EnumProperty<DoubleBlockHalf> HALF = DoublePlantBlock.HALF;
    private static final int DOUBLE_PLANT_AGE_INTERSECTION = 3;
    private static final int BONEMEAL_INCREASE = 1;
    private static final VoxelShape SHAPE_BULB = Block.column(6.0, -1.0, 3.0);
    private static final VoxelShape SHAPE_CROP = Block.column(10.0, -1.0, 5.0);
    private final Function<BlockState, VoxelShape> shapes = this.makeShapes();

    @Override
    public MapCodec<PitcherCropBlock> codec() {
        return CODEC;
    }

    public PitcherCropBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        int[] plantHeights = new int[]{0, 9, 11, 22, 26};
        return this.getShapeForEachState(state -> {
            int height = (state.getValue(AGE) == 0 ? 4 : 6) + plantHeights[state.getValue(AGE)];
            int width = state.getValue(AGE) == 0 ? 6 : 10;

            return switch ((DoubleBlockHalf)state.getValue(HALF)) {
                case LOWER -> Block.column(width, -1.0, Math.min(16, -1 + height));
                case UPPER -> Block.column(width, 0.0, Math.max(0, -1 + height - 16));
            };
        });
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return state.getValue(AGE) == 0 ? SHAPE_BULB : SHAPE_CROP;
        } else {
            return Shapes.empty();
        }
    }

    @Override
    public BlockState updateShape(
        final BlockState state,
        final LevelReader level,
        final ScheduledTickAccess ticks,
        final BlockPos pos,
        final Direction directionToNeighbour,
        final BlockPos neighbourPos,
        final BlockState neighbourState,
        final RandomSource random
    ) {
        if (isDouble(state.getValue(AGE))) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        } else {
            return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
        }
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        return isLower(state) && !sufficientLight(level, pos) ? false : super.canSurvive(state, level, pos);
    }

    @Override
    protected boolean mayPlaceOn(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.is(BlockTags.SUPPORTS_CROPS);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void entityInside(
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Entity entity,
        final InsideBlockEffectApplier effectApplier,
        final boolean isPrecise
    ) {
        if (level instanceof ServerLevel serverLevel && entity instanceof Ravager && serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
            serverLevel.destroyBlock(pos, true, entity);
        }
    }

    @Override
    public boolean canBeReplaced(final BlockState state, final BlockPlaceContext context) {
        return false;
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final @Nullable LivingEntity by, final ItemStack itemStack) {
    }

    @Override
    public boolean isRandomlyTicking(final BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && !this.isMaxAge(state);
    }

    @Override
    public void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        float growthSpeed = CropBlock.getGrowthSpeed(this, level, pos);
        boolean shouldProgressGrowth = random.nextInt((int)(25.0F / growthSpeed) + 1) == 0;
        if (shouldProgressGrowth) {
            this.grow(level, state, pos, 1);
        }
    }

    private void grow(final ServerLevel level, final BlockState lowerState, final BlockPos lowerPos, final int increase) {
        int updatedAge = Math.min(lowerState.getValue(AGE) + increase, 4);
        if (this.canGrow(level, lowerPos, lowerState, updatedAge)) {
            BlockState newLowerState = lowerState.setValue(AGE, updatedAge);
            level.setBlock(lowerPos, newLowerState, 2);
            if (isDouble(updatedAge)) {
                level.setBlock(lowerPos.above(), newLowerState.setValue(HALF, DoubleBlockHalf.UPPER), 3);
            }
        }
    }

    private static boolean canGrowInto(final LevelReader level, final BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.PITCHER_CROP);
    }

    private static boolean sufficientLight(final LevelReader level, final BlockPos pos) {
        return CropBlock.hasSufficientLight(level, pos);
    }

    private static boolean isLower(final BlockState state) {
        return state.is(Blocks.PITCHER_CROP) && state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    private static boolean isDouble(final int age) {
        return age >= 3;
    }

    private boolean canGrow(final LevelReader level, final BlockPos lowerPos, final BlockState lowerState, final int newAge) {
        return !this.isMaxAge(lowerState)
            && sufficientLight(level, lowerPos)
            && level.isInsideBuildHeight(lowerPos.above())
            && (!isDouble(newAge) || canGrowInto(level, lowerPos.above()));
    }

    private boolean isMaxAge(final BlockState state) {
        return state.getValue(AGE) >= 4;
    }

    private PitcherCropBlock.@Nullable PosAndState getLowerHalf(final LevelReader level, final BlockPos pos, final BlockState state) {
        if (isLower(state)) {
            return new PitcherCropBlock.PosAndState(pos, state);
        }

        BlockPos lowerPos = pos.below();
        BlockState lowerState = level.getBlockState(lowerPos);
        return isLower(lowerState) ? new PitcherCropBlock.PosAndState(lowerPos, lowerState) : null;
    }

    @Override
    public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
        PitcherCropBlock.PosAndState lowerHalf = this.getLowerHalf(level, pos, state);
        return lowerHalf == null ? false : this.canGrow(level, lowerHalf.pos, lowerHalf.state, lowerHalf.state.getValue(AGE) + 1);
    }

    @Override
    public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
        PitcherCropBlock.PosAndState lowerHalf = this.getLowerHalf(level, pos, state);
        if (lowerHalf != null) {
            this.grow(level, lowerHalf.state, lowerHalf.pos, 1);
        }
    }

    private record PosAndState(BlockPos pos, BlockState state) {
    }
}