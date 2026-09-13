package net.minecraft.world.level.block.piston;

import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PistonMovingBlockEntity extends BlockEntity {
    private static final int TICKS_TO_EXTEND = 2;
    private static final double PUSH_OFFSET = 0.01;
    public static final double TICK_MOVEMENT = 0.51;
    private static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
    private static final float DEFAULT_PROGRESS = 0.0F;
    private static final boolean DEFAULT_EXTENDING = false;
    private static final boolean DEFAULT_SOURCE = false;
    private BlockState movedState = DEFAULT_BLOCK_STATE;
    private Direction direction;
    private boolean extending = false;
    private boolean isSourcePiston = false;
    private static final ThreadLocal<Direction> NOCLIP = ThreadLocal.withInitial(() -> null);
    private float progress = 0.0F;
    private float progressO = 0.0F;
    private long lastTicked;
    private int deathTicks;

    public PistonMovingBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        super(BlockEntityTypes.PISTON, worldPosition, blockState);
    }

    public PistonMovingBlockEntity(
        final BlockPos worldPosition,
        final BlockState blockState,
        final BlockState movedState,
        final Direction direction,
        final boolean extending,
        final boolean isSourcePiston
    ) {
        this(worldPosition, blockState);
        this.movedState = movedState;
        this.direction = direction;
        this.extending = extending;
        this.isSourcePiston = isSourcePiston;
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    public boolean isExtending() {
        return this.extending;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public boolean isSourcePiston() {
        return this.isSourcePiston;
    }

    public float getProgress(float a) {
        if (a > 1.0F) {
            a = 1.0F;
        }

        return Mth.lerp(a, this.progressO, this.progress);
    }

    public float getXOff(final float a) {
        return this.direction.getStepX() * this.getExtendedProgress(this.getProgress(a));
    }

    public float getYOff(final float a) {
        return this.direction.getStepY() * this.getExtendedProgress(this.getProgress(a));
    }

    public float getZOff(final float a) {
        return this.direction.getStepZ() * this.getExtendedProgress(this.getProgress(a));
    }

    private float getExtendedProgress(final float progress) {
        return this.extending ? progress - 1.0F : 1.0F - progress;
    }

    private BlockState getCollisionRelatedBlockState() {
        return !this.isExtending() && this.isSourcePiston() && this.movedState.getBlock() instanceof PistonBaseBlock
            ? Blocks.PISTON_HEAD
                .defaultBlockState()
                .setValue(PistonHeadBlock.SHORT, this.progress > 0.25F)
                .setValue(PistonHeadBlock.TYPE, this.movedState.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT)
                .setValue(PistonHeadBlock.FACING, this.movedState.getValue(PistonBaseBlock.FACING))
            : this.movedState;
    }

    private static void moveCollidedEntities(final Level level, final BlockPos pos, final float newProgress, final PistonMovingBlockEntity self) {
        Direction movement = self.getMovementDirection();
        double deltaProgress = newProgress - self.progress;
        VoxelShape shape = self.getCollisionRelatedBlockState().getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            AABB aabb = moveByPositionAndProgress(pos, shape.bounds(), self);
            List<Entity> entities = level.getEntities(null, PistonMath.getMovementArea(aabb, movement, deltaProgress).minmax(aabb));
            if (!entities.isEmpty()) {
                List<AABB> shapeAabbs = shape.toAabbs();
                boolean causeBounce = self.movedState.is(Blocks.SLIME_BLOCK);
                Iterator var12 = entities.iterator();

                while (true) {
                    Entity entity;
                    while (true) {
                        if (!var12.hasNext()) {
                            return;
                        }

                        entity = (Entity)var12.next();
                        if (entity.getPistonPushReaction() != PushReaction.IGNORE) {
                            if (!causeBounce) {
                                break;
                            }

                            if (!(entity instanceof ServerPlayer)) {
                                Vec3 deltaMovement = entity.getDeltaMovement();
                                double dx = deltaMovement.x;
                                double dy = deltaMovement.y;
                                double dz = deltaMovement.z;
                                switch (movement.getAxis()) {
                                    case X:
                                        dx = movement.getStepX();
                                        break;
                                    case Y:
                                        dy = movement.getStepY();
                                        break;
                                    case Z:
                                        dz = movement.getStepZ();
                                }

                                entity.setDeltaMovement(dx, dy, dz);
                                break;
                            }
                        }
                    }

                    double delta = 0.0;

                    for (AABB shapeAabb : shapeAabbs) {
                        AABB movingAABB = PistonMath.getMovementArea(moveByPositionAndProgress(pos, shapeAabb, self), movement, deltaProgress);
                        AABB entityAabb = entity.getBoundingBox();
                        if (movingAABB.intersects(entityAabb)) {
                            delta = Math.max(delta, getMovement(movingAABB, movement, entityAabb));
                            if (delta >= deltaProgress) {
                                break;
                            }
                        }
                    }

                    if (!(delta <= 0.0)) {
                        delta = Math.min(delta, deltaProgress) + 0.01;
                        moveEntityByPiston(movement, entity, delta, movement);
                        if (!self.extending && self.isSourcePiston) {
                            fixEntityWithinPistonBase(pos, entity, movement, deltaProgress);
                        }
                    }
                }
            }
        }
    }

    private static void moveEntityByPiston(final Direction pistonDirection, final Entity entity, final double delta, final Direction movement) {
        NOCLIP.set(pistonDirection);
        Vec3 previousPos = entity.position();
        entity.move(MoverType.PISTON, new Vec3(delta * movement.getStepX(), delta * movement.getStepY(), delta * movement.getStepZ()));
        entity.applyEffectsFromBlocks(previousPos, entity.position());
        entity.removeLatestMovementRecording();
        NOCLIP.set(null);
    }

    private static void moveStuckEntities(final Level level, final BlockPos pos, final float newProgress, final PistonMovingBlockEntity self) {
        if (self.isStickyForEntities()) {
            Direction movement = self.getMovementDirection();
            if (movement.getAxis().isHorizontal()) {
                double stickyTop = self.movedState.getCollisionShape(level, pos).max(Direction.Axis.Y);
                AABB aabb = moveByPositionAndProgress(pos, new AABB(0.0, stickyTop, 0.0, 1.0, 1.5000010000000001, 1.0), self);
                double deltaProgress = newProgress - self.progress;

                for (Entity entity : level.getEntities((Entity)null, aabb, entityx -> matchesStickyCritera(aabb, entityx, pos))) {
                    moveEntityByPiston(movement, entity, deltaProgress, movement);
                }
            }
        }
    }

    private static boolean matchesStickyCritera(final AABB aabb, final Entity entity, final BlockPos pos) {
        return entity.getPistonPushReaction() == PushReaction.NORMAL
            && entity.onGround()
            && (
                entity.isSupportedBy(pos)
                    || entity.getX() >= aabb.minX && entity.getX() <= aabb.maxX && entity.getZ() >= aabb.minZ && entity.getZ() <= aabb.maxZ
            );
    }

    private boolean isStickyForEntities() {
        return this.movedState.is(Blocks.HONEY_BLOCK);
    }

    public Direction getMovementDirection() {
        return this.extending ? this.direction : this.direction.getOpposite();
    }

    private static double getMovement(final AABB aabbToBeOutsideOf, final Direction movement, final AABB aabb) {
        switch (movement) {
            case EAST:
                return aabbToBeOutsideOf.maxX - aabb.minX;
            case WEST:
                return aabb.maxX - aabbToBeOutsideOf.minX;
            case UP:
            default:
                return aabbToBeOutsideOf.maxY - aabb.minY;
            case DOWN:
                return aabb.maxY - aabbToBeOutsideOf.minY;
            case SOUTH:
                return aabbToBeOutsideOf.maxZ - aabb.minZ;
            case NORTH:
                return aabb.maxZ - aabbToBeOutsideOf.minZ;
        }
    }

    private static AABB moveByPositionAndProgress(final BlockPos pos, final AABB aabb, final PistonMovingBlockEntity entity) {
        double currentPosition = entity.getExtendedProgress(entity.progress);
        return aabb.move(
            pos.getX() + currentPosition * entity.direction.getStepX(),
            pos.getY() + currentPosition * entity.direction.getStepY(),
            pos.getZ() + currentPosition * entity.direction.getStepZ()
        );
    }

    private static void fixEntityWithinPistonBase(final BlockPos pos, final Entity entity, final Direction direction, final double deltaProgress) {
        AABB entityAabb = entity.getBoundingBox();
        AABB box = Shapes.block().bounds().move(pos);
        if (entityAabb.intersects(box)) {
            Direction opposite = direction.getOpposite();
            double delta = getMovement(box, opposite, entityAabb) + 0.01;
            double deltaIntersected = getMovement(box, opposite, entityAabb.intersect(box)) + 0.01;
            if (Math.abs(delta - deltaIntersected) < 0.01) {
                delta = Math.min(delta, deltaProgress) + 0.01;
                moveEntityByPiston(direction, entity, delta, opposite);
            }
        }
    }

    public BlockState getMovedState() {
        return this.movedState;
    }

    public void finalTick() {
        if (this.level != null && (this.progressO < 1.0F || this.level.isClientSide())) {
            this.progress = 1.0F;
            this.progressO = this.progress;
            this.level.removeBlockEntity(this.worldPosition);
            this.setRemoved();
            if (this.level.getBlockState(this.worldPosition).is(Blocks.MOVING_PISTON)) {
                BlockState newState;
                if (this.isSourcePiston) {
                    newState = Blocks.AIR.defaultBlockState();
                } else {
                    newState = Block.updateFromNeighbourShapes(this.movedState, this.level, this.worldPosition);
                }

                this.level.setBlock(this.worldPosition, newState, 3);
                this.level
                    .neighborChanged(
                        this.worldPosition, newState.getBlock(), ExperimentalRedstoneUtils.initialOrientation(this.level, this.getPushDirection(), null)
                    );
            }
        }
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        this.finalTick();
    }

    public Direction getPushDirection() {
        return this.extending ? this.direction : this.direction.getOpposite();
    }

    public static void tick(final Level level, final BlockPos pos, final BlockState state, final PistonMovingBlockEntity entity) {
        entity.lastTicked = level.getGameTime();
        entity.progressO = entity.progress;
        if (entity.progressO >= 1.0F) {
            if (level.isClientSide() && entity.deathTicks < 5) {
                entity.deathTicks++;
            } else {
                level.removeBlockEntity(pos);
                entity.setRemoved();
                if (level.getBlockState(pos).is(Blocks.MOVING_PISTON)) {
                    BlockState newState = Block.updateFromNeighbourShapes(entity.movedState, level, pos);
                    if (newState.isAir()) {
                        level.setBlock(pos, entity.movedState, 340);
                        Block.updateOrDestroy(entity.movedState, newState, level, pos, 3);
                    } else {
                        if (newState.hasProperty(BlockStateProperties.WATERLOGGED) && newState.getValue(BlockStateProperties.WATERLOGGED)) {
                            newState = newState.setValue(BlockStateProperties.WATERLOGGED, false);
                        }

                        level.setBlock(pos, newState, 67);
                        level.neighborChanged(pos, newState.getBlock(), ExperimentalRedstoneUtils.initialOrientation(level, entity.getPushDirection(), null));
                    }
                }
            }
        } else {
            float newProgress = entity.progress + 0.5F;
            moveCollidedEntities(level, pos, newProgress, entity);
            moveStuckEntities(level, pos, newProgress, entity);
            entity.progress = newProgress;
            if (entity.progress >= 1.0F) {
                entity.progress = 1.0F;
            }
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.movedState = input.read("blockState", BlockState.CODEC).orElse(DEFAULT_BLOCK_STATE);
        this.direction = input.read("facing", Direction.LEGACY_ID_CODEC).orElse(Direction.DOWN);
        this.progress = input.getFloatOr("progress", 0.0F);
        this.progressO = this.progress;
        this.extending = input.getBooleanOr("extending", false);
        this.isSourcePiston = input.getBooleanOr("source", false);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.store("blockState", BlockState.CODEC, this.movedState);
        output.store("facing", Direction.LEGACY_ID_CODEC, this.direction);
        output.putFloat("progress", this.progressO);
        output.putBoolean("extending", this.extending);
        output.putBoolean("source", this.isSourcePiston);
    }

    public VoxelShape getCollisionShape(final BlockGetter level, final BlockPos pos) {
        VoxelShape pistonHeadShape;
        if (!this.extending && this.isSourcePiston && this.movedState.getBlock() instanceof PistonBaseBlock) {
            pistonHeadShape = this.movedState.setValue(PistonBaseBlock.EXTENDED, true).getCollisionShape(level, pos);
        } else {
            pistonHeadShape = Shapes.empty();
        }

        Direction noClipDirection = NOCLIP.get();
        if (this.progress < 1.0 && noClipDirection == this.getMovementDirection()) {
            return pistonHeadShape;
        }

        BlockState blockState;
        if (this.isSourcePiston()) {
            blockState = Blocks.PISTON_HEAD
                .defaultBlockState()
                .setValue(PistonHeadBlock.FACING, this.direction)
                .setValue(PistonHeadBlock.SHORT, this.extending != 1.0F - this.progress < 0.25F);
        } else {
            blockState = this.movedState;
        }

        float extendedProgress = this.getExtendedProgress(this.progress);
        double dx = this.direction.getStepX() * extendedProgress;
        double dy = this.direction.getStepY() * extendedProgress;
        double dz = this.direction.getStepZ() * extendedProgress;
        return Shapes.or(pistonHeadShape, blockState.getCollisionShape(level, pos).move(dx, dy, dz));
    }

    public long getLastTicked() {
        return this.lastTicked;
    }

    @Override
    public void setLevel(final Level level) {
        super.setLevel(level);
        if (level.holderLookup(Registries.BLOCK).get(this.movedState.getBlock().builtInRegistryHolder().key()).isEmpty()) {
            this.movedState = Blocks.AIR.defaultBlockState();
        }
    }
}