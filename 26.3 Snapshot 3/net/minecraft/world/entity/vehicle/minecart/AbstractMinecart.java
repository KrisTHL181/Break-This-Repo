package net.minecraft.world.entity.vehicle.minecart;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractMinecart extends VehicleEntity {
   private static final Vec3 LOWERED_PASSENGER_ATTACHMENT = new Vec3(0.0, 0.0, 0.0);
   private static final EntityDataAccessor<Optional<BlockState>> DATA_ID_CUSTOM_DISPLAY_BLOCK = SynchedEntityData.defineId(
      AbstractMinecart.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE
   );
   private static final EntityDataAccessor<Integer> DATA_ID_DISPLAY_OFFSET = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
   private static final ImmutableMap<Pose, ImmutableList<Integer>> POSE_DISMOUNT_HEIGHTS = ImmutableMap.of(
      Pose.STANDING, ImmutableList.of(0, 1, -1), Pose.CROUCHING, ImmutableList.of(0, 1, -1), Pose.SWIMMING, ImmutableList.of(0, 1)
   );
   protected static final float WATER_SLOWDOWN_FACTOR = 0.95F;
   private static final boolean DEFAULT_FLIPPED_ROTATION = false;
   private boolean onRails;
   private boolean flipped = false;
   private final MinecartBehavior behavior;
   private static final Map<RailShape, Pair<Vec3i, Vec3i>> EXITS = Maps.newEnumMap(
      (Map)Util.make(
         () -> {
            Vec3i xNeg = Direction.WEST.getUnitVec3i();
            Vec3i xPos = Direction.EAST.getUnitVec3i();
            Vec3i zNeg = Direction.NORTH.getUnitVec3i();
            Vec3i zPos = Direction.SOUTH.getUnitVec3i();
            Vec3i xNegBelow = xNeg.below();
            Vec3i xPosBelow = xPos.below();
            Vec3i zNegBelow = zNeg.below();
            Vec3i zPosBelow = zPos.below();
            return ImmutableMap.of(
               RailShape.NORTH_SOUTH,
               Pair.of(zNeg, zPos),
               RailShape.EAST_WEST,
               Pair.of(xNeg, xPos),
               RailShape.ASCENDING_EAST,
               Pair.of(xNegBelow, xPos),
               RailShape.ASCENDING_WEST,
               Pair.of(xNeg, xPosBelow),
               RailShape.ASCENDING_NORTH,
               Pair.of(zNeg, zPosBelow),
               RailShape.ASCENDING_SOUTH,
               Pair.of(zNegBelow, zPos),
               RailShape.SOUTH_EAST,
               Pair.of(zPos, xPos),
               RailShape.SOUTH_WEST,
               Pair.of(zPos, xNeg),
               RailShape.NORTH_WEST,
               Pair.of(zNeg, xNeg),
               RailShape.NORTH_EAST,
               Pair.of(zNeg, xPos)
            );
         }
      )
   );

   protected AbstractMinecart(final EntityType<?> type, final Level level) {
      super(type, level);
      this.blocksBuilding = true;
      if (useExperimentalMovement(level)) {
         this.behavior = new NewMinecartBehavior(this);
      } else {
         this.behavior = new OldMinecartBehavior(this);
      }
   }

   protected AbstractMinecart(final EntityType<?> type, final Level level, final double x, final double y, final double z) {
      this(type, level);
      this.setInitialPos(x, y, z);
   }

   public void setInitialPos(final double x, final double y, final double z) {
      this.setPos(x, y, z);
      this.xo = x;
      this.yo = y;
      this.zo = z;
   }

   public static <T extends AbstractMinecart> @Nullable T createMinecart(
      final Level level,
      final double x,
      final double y,
      final double z,
      final EntityType<T> type,
      final EntitySpawnReason reason,
      final ItemStack itemStack,
      final @Nullable Player player
   ) {
      T entity = (T)type.create(level, reason);
      if (entity != null) {
         entity.setInitialPos(x, y, z);
         EntityType.createDefaultStackConfig(level, itemStack, player).apply(entity);
         if (entity.getBehavior() instanceof NewMinecartBehavior newMinecartBehavior) {
            BlockPos currentPos = entity.getCurrentBlockPosOrRailBelow();
            BlockState currentState = level.getBlockState(currentPos);
            newMinecartBehavior.adjustToRails(currentPos, currentState, true);
         }
      }

      return entity;
   }

   public MinecartBehavior getBehavior() {
      return this.behavior;
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_ID_CUSTOM_DISPLAY_BLOCK, Optional.empty());
      entityData.define(DATA_ID_DISPLAY_OFFSET, this.getDefaultDisplayOffset());
   }

   @Override
   public boolean canCollideWith(final Entity entity) {
      return AbstractBoat.canVehicleCollide(this, entity);
   }

   @Override
   public boolean isPushable() {
      return true;
   }

   @Override
   public Vec3 getRelativePortalPosition(final Direction.Axis axis, final BlockUtil.FoundRectangle portalArea) {
      return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, portalArea));
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(final Entity passenger, final EntityDimensions dimensions, final float scale) {
      boolean shouldLowerAttachmentPoint = passenger instanceof Villager || passenger instanceof WanderingTrader;
      return shouldLowerAttachmentPoint ? LOWERED_PASSENGER_ATTACHMENT : super.getPassengerAttachmentPoint(passenger, dimensions, scale);
   }

   @Override
   public Vec3 getDismountLocationForPassenger(final LivingEntity passenger) {
      Direction forward = this.getMotionDirection();
      if (forward.getAxis() == Direction.Axis.Y) {
         return super.getDismountLocationForPassenger(passenger);
      }

      int[][] offsets = DismountHelper.offsetsForDirection(forward);
      BlockPos vehicleBlockPos = this.blockPosition();
      BlockPos.MutableBlockPos targetBlockPos = new BlockPos.MutableBlockPos();
      ImmutableList<Pose> dismountPoses = passenger.getDismountPoses();
      UnmodifiableIterator vehicleTop = dismountPoses.iterator();

      while (vehicleTop.hasNext()) {
         Pose pose = (Pose)vehicleTop.next();
         EntityDimensions passengerDimensions = passenger.getDimensions(pose);
         float dismountAreaReach = Math.min(passengerDimensions.width(), 1.0F) / 2.0F;
         UnmodifiableIterator poseHeight = ((ImmutableList)POSE_DISMOUNT_HEIGHTS.get(pose)).iterator();

         while (poseHeight.hasNext()) {
            int offsetY = (Integer)poseHeight.next();

            for (int[] offsetXZ : offsets) {
               targetBlockPos.set(vehicleBlockPos.getX() + offsetXZ[0], vehicleBlockPos.getY() + offsetY, vehicleBlockPos.getZ() + offsetXZ[1]);
               double blockFloorHeight = this.level()
                  .getBlockFloorHeight(
                     DismountHelper.nonClimbableShape(this.level(), targetBlockPos),
                     () -> DismountHelper.nonClimbableShape(this.level(), targetBlockPos.below())
                  );
               if (DismountHelper.isBlockFloorValid(blockFloorHeight)) {
                  AABB dismountCollisionBox = new AABB(
                     -dismountAreaReach, 0.0, -dismountAreaReach, dismountAreaReach, passengerDimensions.height(), dismountAreaReach
                  );
                  Vec3 location = Vec3.upFromBottomCenterOf(targetBlockPos, blockFloorHeight);
                  if (DismountHelper.canDismountTo(this.level(), passenger, dismountCollisionBox.move(location))) {
                     passenger.setPose(pose);
                     return location;
                  }
               }
            }
         }
      }

      double vehicleTopx = this.getBoundingBox().maxY;
      targetBlockPos.set(vehicleBlockPos.getX(), vehicleTopx, vehicleBlockPos.getZ());
      UnmodifiableIterator var22 = dismountPoses.iterator();

      while (var22.hasNext()) {
         Pose pose = (Pose)var22.next();
         double poseHeight = passenger.getDimensions(pose).height();
         int blockCoverageY = Mth.ceil(vehicleTopx - targetBlockPos.getY() + poseHeight);
         double ceilingAboveVehicle = DismountHelper.findCeilingFrom(
            targetBlockPos, blockCoverageY, pos -> this.level().getBlockState(pos).getCollisionShape(this.level(), pos)
         );
         if (vehicleTopx + poseHeight <= ceilingAboveVehicle) {
            passenger.setPose(pose);
            break;
         }
      }

      return super.getDismountLocationForPassenger(passenger);
   }

   @Override
   protected float getBlockSpeedFactor() {
      BlockState blockState = this.level().getBlockState(this.blockPosition());
      return blockState.is(BlockTags.RAILS) ? 1.0F : super.getBlockSpeedFactor();
   }

   @Override
   public void animateHurt(final float yaw) {
      this.setHurtDir(-this.getHurtDir());
      this.setHurtTime(10);
      this.setDamage(this.getDamage() + this.getDamage() * 10.0F);
   }

   @Override
   public boolean isPickable() {
      return !this.isRemoved();
   }

   public static Pair<Vec3i, Vec3i> exits(final RailShape shape) {
      return EXITS.get(shape);
   }

   @Override
   public Direction getMotionDirection() {
      return this.behavior.getMotionDirection();
   }

   @Override
   protected double getDefaultGravity() {
      return this.isInWater() ? 0.005 : 0.04;
   }

   @Override
   public void tick() {
      if (this.getHurtTime() > 0) {
         this.setHurtTime(this.getHurtTime() - 1);
      }

      if (this.getDamage() > 0.0F) {
         this.setDamage(this.getDamage() - 1.0F);
      }

      this.checkBelowWorld();
      this.computeSpeed();
      this.handlePortal();
      this.behavior.tick();
      this.updateFluidInteraction();
      if (this.isInLava()) {
         this.lavaIgnite();
         this.lavaHurt();
         this.fallDistance *= 0.5;
      }

      this.firstTick = false;
   }

   public boolean isFirstTick() {
      return this.firstTick;
   }

   public BlockPos getCurrentBlockPosOrRailBelow() {
      int xt = Mth.floor(this.getX());
      int yt = Mth.floor(this.getY());
      int zt = Mth.floor(this.getZ());
      if (useExperimentalMovement(this.level())) {
         double y = this.getY() - 0.1 - 1.0E-5F;
         if (this.level().getBlockState(BlockPos.containing(xt, y, zt)).is(BlockTags.RAILS)) {
            yt = Mth.floor(y);
         }
      } else if (this.level().getBlockState(new BlockPos(xt, yt - 1, zt)).is(BlockTags.RAILS)) {
         yt--;
      }

      return new BlockPos(xt, yt, zt);
   }

   protected double getMaxSpeed(final ServerLevel level) {
      return this.behavior.getMaxSpeed(level);
   }

   public void activateMinecart(final ServerLevel level, final int xt, final int yt, final int zt, final boolean state) {
   }

   @Override
   public void lerpPositionAndRotationStep(
      final int stepsToTarget, final double targetX, final double targetY, final double targetZ, final double targetYRot, final double targetXRot
   ) {
      super.lerpPositionAndRotationStep(stepsToTarget, targetX, targetY, targetZ, targetYRot, targetXRot);
   }

   @Override
   public void applyGravity() {
      super.applyGravity();
   }

   @Override
   public void reapplyPosition() {
      super.reapplyPosition();
   }

   @Override
   public boolean updateFluidInteraction() {
      return super.updateFluidInteraction();
   }

   @Override
   public Vec3 getKnownMovement() {
      return this.behavior.getKnownMovement(super.getKnownMovement());
   }

   @Override
   public @Nullable InterpolationHandler getInterpolation() {
      return this.behavior.getInterpolation();
   }

   @Override
   public void onInterpolationStart(final InterpolationHandler interpolation) {
      this.behavior.onInterpolationStart(interpolation);
   }

   @Override
   public void recreateFromPacket(final ClientboundAddEntityPacket packet) {
      super.recreateFromPacket(packet);
      this.behavior.lerpMotion(this.getDeltaMovement());
   }

   @Override
   public void lerpMotion(final Vec3 movement) {
      this.behavior.lerpMotion(movement);
   }

   protected void moveAlongTrack(final ServerLevel level) {
      this.behavior.moveAlongTrack(level);
   }

   protected void comeOffTrack(final ServerLevel level) {
      double maxSpeed = this.getMaxSpeed(level);
      Vec3 movement = this.getDeltaMovement();
      this.setDeltaMovement(Mth.clamp(movement.x, -maxSpeed, maxSpeed), movement.y, Mth.clamp(movement.z, -maxSpeed, maxSpeed));
      if (this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
      }

      this.move(MoverType.SELF, this.getDeltaMovement());
      if (!this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().scale(this.getAirDrag()));
      }
   }

   @Override
   protected float getAirDrag() {
      return 0.95F;
   }

   protected double makeStepAlongTrack(final BlockPos pos, final RailShape shape, final double movementLeft) {
      return this.behavior.stepAlongTrack(pos, shape, movementLeft);
   }

   @Override
   public void move(final MoverType moverType, final Vec3 delta) {
      if (useExperimentalMovement(this.level())) {
         Vec3 toPosition = this.position().add(delta);
         super.move(moverType, delta);
         boolean shouldContinue = this.behavior.pushAndPickupEntities();
         if (shouldContinue) {
            super.move(moverType, toPosition.subtract(this.position()));
         }

         if (moverType.equals(MoverType.PISTON)) {
            this.onRails = false;
         }
      } else {
         super.move(moverType, delta);
         this.applyEffectsFromBlocks();
      }
   }

   @Override
   public void applyEffectsFromBlocks() {
      if (useExperimentalMovement(this.level())) {
         super.applyEffectsFromBlocks();
      } else {
         this.applyEffectsFromBlocks(this.position(), this.position());
         this.clearMovementThisTick();
      }
   }

   @Override
   public boolean isOnRails() {
      return this.onRails;
   }

   public void setOnRails(final boolean onRails) {
      this.onRails = onRails;
   }

   public boolean isFlipped() {
      return this.flipped;
   }

   public void setFlipped(final boolean flipped) {
      this.flipped = flipped;
   }

   public Vec3 getRedstoneDirection(final BlockPos pos) {
      BlockState state = this.level().getBlockState(pos);
      if (state.is(Blocks.POWERED_RAIL) && state.getValue(PoweredRailBlock.POWERED)) {
         RailShape shape = state.getValue(((BaseRailBlock)state.getBlock()).getShapeProperty());
         if (shape == RailShape.EAST_WEST) {
            if (this.isRedstoneConductor(pos.west())) {
               return new Vec3(1.0, 0.0, 0.0);
            }

            if (this.isRedstoneConductor(pos.east())) {
               return new Vec3(-1.0, 0.0, 0.0);
            }
         } else if (shape == RailShape.NORTH_SOUTH) {
            if (this.isRedstoneConductor(pos.north())) {
               return new Vec3(0.0, 0.0, 1.0);
            }

            if (this.isRedstoneConductor(pos.south())) {
               return new Vec3(0.0, 0.0, -1.0);
            }
         }

         return Vec3.ZERO;
      } else {
         return Vec3.ZERO;
      }
   }

   public boolean isRedstoneConductor(final BlockPos pos) {
      return this.level().getBlockState(pos).isRedstoneConductor(this.level(), pos);
   }

   protected Vec3 applyNaturalSlowdown(final Vec3 movement) {
      double slowdownFactor = this.behavior.getSlowdownFactor();
      Vec3 newMovement = movement.multiply(slowdownFactor, 0.0, slowdownFactor);
      if (this.isInWater()) {
         newMovement = newMovement.scale(0.95F);
      }

      return newMovement;
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      this.setCustomDisplayBlockState(input.read("DisplayState", BlockState.CODEC));
      this.setDisplayOffset(input.getIntOr("DisplayOffset", this.getDefaultDisplayOffset()));
      this.flipped = input.getBooleanOr("FlippedRotation", false);
      this.firstTick = input.getBooleanOr("HasTicked", false);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      this.getCustomDisplayBlockState().ifPresent(blockState -> output.store("DisplayState", BlockState.CODEC, blockState));
      int displayOffset = this.getDisplayOffset();
      if (displayOffset != this.getDefaultDisplayOffset()) {
         output.putInt("DisplayOffset", displayOffset);
      }

      output.putBoolean("FlippedRotation", this.flipped);
      output.putBoolean("HasTicked", this.firstTick);
   }

   @Override
   public void push(final Entity entity) {
      if (!this.level().isClientSide()) {
         if (!entity.noPhysics && !this.noPhysics) {
            if (!this.hasPassenger(entity)) {
               double xa = entity.getX() - this.getX();
               double za = entity.getZ() - this.getZ();
               double dd = xa * xa + za * za;
               if (dd >= 1.0E-4F) {
                  dd = Math.sqrt(dd);
                  xa /= dd;
                  za /= dd;
                  double pow = 1.0 / dd;
                  if (pow > 1.0) {
                     pow = 1.0;
                  }

                  xa *= pow;
                  za *= pow;
                  xa *= 0.1F;
                  za *= 0.1F;
                  xa *= 0.5;
                  za *= 0.5;
                  if (entity instanceof AbstractMinecart otherMinecart) {
                     this.pushOtherMinecart(otherMinecart, xa, za);
                  } else {
                     this.push(-xa, 0.0, -za);
                     entity.push(xa / 4.0, 0.0, za / 4.0);
                  }
               }
            }
         }
      }
   }

   private void pushOtherMinecart(final AbstractMinecart otherMinecart, final double xa, final double za) {
      double xo;
      double zo;
      if (useExperimentalMovement(this.level())) {
         xo = this.getDeltaMovement().x;
         zo = this.getDeltaMovement().z;
      } else {
         xo = otherMinecart.getX() - this.getX();
         zo = otherMinecart.getZ() - this.getZ();
      }

      Vec3 dir = new Vec3(xo, 0.0, zo).normalize();
      Vec3 facing = new Vec3(Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)), 0.0, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0))).normalize();
      double dot = Math.abs(dir.dot(facing));
      if (!(dot < 0.8F) || useExperimentalMovement(this.level())) {
         Vec3 movement = this.getDeltaMovement();
         Vec3 entityMovement = otherMinecart.getDeltaMovement();
         if (otherMinecart.isFurnace() && !this.isFurnace()) {
            this.setDeltaMovement(movement.multiply(0.2, 1.0, 0.2));
            this.push(entityMovement.x - xa, 0.0, entityMovement.z - za);
            otherMinecart.setDeltaMovement(entityMovement.multiply(0.95, 1.0, 0.95));
         } else if (!otherMinecart.isFurnace() && this.isFurnace()) {
            otherMinecart.setDeltaMovement(entityMovement.multiply(0.2, 1.0, 0.2));
            otherMinecart.push(movement.x + xa, 0.0, movement.z + za);
            this.setDeltaMovement(movement.multiply(0.95, 1.0, 0.95));
         } else {
            double xdd = (entityMovement.x + movement.x) / 2.0;
            double zdd = (entityMovement.z + movement.z) / 2.0;
            this.setDeltaMovement(movement.multiply(0.2, 1.0, 0.2));
            this.push(xdd - xa, 0.0, zdd - za);
            otherMinecart.setDeltaMovement(entityMovement.multiply(0.2, 1.0, 0.2));
            otherMinecart.push(xdd + xa, 0.0, zdd + za);
         }
      }
   }

   public BlockState getDisplayBlockState() {
      return this.getCustomDisplayBlockState().orElseGet(this::getDefaultDisplayBlockState);
   }

   private Optional<BlockState> getCustomDisplayBlockState() {
      return this.getEntityData().get(DATA_ID_CUSTOM_DISPLAY_BLOCK);
   }

   public BlockState getDefaultDisplayBlockState() {
      return Blocks.AIR.defaultBlockState();
   }

   public int getDisplayOffset() {
      return this.getEntityData().get(DATA_ID_DISPLAY_OFFSET);
   }

   public int getDefaultDisplayOffset() {
      return 6;
   }

   public void setCustomDisplayBlockState(final Optional<BlockState> state) {
      this.getEntityData().set(DATA_ID_CUSTOM_DISPLAY_BLOCK, state);
   }

   public void setDisplayOffset(final int offset) {
      this.getEntityData().set(DATA_ID_DISPLAY_OFFSET, offset);
   }

   public static boolean useExperimentalMovement(final Level level) {
      return level.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
   }

   @Override
   public abstract ItemStack getPickResult();

   public boolean isRideable() {
      return false;
   }

   public boolean isFurnace() {
      return false;
   }
}
