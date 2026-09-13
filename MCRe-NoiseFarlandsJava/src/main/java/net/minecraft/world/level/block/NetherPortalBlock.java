package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class NetherPortalBlock extends Block implements Portal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<NetherPortalBlock> CODEC = simpleCodec(NetherPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.column(4.0, 16.0, 0.0, 16.0));

    @Override
    public MapCodec<NetherPortalBlock> codec() {
        return CODEC;
    }

    public NetherPortalBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPES.get(state.getValue(AXIS));
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, BlockPos pos, final RandomSource random) {
        if (level.isSpawningMonsters()
            && level.getLevelData().getDifficulty() != Difficulty.PEACEFUL
            && level.environmentAttributes().getValue(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, pos)
            && random.nextInt(2000) < level.getDifficulty().getId()
            && level.anyPlayerCloseEnoughForSpawning(pos)) {
            while (level.getBlockState(pos).is(this)) {
                pos = pos.below();
            }

            if (level.getBlockState(pos).isValidSpawn(level, pos, EntityTypes.ZOMBIFIED_PIGLIN)) {
                Entity entity = EntityTypes.ZOMBIFIED_PIGLIN.spawn(level, pos.above(), EntitySpawnReason.STRUCTURE);
                if (entity != null) {
                    entity.setPortalCooldown();
                    Entity vehicle = entity.getVehicle();
                    if (vehicle != null) {
                        vehicle.setPortalCooldown();
                    }
                }
            }
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
        Direction.Axis updateAxis = directionToNeighbour.getAxis();
        Direction.Axis axis = state.getValue(AXIS);
        boolean wrongAxis = axis != updateAxis && updateAxis.isHorizontal();
        return !wrongAxis && !neighbourState.is(this) && !PortalShape.findAnyShape(level, pos, axis).isComplete()
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
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
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(final ServerLevel level, final Entity entity) {
        return entity instanceof Player player
            ? Math.max(
                0,
                level.getGameRules()
                    .get(player.getAbilities().invulnerable ? GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY)
            )
            : 0;
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(final ServerLevel currentLevel, final Entity entity, final BlockPos portalEntryPos) {
        ResourceKey<Level> newDimension = currentLevel.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
        ServerLevel newLevel = currentLevel.getServer().getLevel(newDimension);
        if (newLevel == null) {
            return null;
        }

        boolean toNether = newLevel.dimension() == Level.NETHER;
        WorldBorder newWorldBorder = newLevel.getWorldBorder();
        double teleportationScale = DimensionType.getTeleportationScale(currentLevel.dimensionType(), newLevel.dimensionType());
        BlockPos approximateExitPos = newWorldBorder.clampToBounds(entity.getX() * teleportationScale, entity.getY(), entity.getZ() * teleportationScale);
        return this.getExitPortal(newLevel, entity, portalEntryPos, approximateExitPos, toNether, newWorldBorder);
    }

    private @Nullable TeleportTransition getExitPortal(
        final ServerLevel newLevel,
        final Entity entity,
        final BlockPos portalEntryPos,
        final BlockPos approximateExitPos,
        final boolean toNether,
        final WorldBorder worldBorder
    ) {
        Optional<BlockPos> exitPortalPos = newLevel.getPortalForcer().findClosestPortalPosition(approximateExitPos, toNether, worldBorder);
        BlockUtil.FoundRectangle exitPortal;
        TeleportTransition.PostTeleportTransition post;
        if (exitPortalPos.isPresent()) {
            BlockPos pos = exitPortalPos.get();
            BlockState portalState = newLevel.getBlockState(pos);
            exitPortal = BlockUtil.getLargestRectangleAround(
                pos,
                portalState.getValue(BlockStateProperties.HORIZONTAL_AXIS),
                21,
                Direction.Axis.Y,
                21,
                blockPos -> newLevel.getBlockState(blockPos) == portalState
            );
            post = TeleportTransition.PLAY_PORTAL_SOUND.then(e -> e.placePortalTicket(pos));
        } else {
            Direction.Axis sourcePortalAxis = entity.level().getBlockState(portalEntryPos).getOptionalValue(AXIS).orElse(Direction.Axis.X);
            Optional<BlockUtil.FoundRectangle> createdExit = newLevel.getPortalForcer().createPortal(approximateExitPos, sourcePortalAxis);
            if (createdExit.isEmpty()) {
                LOGGER.error("Unable to create a portal, likely target out of worldborder");
                return null;
            }

            exitPortal = createdExit.get();
            post = TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET);
        }

        return getDimensionTransitionFromExit(entity, portalEntryPos, exitPortal, newLevel, post);
    }

    private static TeleportTransition getDimensionTransitionFromExit(
        final Entity entity,
        final BlockPos portalEntryPos,
        final BlockUtil.FoundRectangle exitPortal,
        final ServerLevel newLevel,
        final TeleportTransition.PostTeleportTransition postTeleportTransition
    ) {
        BlockState blockState = entity.level().getBlockState(portalEntryPos);
        Direction.Axis axis;
        Vec3 offset;
        if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            axis = blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
            BlockUtil.FoundRectangle portalArea = BlockUtil.getLargestRectangleAround(
                portalEntryPos, axis, 21, Direction.Axis.Y, 21, pos -> entity.level().getBlockState(pos) == blockState
            );
            offset = entity.getRelativePortalPosition(axis, portalArea);
        } else {
            axis = Direction.Axis.X;
            offset = new Vec3(0.5, 0.0, 0.0);
        }

        return createDimensionTransition(newLevel, exitPortal, axis, offset, entity, postTeleportTransition);
    }

    private static TeleportTransition createDimensionTransition(
        final ServerLevel newLevel,
        final BlockUtil.FoundRectangle foundRectangle,
        final Direction.Axis portalAxis,
        final Vec3 offset,
        final Entity entity,
        final TeleportTransition.PostTeleportTransition postTeleportTransition
    ) {
        BlockPos bottomLeft = foundRectangle.minCorner;
        BlockState blockState = newLevel.getBlockState(bottomLeft);
        Direction.Axis axis = blockState.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
        double width = foundRectangle.axis1Size;
        double height = foundRectangle.axis2Size;
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        int outputRotation = portalAxis == axis ? 0 : 90;
        double offsetRight = dimensions.width() / 2.0 + (width - dimensions.width()) * offset.x();
        double offsetUp = (height - dimensions.height()) * offset.y();
        double offsetForward = 0.5 + offset.z();
        boolean xAligned = axis == Direction.Axis.X;
        Vec3 targetPos = new Vec3(
            bottomLeft.getX() + (xAligned ? offsetRight : offsetForward),
            bottomLeft.getY() + offsetUp,
            bottomLeft.getZ() + (xAligned ? offsetForward : offsetRight)
        );
        Vec3 collisionFreePos = PortalShape.findCollisionFreePosition(targetPos, newLevel, entity, dimensions);
        return new TeleportTransition(
            newLevel, collisionFreePos, Vec3.ZERO, outputRotation, 0.0F, Relative.union(Relative.DELTA, Relative.ROTATION), postTeleportTransition
        );
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                SoundEvents.PORTAL_AMBIENT,
                SoundSource.BLOCKS,
                0.5F,
                random.nextFloat() * 0.4F + 0.8F,
                false
            );
        }

        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double xa = (random.nextFloat() - 0.5) * 0.5;
            double ya = (random.nextFloat() - 0.5) * 0.5;
            double za = (random.nextFloat() - 0.5) * 0.5;
            int flip = random.nextInt(2) * 2 - 1;
            if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
                x = pos.getX() + 0.5 + 0.25 * flip;
                xa = random.nextFloat() * 2.0F * flip;
            } else {
                z = pos.getZ() + 0.5 + 0.25 * flip;
                za = random.nextFloat() * 2.0F * flip;
            }

            level.addParticle(ParticleTypes.PORTAL, x, y, z, xa, ya, za);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state, final boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                switch ((Direction.Axis)state.getValue(AXIS)) {
                    case X:
                        return state.setValue(AXIS, Direction.Axis.Z);
                    case Z:
                        return state.setValue(AXIS, Direction.Axis.X);
                    default:
                        return state;
                }
            default:
                return state;
        }
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}