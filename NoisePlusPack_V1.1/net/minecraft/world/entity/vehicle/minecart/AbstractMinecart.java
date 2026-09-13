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
            Vec3i vec3i = Direction.WEST.getUnitVec3i();
            Vec3i vec3i1 = Direction.EAST.getUnitVec3i();
            Vec3i vec3i2 = Direction.NORTH.getUnitVec3i();
            Vec3i vec3i3 = Direction.SOUTH.getUnitVec3i();
            Vec3i vec3i4 = vec3i.below();
            Vec3i vec3i5 = vec3i1.below();
            Vec3i vec3i6 = vec3i2.below();
            Vec3i vec3i7 = vec3i3.below();
            return ImmutableMap.of(
               RailShape.NORTH_SOUTH,
               Pair.of(vec3i2, vec3i3),
               RailShape.EAST_WEST,
               Pair.of(vec3i, vec3i1),
               RailShape.ASCENDING_EAST,
               Pair.of(vec3i4, vec3i1),
               RailShape.ASCENDING_WEST,
               Pair.of(vec3i, vec3i5),
               RailShape.ASCENDING_NORTH,
               Pair.of(vec3i2, vec3i7),
               RailShape.ASCENDING_SOUTH,
               Pair.of(vec3i6, vec3i3),
               RailShape.SOUTH_EAST,
               Pair.of(vec3i3, vec3i1),
               RailShape.SOUTH_WEST,
               Pair.of(vec3i3, vec3i),
               RailShape.NORTH_WEST,
               Pair.of(vec3i2, vec3i),
               RailShape.NORTH_EAST,
               Pair.of(vec3i2, vec3i1)
            );
         }
      )
   );

   protected AbstractMinecart(EntityType<?> p_452328_, Level p_453891_) {
      super(p_452328_, p_453891_);
      this.blocksBuilding = true;
      if (useExperimentalMovement(p_453891_)) {
         this.behavior = new NewMinecartBehavior(this);
      } else {
         this.behavior = new OldMinecartBehavior(this);
      }
   }

   protected AbstractMinecart(EntityType<?> p_455342_, Level p_453365_, double p_454565_, double p_451452_, double p_452544_) {
      this(p_455342_, p_453365_);
      this.setInitialPos(p_454565_, p_451452_, p_452544_);
   }

   public void setInitialPos(double p_453481_, double p_458456_, double p_457076_) {
      this.setPos(p_453481_, p_458456_, p_457076_);
      this.xo = p_453481_;
      this.yo = p_458456_;
      this.zo = p_457076_;
   }

   public static <T extends AbstractMinecart> @Nullable T createMinecart(
      Level p_455264_,
      double p_454967_,
      double p_450280_,
      double p_460267_,
      EntityType<T> p_459528_,
      EntitySpawnReason p_453547_,
      ItemStack p_459517_,
      @Nullable Player p_459840_
   ) {
      T t = (T)p_459528_.create(p_455264_, p_453547_);
      if (t != null) {
         t.setInitialPos(p_454967_, p_450280_, p_460267_);
         EntityType.createDefaultStackConfig(p_455264_, p_459517_, p_459840_).accept(t);
         if (t.getBehavior() instanceof NewMinecartBehavior newminecartbehavior) {
            BlockPos blockpos = t.getCurrentBlockPosOrRailBelow();
            BlockState blockstate = p_455264_.getBlockState(blockpos);
            newminecartbehavior.adjustToRails(blockpos, blockstate, true);
         }
      }

      return t;
   }

   public MinecartBehavior getBehavior() {
      return this.behavior;
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_451331_) {
      super.defineSynchedData(p_451331_);
      p_451331_.define(DATA_ID_CUSTOM_DISPLAY_BLOCK, Optional.empty());
      p_451331_.define(DATA_ID_DISPLAY_OFFSET, this.getDefaultDisplayOffset());
   }

   @Override
   public boolean canCollideWith(Entity p_461068_) {
      return AbstractBoat.canVehicleCollide(this, p_461068_);
   }

   @Override
   public boolean isPushable() {
      return true;
   }

   @Override
   public Vec3 getRelativePortalPosition(Direction.Axis p_456370_, BlockUtil.FoundRectangle p_450788_) {
      return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(p_456370_, p_450788_));
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity p_459451_, EntityDimensions p_460109_, float p_453698_) {
      boolean flag = p_459451_ instanceof Villager || p_459451_ instanceof WanderingTrader;
      return flag ? LOWERED_PASSENGER_ATTACHMENT : super.getPassengerAttachmentPoint(p_459451_, p_460109_, p_453698_);
   }

   @Override
   public Vec3 getDismountLocationForPassenger(LivingEntity p_453004_) {
      Direction direction = this.getMotionDirection();
      if (direction.getAxis() == Direction.Axis.Y) {
         return super.getDismountLocationForPassenger(p_453004_);
      }

      int[][] aint = DismountHelper.offsetsForDirection(direction);
      BlockPos blockpos = this.blockPosition();
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      ImmutableList<Pose> immutablelist = p_453004_.getDismountPoses();
      UnmodifiableIterator d1 = immutablelist.iterator();

      while (d1.hasNext()) {
         Pose pose = (Pose)d1.next();
         EntityDimensions entitydimensions = p_453004_.getDimensions(pose);
         float f = Math.min(entitydimensions.width(), 1.0F) / 2.0F;
         UnmodifiableIterator d2 = ((ImmutableList)POSE_DISMOUNT_HEIGHTS.get(pose)).iterator();

         while (d2.hasNext()) {
            int i = (Integer)d2.next();

            for (int[] aint1 : aint) {
               blockpos$mutableblockpos.set(blockpos.getX() + aint1[0], blockpos.getY() + i, blockpos.getZ() + aint1[1]);
               double d0 = this.level()
                  .getBlockFloorHeight(
                     DismountHelper.nonClimbableShape(this.level(), blockpos$mutableblockpos),
                     () -> DismountHelper.nonClimbableShape(this.level(), blockpos$mutableblockpos.below())
                  );
               if (DismountHelper.isBlockFloorValid(d0)) {
                  AABB aabb = new AABB(-f, 0.0, -f, f, entitydimensions.height(), f);
                  Vec3 vec3 = Vec3.upFromBottomCenterOf(blockpos$mutableblockpos, d0);
                  if (DismountHelper.canDismountTo(this.level(), p_453004_, aabb.move(vec3))) {
                     p_453004_.setPose(pose);
                     return vec3;
                  }
               }
            }
         }
      }

      double d1x = this.getBoundingBox().maxY;
      blockpos$mutableblockpos.set(blockpos.getX(), d1x, blockpos.getZ());
      UnmodifiableIterator var22 = immutablelist.iterator();

      while (var22.hasNext()) {
         Pose pose1 = (Pose)var22.next();
         double d2 = p_453004_.getDimensions(pose1).height();
         int j = Mth.ceil(d1x - blockpos$mutableblockpos.getY() + d2);
         double d3 = DismountHelper.findCeilingFrom(
            blockpos$mutableblockpos, j, p_450533_ -> this.level().getBlockState(p_450533_).getCollisionShape(this.level(), p_450533_)
         );
         if (d1x + d2 <= d3) {
            p_453004_.setPose(pose1);
            break;
         }
      }

      return super.getDismountLocationForPassenger(p_453004_);
   }

   @Override
   protected float getBlockSpeedFactor() {
      BlockState blockstate = this.level().getBlockState(this.blockPosition());
      return blockstate.is(BlockTags.RAILS) ? 1.0F : super.getBlockSpeedFactor();
   }

   @Override
   public void animateHurt(float p_453312_) {
      this.setHurtDir(-this.getHurtDir());
      this.setHurtTime(10);
      this.setDamage(this.getDamage() + this.getDamage() * 10.0F);
   }

   @Override
   public boolean isPickable() {
      return !this.isRemoved();
   }

   public static Pair<Vec3i, Vec3i> exits(RailShape p_454129_) {
      return EXITS.get(p_454129_);
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
      this.updateInWaterStateAndDoFluidPushing();
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
      int i = Mth.floor(this.getX());
      int j = Mth.floor(this.getY());
      int k = Mth.floor(this.getZ());
      if (useExperimentalMovement(this.level())) {
         double d0 = this.getY() - 0.1 - 1.0E-5F;
         if (this.level().getBlockState(BlockPos.containing(i, d0, k)).is(BlockTags.RAILS)) {
            j = Mth.floor(d0);
         }
      } else if (this.level().getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
         j--;
      }

      return new BlockPos(i, j, k);
   }

   protected double getMaxSpeed(ServerLevel p_460746_) {
      return this.behavior.getMaxSpeed(p_460746_);
   }

   public void activateMinecart(ServerLevel p_457918_, int p_459273_, int p_457555_, int p_451681_, boolean p_451146_) {
   }

   @Override
   public void lerpPositionAndRotationStep(int p_457718_, double p_456924_, double p_453241_, double p_451781_, double p_456392_, double p_450570_) {
      super.lerpPositionAndRotationStep(p_457718_, p_456924_, p_453241_, p_451781_, p_456392_, p_450570_);
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
   public boolean updateInWaterStateAndDoFluidPushing() {
      return super.updateInWaterStateAndDoFluidPushing();
   }

   @Override
   public Vec3 getKnownMovement() {
      return this.behavior.getKnownMovement(super.getKnownMovement());
   }

   @Override
   public InterpolationHandler getInterpolation() {
      return this.behavior.getInterpolation();
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_452417_) {
      super.recreateFromPacket(p_452417_);
      this.behavior.lerpMotion(this.getDeltaMovement());
   }

   @Override
   public void lerpMotion(Vec3 p_456883_) {
      this.behavior.lerpMotion(p_456883_);
   }

   protected void moveAlongTrack(ServerLevel p_457919_) {
      this.behavior.moveAlongTrack(p_457919_);
   }

   protected void comeOffTrack(ServerLevel p_460723_) {
      double d0 = this.getMaxSpeed(p_460723_);
      Vec3 vec3 = this.getDeltaMovement();
      this.setDeltaMovement(Mth.clamp(vec3.x, -d0, d0), vec3.y, Mth.clamp(vec3.z, -d0, d0));
      if (this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
      }

      this.move(MoverType.SELF, this.getDeltaMovement());
      if (!this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.95));
      }
   }

   protected double makeStepAlongTrack(BlockPos p_452232_, RailShape p_460524_, double p_457348_) {
      return this.behavior.stepAlongTrack(p_452232_, p_460524_, p_457348_);
   }

   @Override
   public void move(MoverType p_455369_, Vec3 p_458411_) {
      if (useExperimentalMovement(this.level())) {
         Vec3 vec3 = this.position().add(p_458411_);
         super.move(p_455369_, p_458411_);
         boolean flag = this.behavior.pushAndPickupEntities();
         if (flag) {
            super.move(p_455369_, vec3.subtract(this.position()));
         }

         if (p_455369_.equals(MoverType.PISTON)) {
            this.onRails = false;
         }
      } else {
         super.move(p_455369_, p_458411_);
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

   public void setOnRails(boolean p_452702_) {
      this.onRails = p_452702_;
   }

   public boolean isFlipped() {
      return this.flipped;
   }

   public void setFlipped(boolean p_456494_) {
      this.flipped = p_456494_;
   }

   public Vec3 getRedstoneDirection(BlockPos p_456192_) {
      BlockState blockstate = this.level().getBlockState(p_456192_);
      if (blockstate.is(Blocks.POWERED_RAIL) && blockstate.getValue(PoweredRailBlock.POWERED)) {
         RailShape railshape = blockstate.getValue(((BaseRailBlock)blockstate.getBlock()).getShapeProperty());
         if (railshape == RailShape.EAST_WEST) {
            if (this.isRedstoneConductor(p_456192_.west())) {
               return new Vec3(1.0, 0.0, 0.0);
            }

            if (this.isRedstoneConductor(p_456192_.east())) {
               return new Vec3(-1.0, 0.0, 0.0);
            }
         } else if (railshape == RailShape.NORTH_SOUTH) {
            if (this.isRedstoneConductor(p_456192_.north())) {
               return new Vec3(0.0, 0.0, 1.0);
            }

            if (this.isRedstoneConductor(p_456192_.south())) {
               return new Vec3(0.0, 0.0, -1.0);
            }
         }

         return Vec3.ZERO;
      } else {
         return Vec3.ZERO;
      }
   }

   public boolean isRedstoneConductor(BlockPos p_456207_) {
      return this.level().getBlockState(p_456207_).isRedstoneConductor(this.level(), p_456207_);
   }

   protected Vec3 applyNaturalSlowdown(Vec3 p_450255_) {
      double d0 = this.behavior.getSlowdownFactor();
      Vec3 vec3 = p_450255_.multiply(d0, 0.0, d0);
      if (this.isInWater()) {
         vec3 = vec3.scale(0.95F);
      }

      return vec3;
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_457529_) {
      this.setCustomDisplayBlockState(p_457529_.read("DisplayState", BlockState.CODEC));
      this.setDisplayOffset(p_457529_.getIntOr("DisplayOffset", this.getDefaultDisplayOffset()));
      this.flipped = p_457529_.getBooleanOr("FlippedRotation", false);
      this.firstTick = p_457529_.getBooleanOr("HasTicked", false);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_455537_) {
      this.getCustomDisplayBlockState().ifPresent(p_460577_ -> p_455537_.store("DisplayState", BlockState.CODEC, p_460577_));
      int i = this.getDisplayOffset();
      if (i != this.getDefaultDisplayOffset()) {
         p_455537_.putInt("DisplayOffset", i);
      }

      p_455537_.putBoolean("FlippedRotation", this.flipped);
      p_455537_.putBoolean("HasTicked", this.firstTick);
   }

   @Override
   public void push(Entity p_451226_) {
      if (!this.level().isClientSide() && !p_451226_.noPhysics && !this.noPhysics && !this.hasPassenger(p_451226_)) {
         double d0 = p_451226_.getX() - this.getX();
         double d1 = p_451226_.getZ() - this.getZ();
         double d2 = d0 * d0 + d1 * d1;
         if (d2 >= 1.0E-4F) {
            d2 = Math.sqrt(d2);
            d0 /= d2;
            d1 /= d2;
            double d3 = 1.0 / d2;
            if (d3 > 1.0) {
               d3 = 1.0;
            }

            d0 *= d3;
            d1 *= d3;
            d0 *= 0.1F;
            d1 *= 0.1F;
            d0 *= 0.5;
            d1 *= 0.5;
            if (p_451226_ instanceof AbstractMinecart abstractminecart) {
               this.pushOtherMinecart(abstractminecart, d0, d1);
            } else {
               this.push(-d0, 0.0, -d1);
               p_451226_.push(d0 / 4.0, 0.0, d1 / 4.0);
            }
         }
      }
   }

   private void pushOtherMinecart(AbstractMinecart p_454416_, double p_458570_, double p_450772_) {
      double d0;
      double d1;
      if (useExperimentalMovement(this.level())) {
         d0 = this.getDeltaMovement().x;
         d1 = this.getDeltaMovement().z;
      } else {
         d0 = p_454416_.getX() - this.getX();
         d1 = p_454416_.getZ() - this.getZ();
      }

      Vec3 vec3 = new Vec3(d0, 0.0, d1).normalize();
      Vec3 vec31 = new Vec3(Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)), 0.0, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0))).normalize();
      double d2 = Math.abs(vec3.dot(vec31));
      if (!(d2 < 0.8F) || useExperimentalMovement(this.level())) {
         Vec3 vec32 = this.getDeltaMovement();
         Vec3 vec33 = p_454416_.getDeltaMovement();
         if (p_454416_.isFurnace() && !this.isFurnace()) {
            this.setDeltaMovement(vec32.multiply(0.2, 1.0, 0.2));
            this.push(vec33.x - p_458570_, 0.0, vec33.z - p_450772_);
            p_454416_.setDeltaMovement(vec33.multiply(0.95, 1.0, 0.95));
         } else if (!p_454416_.isFurnace() && this.isFurnace()) {
            p_454416_.setDeltaMovement(vec33.multiply(0.2, 1.0, 0.2));
            p_454416_.push(vec32.x + p_458570_, 0.0, vec32.z + p_450772_);
            this.setDeltaMovement(vec32.multiply(0.95, 1.0, 0.95));
         } else {
            double d3 = (vec33.x + vec32.x) / 2.0;
            double d4 = (vec33.z + vec32.z) / 2.0;
            this.setDeltaMovement(vec32.multiply(0.2, 1.0, 0.2));
            this.push(d3 - p_458570_, 0.0, d4 - p_450772_);
            p_454416_.setDeltaMovement(vec33.multiply(0.2, 1.0, 0.2));
            p_454416_.push(d3 + p_458570_, 0.0, d4 + p_450772_);
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

   public void setCustomDisplayBlockState(Optional<BlockState> p_455986_) {
      this.getEntityData().set(DATA_ID_CUSTOM_DISPLAY_BLOCK, p_455986_);
   }

   public void setDisplayOffset(int p_457721_) {
      this.getEntityData().set(DATA_ID_DISPLAY_OFFSET, p_457721_);
   }

   public static boolean useExperimentalMovement(Level p_452543_) {
      return p_452543_.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
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
