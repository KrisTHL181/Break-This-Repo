package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ChorusFlowerBlock extends Block {
    public static final MapCodec<ChorusFlowerBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("plant").forGetter(b -> b.plant), propertiesCodec()).apply(i, ChorusFlowerBlock::new)
    );
    public static final int DEAD_AGE = 5;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_5;
    private static final VoxelShape SHAPE_BLOCK_SUPPORT = Block.column(14.0, 0.0, 15.0);
    private final Block plant;

    @Override
    public MapCodec<ChorusFlowerBlock> codec() {
        return CODEC;
    }

    protected ChorusFlowerBlock(final Block plant, final BlockBehaviour.Properties properties) {
        super(properties);
        this.plant = plant;
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean isRandomlyTicking(final BlockState state) {
        return state.getValue(AGE) < 5;
    }

    @Override
    public VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return SHAPE_BLOCK_SUPPORT;
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        BlockPos above = pos.above();
        if (level.isEmptyBlock(above) && above.getY() <= level.getMaxY()) {
            int currentAge = state.getValue(AGE);
            if (currentAge < 5) {
                boolean growUpwards = false;
                boolean pillarOnSupportBlock = false;
                BlockState belowState = level.getBlockState(pos.below());
                if (belowState.is(BlockTags.SUPPORTS_CHORUS_FLOWER)) {
                    growUpwards = true;
                } else if (belowState.is(this.plant)) {
                    int height = 1;

                    for (int i = 0; i < 4; i++) {
                        BlockState testState = level.getBlockState(pos.below(height + 1));
                        if (!testState.is(this.plant)) {
                            if (testState.is(BlockTags.SUPPORTS_CHORUS_FLOWER)) {
                                pillarOnSupportBlock = true;
                            }
                            break;
                        }

                        height++;
                    }

                    if (height < 2 || height <= random.nextInt(pillarOnSupportBlock ? 5 : 4)) {
                        growUpwards = true;
                    }
                } else if (belowState.isAir()) {
                    growUpwards = true;
                }

                if (growUpwards && allNeighborsEmpty(level, above, null) && level.isEmptyBlock(pos.above(2))) {
                    level.setBlock(pos, ChorusPlantBlock.getStateWithConnections(level, pos, this.plant.defaultBlockState()), 2);
                    this.placeGrownFlower(level, above, currentAge);
                } else if (currentAge < 4) {
                    int numBranchAttempts = random.nextInt(4);
                    if (pillarOnSupportBlock) {
                        numBranchAttempts++;
                    }

                    boolean createdBranch = false;

                    for (int i = 0; i < numBranchAttempts; i++) {
                        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                        BlockPos target = pos.relative(direction);
                        if (level.isEmptyBlock(target) && level.isEmptyBlock(target.below()) && allNeighborsEmpty(level, target, direction.getOpposite())) {
                            this.placeGrownFlower(level, target, currentAge + 1);
                            createdBranch = true;
                        }
                    }

                    if (createdBranch) {
                        level.setBlock(pos, ChorusPlantBlock.getStateWithConnections(level, pos, this.plant.defaultBlockState()), 2);
                    } else {
                        this.placeDeadFlower(level, pos);
                    }
                } else {
                    this.placeDeadFlower(level, pos);
                }
            }
        }
    }

    private void placeGrownFlower(final Level level, final BlockPos pos, final int age) {
        level.setBlock(pos, this.defaultBlockState().setValue(AGE, age), 2);
        level.levelEvent(1033, pos, 0);
    }

    private void placeDeadFlower(final Level level, final BlockPos pos) {
        level.setBlock(pos, this.defaultBlockState().setValue(AGE, 5), 2);
        level.levelEvent(1034, pos, 0);
    }

    private static boolean allNeighborsEmpty(final LevelReader level, final BlockPos pos, final @Nullable Direction ignore) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != ignore && !level.isEmptyBlock(pos.relative(direction))) {
                return false;
            }
        }

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
        if (directionToNeighbour != Direction.UP && !state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }

        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        BlockState belowState = level.getBlockState(pos.below());
        if (!belowState.is(this.plant) && !belowState.is(BlockTags.SUPPORTS_CHORUS_FLOWER)) {
            if (!belowState.isAir()) {
                return false;
            }

            boolean oneNeighbor = false;

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockState neighbor = level.getBlockState(pos.relative(direction));
                if (neighbor.is(this.plant)) {
                    if (oneNeighbor) {
                        return false;
                    }

                    oneNeighbor = true;
                } else if (!neighbor.isAir()) {
                    return false;
                }
            }

            return oneNeighbor;
        } else {
            return true;
        }
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    public static void generatePlant(final LevelAccessor level, final BlockPos target, final RandomSource random, final int maxHorizontalSpread) {
        level.setBlock(target, ChorusPlantBlock.getStateWithConnections(level, target, Blocks.CHORUS_PLANT.defaultBlockState()), 2);
        growTreeRecursive(level, target, random, target, maxHorizontalSpread, 0);
    }

    private static void growTreeRecursive(
        final LevelAccessor level, final BlockPos current, final RandomSource random, final BlockPos startPos, final int maxHorizontalSpread, final int depth
    ) {
        Block chorus = Blocks.CHORUS_PLANT;
        int height = random.nextInt(4) + 1;
        if (depth == 0) {
            height++;
        }

        for (int i = 0; i < height; i++) {
            BlockPos target = current.above(i + 1);
            if (!allNeighborsEmpty(level, target, null)) {
                return;
            }

            level.setBlock(target, ChorusPlantBlock.getStateWithConnections(level, target, chorus.defaultBlockState()), 2);
            level.setBlock(target.below(), ChorusPlantBlock.getStateWithConnections(level, target.below(), chorus.defaultBlockState()), 2);
        }

        boolean placedStem = false;
        if (depth < 4) {
            int stems = random.nextInt(4);
            if (depth == 0) {
                stems++;
            }

            for (int i = 0; i < stems; i++) {
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos target = current.above(height).relative(direction);
                if (Math.abs(target.getX() - startPos.getX()) < maxHorizontalSpread
                    && Math.abs(target.getZ() - startPos.getZ()) < maxHorizontalSpread
                    && level.isEmptyBlock(target)
                    && level.isEmptyBlock(target.below())
                    && allNeighborsEmpty(level, target, direction.getOpposite())) {
                    placedStem = true;
                    level.setBlock(target, ChorusPlantBlock.getStateWithConnections(level, target, chorus.defaultBlockState()), 2);
                    level.setBlock(
                        target.relative(direction.getOpposite()),
                        ChorusPlantBlock.getStateWithConnections(level, target.relative(direction.getOpposite()), chorus.defaultBlockState()),
                        2
                    );
                    growTreeRecursive(level, target, random, startPos, maxHorizontalSpread, depth + 1);
                }
            }
        }

        if (!placedStem) {
            level.setBlock(current.above(height), Blocks.CHORUS_FLOWER.defaultBlockState().setValue(AGE, 5), 2);
        }
    }

    @Override
    protected void onProjectileHit(final Level level, final BlockState state, final BlockHitResult blockHit, final Projectile projectile) {
        BlockPos pos = blockHit.getBlockPos();
        if (level instanceof ServerLevel serverLevel && projectile.mayInteract(serverLevel, pos) && projectile.mayBreak(serverLevel)) {
            level.destroyBlock(pos, true, projectile);
        }
    }
}