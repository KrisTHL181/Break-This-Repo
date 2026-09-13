package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PointedDripstoneBlock extends SpeleothemBlock {
    public static final MapCodec<PointedDripstoneBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(BlockState.CODEC.fieldOf("block_to_grow_on").forGetter(b -> b.blockToGrowOn), propertiesCodec()).apply(i, PointedDripstoneBlock::new)
    );
    private static final int MAX_SEARCH_LENGTH_WHEN_CHECKING_DRIP_TYPE = 11;
    private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK = 0.02F;
    private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK_IF_UNDER_LIQUID_SOURCE = 0.12F;
    private static final int MAX_SEARCH_LENGTH_BETWEEN_STALACTITE_TIP_AND_CAULDRON = 11;
    private static final float WATER_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.17578125F;
    private static final float LAVA_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.05859375F;
    private static final float STALAGMITE_FALL_DISTANCE_OFFSET = 2.5F;
    private static final int STALAGMITE_FALL_DAMAGE_MODIFIER = 2;
    private static final double STALACTITE_DRIP_START_PIXEL = SHAPE_TIP_DOWN.min(Direction.Axis.Y);
    private static final VoxelShape REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK = Block.column(4.0, 0.0, 16.0);

    @Override
    public MapCodec<PointedDripstoneBlock> codec() {
        return CODEC;
    }

    public PointedDripstoneBlock(final BlockState blockToGrowOn, final BlockBehaviour.Properties properties) {
        super(blockToGrowOn, properties);
    }

    @Override
    protected int getStalactiteLandingSound() {
        return 1045;
    }

    @Override
    public void fallOn(final Level level, final BlockState state, final BlockPos pos, final Entity entity, final double fallDistance) {
        if (state.getValue(TIP_DIRECTION) == Direction.UP && state.getValue(THICKNESS) == SpeleothemThickness.TIP) {
            entity.causeFallDamage(fallDistance + 2.5, 2.0F, level.damageSources().stalagmite());
        } else {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (isFreeHangingStalactite(state)) {
            float randomValue = random.nextFloat();
            if (!(randomValue > 0.12F)) {
                getFluidAboveStalactite(level, pos, state)
                    .filter(fluidAbove -> randomValue < 0.02F || canFillCauldron(fluidAbove.fluid))
                    .ifPresent(fluidAbove -> spawnDripParticle(level, pos, state, fluidAbove.fluid, fluidAbove.pos));
            }
        }
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        maybeTransferFluid(state, level, pos, random.nextFloat());
        super.randomTick(state, level, pos, random);
    }

    @VisibleForTesting
    public static void maybeTransferFluid(final BlockState state, final ServerLevel level, final BlockPos pos, final float randomValue) {
        if (!(randomValue > 0.17578125F) || !(randomValue > 0.05859375F)) {
            if (isStalactiteStartPos(state, level, pos)) {
                Optional<PointedDripstoneBlock.FluidInfo> fluidInfo = getFluidAboveStalactite(level, pos, state);
                if (!fluidInfo.isEmpty()) {
                    Fluid fluid = fluidInfo.get().fluid;
                    float transferProbability;
                    if (fluid == Fluids.WATER) {
                        transferProbability = 0.17578125F;
                    } else {
                        if (fluid != Fluids.LAVA) {
                            return;
                        }

                        transferProbability = 0.05859375F;
                    }

                    if (!(randomValue >= transferProbability)) {
                        BlockPos stalactiteTipPos = findTip(state, level, pos, 11, false);
                        if (stalactiteTipPos != null) {
                            if (fluidInfo.get().sourceState.is(Blocks.MUD) && fluid == Fluids.WATER) {
                                BlockState newState = Blocks.CLAY.defaultBlockState();
                                level.setBlockAndUpdate(fluidInfo.get().pos, newState);
                                Block.pushEntitiesUp(fluidInfo.get().sourceState, newState, level, fluidInfo.get().pos);
                                level.gameEvent(GameEvent.BLOCK_CHANGE, fluidInfo.get().pos, GameEvent.Context.of(newState));
                                level.levelEvent(1504, stalactiteTipPos, 0);
                            } else {
                                BlockPos cauldronPos = findFillableCauldronBelowStalactiteTip(level, stalactiteTipPos, fluid);
                                if (cauldronPos != null) {
                                    level.levelEvent(1504, stalactiteTipPos, 0);
                                    int fallDistance = stalactiteTipPos.getY() - cauldronPos.getY();
                                    int delay = 50 + fallDistance;
                                    BlockState cauldronState = level.getBlockState(cauldronPos);
                                    level.scheduleTick(cauldronPos, cauldronState.getBlock(), delay);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void spawnDripParticle(final Level level, final BlockPos stalactiteTipPos, final BlockState stalactiteTipState) {
        getFluidAboveStalactite(level, stalactiteTipPos, stalactiteTipState)
            .ifPresent(fluidAbove -> spawnDripParticle(level, stalactiteTipPos, stalactiteTipState, fluidAbove.fluid, fluidAbove.pos));
    }

    private static void spawnDripParticle(
        final Level level, final BlockPos stalactiteTipPos, final BlockState stalactiteTipState, final Fluid fluidAbove, final BlockPos posAbove
    ) {
        Vec3 offset = stalactiteTipState.getOffset(stalactiteTipPos);
        double PIXEL_SIZE = 0.0625;
        double x = stalactiteTipPos.getX() + 0.5 + offset.x;
        double y = stalactiteTipPos.getY() + STALACTITE_DRIP_START_PIXEL - 0.0625;
        double z = stalactiteTipPos.getZ() + 0.5 + offset.z;
        ParticleOptions dripParticle = getDripParticle(level, fluidAbove, posAbove);
        level.addParticle(dripParticle, x, y, z, 0.0, 0.0, 0.0);
    }

    private static Optional<BlockPos> findRootBlock(final Level level, final BlockPos pos, final BlockState dripStoneState, final int maxSearchLength) {
        Direction tipDirection = dripStoneState.getValue(TIP_DIRECTION);
        BiPredicate<BlockPos, BlockState> pathPredicate = (pathPos, state) -> state.is(dripStoneState.getBlock())
            && state.getValue(TIP_DIRECTION) == tipDirection;
        return findBlockVertical(
            level, pos, tipDirection.getOpposite().getAxisDirection(), pathPredicate, state -> !state.is(dripStoneState.getBlock()), maxSearchLength
        );
    }

    private static @Nullable BlockPos findFillableCauldronBelowStalactiteTip(final Level level, final BlockPos stalactiteTipPos, final Fluid fluid) {
        Predicate<BlockState> cauldronPredicate = state -> state.getBlock() instanceof AbstractCauldronBlock
            && ((AbstractCauldronBlock)state.getBlock()).canReceiveStalactiteDrip(fluid);
        BiPredicate<BlockPos, BlockState> pathPredicate = (pos, state) -> canDripThrough(level, pos, state);
        return findBlockVertical(level, stalactiteTipPos, Direction.DOWN.getAxisDirection(), pathPredicate, cauldronPredicate, 11).orElse(null);
    }

    public static @Nullable BlockPos findStalactiteTipAboveCauldron(final Level level, final BlockPos cauldronPos) {
        BiPredicate<BlockPos, BlockState> pathPredicate = (pos, state) -> canDripThrough(level, pos, state);
        return findBlockVertical(level, cauldronPos, Direction.UP.getAxisDirection(), pathPredicate, SpeleothemBlock::isFreeHangingStalactite, 11).orElse(null);
    }

    public static Fluid getCauldronFillFluidType(final ServerLevel level, final BlockPos stalactitePos) {
        return getFluidAboveStalactite(level, stalactitePos, level.getBlockState(stalactitePos))
            .map(fluidSource -> fluidSource.fluid)
            .filter(PointedDripstoneBlock::canFillCauldron)
            .orElse(Fluids.EMPTY);
    }

    private static Optional<PointedDripstoneBlock.FluidInfo> getFluidAboveStalactite(
        final Level level, final BlockPos stalactitePos, final BlockState stalactiteState
    ) {
        return !isStalactite(stalactiteState) ? Optional.empty() : findRootBlock(level, stalactitePos, stalactiteState, 11).map(rootPos -> {
            BlockPos abovePos = rootPos.above();
            BlockState aboveState = level.getBlockState(abovePos);
            Fluid fluid;
            if (aboveState.is(Blocks.MUD) && !level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, abovePos)) {
                fluid = Fluids.WATER;
            } else {
                fluid = level.getFluidState(abovePos).getType();
            }

            return new PointedDripstoneBlock.FluidInfo(abovePos, fluid, aboveState);
        });
    }

    private static boolean canFillCauldron(final Fluid fluidAbove) {
        return fluidAbove == Fluids.LAVA || fluidAbove == Fluids.WATER;
    }

    @Override
    protected boolean canGrow(final LevelReader level, final BlockPos pos) {
        FluidState fluidState = level.getBlockState(pos.above(2)).getFluidState();
        return super.canGrow(level, pos) && fluidState.is(Fluids.WATER) && fluidState.isSource();
    }

    private static ParticleOptions getDripParticle(final Level level, final Fluid fluidAbove, final BlockPos posAbove) {
        if (fluidAbove.isSame(Fluids.EMPTY)) {
            return level.environmentAttributes().getValue(EnvironmentAttributes.DEFAULT_DRIPSTONE_PARTICLE, posAbove);
        } else {
            return fluidAbove.is(FluidTags.LAVA) ? ParticleTypes.DRIPPING_DRIPSTONE_LAVA : ParticleTypes.DRIPPING_DRIPSTONE_WATER;
        }
    }

    @Override
    protected boolean blocksStalagmiteScan(final LevelReader level, final BlockPos pos, final BlockState state) {
        return !canDripThrough(level, pos, state);
    }

    private static boolean canDripThrough(final BlockGetter level, final BlockPos pos, final BlockState state) {
        if (state.isAir()) {
            return true;
        }

        if (state.isSolidRender()) {
            return false;
        }

        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        VoxelShape collisionShape = state.getCollisionShape(level, pos);
        return !Shapes.joinIsNotEmpty(REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK, collisionShape, BooleanOp.AND);
    }

    private record FluidInfo(BlockPos pos, Fluid fluid, BlockState sourceState) {
    }
}