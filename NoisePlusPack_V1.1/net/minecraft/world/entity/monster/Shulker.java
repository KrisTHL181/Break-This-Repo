package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class Shulker extends AbstractGolem implements Enemy {
   private static final Identifier COVERED_ARMOR_MODIFIER_ID = Identifier.withDefaultNamespace("covered");
   private static final AttributeModifier COVERED_ARMOR_MODIFIER = new AttributeModifier(COVERED_ARMOR_MODIFIER_ID, 20.0, AttributeModifier.Operation.ADD_VALUE);
   protected static final EntityDataAccessor<Direction> DATA_ATTACH_FACE_ID = SynchedEntityData.defineId(Shulker.class, EntityDataSerializers.DIRECTION);
   protected static final EntityDataAccessor<Byte> DATA_PEEK_ID = SynchedEntityData.defineId(Shulker.class, EntityDataSerializers.BYTE);
   protected static final EntityDataAccessor<Byte> DATA_COLOR_ID = SynchedEntityData.defineId(Shulker.class, EntityDataSerializers.BYTE);
   private static final int TELEPORT_STEPS = 6;
   private static final byte NO_COLOR = 16;
   private static final byte DEFAULT_COLOR = 16;
   private static final int MAX_TELEPORT_DISTANCE = 8;
   private static final int OTHER_SHULKER_SCAN_RADIUS = 8;
   private static final int OTHER_SHULKER_LIMIT = 5;
   private static final float PEEK_PER_TICK = 0.05F;
   private static final byte DEFAULT_PEEK = 0;
   private static final Direction DEFAULT_ATTACH_FACE = Direction.DOWN;
   static final Vector3f FORWARD = Util.make(() -> {
      Vec3i vec3i = Direction.SOUTH.getUnitVec3i();
      return new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ());
   });
   private static final float MAX_SCALE = 3.0F;
   private float currentPeekAmountO;
   private float currentPeekAmount;
   private @Nullable BlockPos clientOldAttachPosition;
   private int clientSideTeleportInterpolation;
   private static final float MAX_LID_OPEN = 1.0F;

   public Shulker(EntityType<? extends Shulker> p_33404_, Level p_33405_) {
      super(p_33404_, p_33405_);
      this.xpReward = 5;
      this.lookControl = new Shulker.ShulkerLookControl(this);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.02F, true));
      this.goalSelector.addGoal(4, new Shulker.ShulkerAttackGoal());
      this.goalSelector.addGoal(7, new Shulker.ShulkerPeekGoal());
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, this.getClass()).setAlertOthers());
      this.targetSelector.addGoal(2, new Shulker.ShulkerNearestAttackGoal(this));
      this.targetSelector.addGoal(3, new Shulker.ShulkerDefenseAttackGoal(this));
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.SHULKER_AMBIENT;
   }

   @Override
   public void playAmbientSound() {
      if (!this.isClosed()) {
         super.playAmbientSound();
      }
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.SHULKER_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_33457_) {
      return this.isClosed() ? SoundEvents.SHULKER_HURT_CLOSED : SoundEvents.SHULKER_HURT;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_335590_) {
      super.defineSynchedData(p_335590_);
      p_335590_.define(DATA_ATTACH_FACE_ID, DEFAULT_ATTACH_FACE);
      p_335590_.define(DATA_PEEK_ID, (byte)0);
      p_335590_.define(DATA_COLOR_ID, (byte)16);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0);
   }

   @Override
   protected BodyRotationControl createBodyControl() {
      return new Shulker.ShulkerBodyRotationControl(this);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_410051_) {
      super.readAdditionalSaveData(p_410051_);
      this.setAttachFace(p_410051_.<Direction>read("AttachFace", Direction.LEGACY_ID_CODEC).orElse(DEFAULT_ATTACH_FACE));
      this.entityData.set(DATA_PEEK_ID, p_410051_.getByteOr("Peek", (byte)0));
      this.entityData.set(DATA_COLOR_ID, p_410051_.getByteOr("Color", (byte)16));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_409221_) {
      super.addAdditionalSaveData(p_409221_);
      p_409221_.store("AttachFace", Direction.LEGACY_ID_CODEC, this.getAttachFace());
      p_409221_.putByte("Peek", this.entityData.get(DATA_PEEK_ID));
      p_409221_.putByte("Color", this.entityData.get(DATA_COLOR_ID));
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && !this.isPassenger() && !this.canStayAt(this.blockPosition(), this.getAttachFace())) {
         this.findNewAttachment();
      }

      if (this.updatePeekAmount()) {
         this.onPeekAmountChange();
      }

      if (this.level().isClientSide()) {
         if (this.clientSideTeleportInterpolation > 0) {
            this.clientSideTeleportInterpolation--;
         } else {
            this.clientOldAttachPosition = null;
         }
      }
   }

   private void findNewAttachment() {
      Direction direction = this.findAttachableSurface(this.blockPosition());
      if (direction != null) {
         this.setAttachFace(direction);
      } else {
         this.teleportSomewhere();
      }
   }

   @Override
   protected AABB makeBoundingBox(Vec3 p_378289_) {
      float f = getPhysicalPeek(this.currentPeekAmount);
      Direction direction = this.getAttachFace().getOpposite();
      return getProgressAabb(this.getScale(), direction, f, p_378289_);
   }

   private static float getPhysicalPeek(float p_149769_) {
      return 0.5F - Mth.sin((0.5F + p_149769_) * (float) Math.PI) * 0.5F;
   }

   private boolean updatePeekAmount() {
      this.currentPeekAmountO = this.currentPeekAmount;
      float f = this.getRawPeekAmount() * 0.01F;
      if (this.currentPeekAmount == f) {
         return false;
      }

      if (this.currentPeekAmount > f) {
         this.currentPeekAmount = Mth.clamp(this.currentPeekAmount - 0.05F, f, 1.0F);
      } else {
         this.currentPeekAmount = Mth.clamp(this.currentPeekAmount + 0.05F, 0.0F, f);
      }

      return true;
   }

   private void onPeekAmountChange() {
      this.reapplyPosition();
      float f = getPhysicalPeek(this.currentPeekAmount);
      float f1 = getPhysicalPeek(this.currentPeekAmountO);
      Direction direction = this.getAttachFace().getOpposite();
      float f2 = (f - f1) * this.getScale();
      if (!(f2 <= 0.0F)) {
         for (Entity entity : this.level()
            .getEntities(
               this,
               getProgressDeltaAabb(this.getScale(), direction, f1, f, this.position()),
               EntitySelector.NO_SPECTATORS.and(p_149771_ -> !p_149771_.isPassengerOfSameVehicle(this))
            )) {
            if (!(entity instanceof Shulker) && !entity.noPhysics) {
               entity.move(MoverType.SHULKER, new Vec3(f2 * direction.getStepX(), f2 * direction.getStepY(), f2 * direction.getStepZ()));
            }
         }
      }
   }

   public static AABB getProgressAabb(float p_149792_, Direction p_149791_, float p_330131_, Vec3 p_375540_) {
      return getProgressDeltaAabb(p_149792_, p_149791_, -1.0F, p_330131_, p_375540_);
   }

   public static AABB getProgressDeltaAabb(float p_149795_, Direction p_149794_, float p_149796_, float p_333964_, Vec3 p_378565_) {
      AABB aabb = new AABB(-p_149795_ * 0.5, 0.0, -p_149795_ * 0.5, p_149795_ * 0.5, p_149795_, p_149795_ * 0.5);
      double d0 = Math.max(p_149796_, p_333964_);
      double d1 = Math.min(p_149796_, p_333964_);
      AABB aabb1 = aabb.expandTowards(p_149794_.getStepX() * d0 * p_149795_, p_149794_.getStepY() * d0 * p_149795_, p_149794_.getStepZ() * d0 * p_149795_)
         .contract(
            -p_149794_.getStepX() * (1.0 + d1) * p_149795_, -p_149794_.getStepY() * (1.0 + d1) * p_149795_, -p_149794_.getStepZ() * (1.0 + d1) * p_149795_
         );
      return aabb1.move(p_378565_.x, p_378565_.y, p_378565_.z);
   }

   @Override
   public boolean startRiding(Entity p_149773_, boolean p_149774_, boolean p_423222_) {
      if (this.level().isClientSide()) {
         this.clientOldAttachPosition = null;
         this.clientSideTeleportInterpolation = 0;
      }

      this.setAttachFace(Direction.DOWN);
      return super.startRiding(p_149773_, p_149774_, p_423222_);
   }

   @Override
   public void stopRiding() {
      super.stopRiding();
      if (this.level().isClientSide()) {
         this.clientOldAttachPosition = this.blockPosition();
      }

      this.yBodyRotO = 0.0F;
      this.yBodyRot = 0.0F;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_149780_, DifficultyInstance p_149781_, EntitySpawnReason p_365010_, @Nullable SpawnGroupData p_149783_
   ) {
      this.setYRot(0.0F);
      this.yHeadRot = this.getYRot();
      this.setOldPosAndRot();
      return super.finalizeSpawn(p_149780_, p_149781_, p_365010_, p_149783_);
   }

   @Override
   public void move(MoverType p_33424_, Vec3 p_33425_) {
      if (p_33424_ == MoverType.SHULKER_BOX) {
         this.teleportSomewhere();
      } else {
         super.move(p_33424_, p_33425_);
      }
   }

   @Override
   public Vec3 getDeltaMovement() {
      return Vec3.ZERO;
   }

   @Override
   public void setDeltaMovement(Vec3 p_149804_) {
   }

   @Override
   public void setPos(double p_33449_, double p_33450_, double p_33451_) {
      BlockPos blockpos = this.blockPosition();
      if (this.isPassenger()) {
         super.setPos(p_33449_, p_33450_, p_33451_);
      } else {
         super.setPos(Mth.floor(p_33449_) + 0.5, Mth.floor(p_33450_ + 0.5), Mth.floor(p_33451_) + 0.5);
      }

      if (this.tickCount != 0) {
         BlockPos blockpos1 = this.blockPosition();
         if (!blockpos1.equals(blockpos)) {
            this.entityData.set(DATA_PEEK_ID, (byte)0);
            this.needsSync = true;
            if (this.level().isClientSide() && !this.isPassenger() && !blockpos1.equals(this.clientOldAttachPosition)) {
               this.clientOldAttachPosition = blockpos;
               this.clientSideTeleportInterpolation = 6;
               this.xOld = this.getX();
               this.yOld = this.getY();
               this.zOld = this.getZ();
            }
         }
      }
   }

   protected @Nullable Direction findAttachableSurface(BlockPos p_149811_) {
      for (Direction direction : Direction.values()) {
         if (this.canStayAt(p_149811_, direction)) {
            return direction;
         }
      }

      return null;
   }

   boolean canStayAt(BlockPos p_149786_, Direction p_149787_) {
      if (this.isPositionBlocked(p_149786_)) {
         return false;
      }

      Direction direction = p_149787_.getOpposite();
      if (!this.level().loadedAndEntityCanStandOnFace(p_149786_.relative(p_149787_), this, direction)) {
         return false;
      }

      AABB aabb = getProgressAabb(this.getScale(), direction, 1.0F, p_149786_.getBottomCenter()).deflate(1.0E-6);
      return this.level().noCollision(this, aabb);
   }

   private boolean isPositionBlocked(BlockPos p_149813_) {
      BlockState blockstate = this.level().getBlockState(p_149813_);
      if (blockstate.isAir()) {
         return false;
      }

      boolean flag = blockstate.is(Blocks.MOVING_PISTON) && p_149813_.equals(this.blockPosition());
      return !flag;
   }

   protected boolean teleportSomewhere() {
      if (!this.isNoAi() && this.isAlive()) {
         BlockPos blockpos = this.blockPosition();

         for (int i = 0; i < 5; i++) {
            BlockPos blockpos1 = blockpos.offset(
               Mth.randomBetweenInclusive(this.random, -8, 8), Mth.randomBetweenInclusive(this.random, -8, 8), Mth.randomBetweenInclusive(this.random, -8, 8)
            );
            if (blockpos1.getY() > this.level().getMinY()
               && this.level().isEmptyBlock(blockpos1)
               && this.level().getWorldBorder().isWithinBounds(blockpos1)
               && this.level().noCollision(this, new AABB(blockpos1).deflate(1.0E-6))) {
               Direction direction = this.findAttachableSurface(blockpos1);
               if (direction != null) {
                  this.unRide();
                  this.setAttachFace(direction);
                  this.playSound(SoundEvents.SHULKER_TELEPORT, 1.0F, 1.0F);
                  this.setPos(blockpos1.getX() + 0.5, blockpos1.getY(), blockpos1.getZ() + 0.5);
                  this.level().gameEvent(GameEvent.TELEPORT, blockpos, GameEvent.Context.of(this));
                  this.entityData.set(DATA_PEEK_ID, (byte)0);
                  this.setTarget(null);
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   public InterpolationHandler getInterpolation() {
      return null;
   }

   @Override
   public boolean hurtServer(ServerLevel p_366136_, DamageSource p_366119_, float p_367361_) {
      if (this.isClosed()) {
         Entity entity = p_366119_.getDirectEntity();
         if (entity instanceof AbstractArrow) {
            return false;
         }
      }

      if (!super.hurtServer(p_366136_, p_366119_, p_367361_)) {
         return false;
      }

      if (this.getHealth() < this.getMaxHealth() * 0.5 && this.random.nextInt(4) == 0) {
         this.teleportSomewhere();
      } else if (p_366119_.is(DamageTypeTags.IS_PROJECTILE)) {
         Entity entity1 = p_366119_.getDirectEntity();
         if (entity1 != null && entity1.getType() == EntityType.SHULKER_BULLET) {
            this.hitByShulkerBullet();
         }
      }

      return true;
   }

   private boolean isClosed() {
      return this.getRawPeekAmount() == 0;
   }

   private void hitByShulkerBullet() {
      Vec3 vec3 = this.position();
      AABB aabb = this.getBoundingBox();
      if (!this.isClosed() && this.teleportSomewhere()) {
         int i = this.level().getEntities(EntityType.SHULKER, aabb.inflate(8.0), Entity::isAlive).size();
         float f = (i - 1) / 5.0F;
         if (!(this.level().random.nextFloat() < f)) {
            Shulker shulker = EntityType.SHULKER.create(this.level(), EntitySpawnReason.BREEDING);
            if (shulker != null) {
               shulker.setVariant(this.getVariant());
               shulker.snapTo(vec3);
               this.level().addFreshEntity(shulker);
            }
         }
      }
   }

   @Override
   public boolean canBeCollidedWith(@Nullable Entity p_408988_) {
      return this.isAlive();
   }

   public Direction getAttachFace() {
      return this.entityData.get(DATA_ATTACH_FACE_ID);
   }

   private void setAttachFace(Direction p_149789_) {
      this.entityData.set(DATA_ATTACH_FACE_ID, p_149789_);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_33434_) {
      if (DATA_ATTACH_FACE_ID.equals(p_33434_)) {
         this.setBoundingBox(this.makeBoundingBox());
      }

      super.onSyncedDataUpdated(p_33434_);
   }

   private int getRawPeekAmount() {
      return this.entityData.get(DATA_PEEK_ID);
   }

   void setRawPeekAmount(int p_33419_) {
      if (!this.level().isClientSide()) {
         this.getAttribute(Attributes.ARMOR).removeModifier(COVERED_ARMOR_MODIFIER_ID);
         if (p_33419_ == 0) {
            this.getAttribute(Attributes.ARMOR).addPermanentModifier(COVERED_ARMOR_MODIFIER);
            this.playSound(SoundEvents.SHULKER_CLOSE, 1.0F, 1.0F);
            this.gameEvent(GameEvent.CONTAINER_CLOSE);
         } else {
            this.playSound(SoundEvents.SHULKER_OPEN, 1.0F, 1.0F);
            this.gameEvent(GameEvent.CONTAINER_OPEN);
         }
      }

      this.entityData.set(DATA_PEEK_ID, (byte)p_33419_);
   }

   public float getClientPeekAmount(float p_33481_) {
      return Mth.lerp(p_33481_, this.currentPeekAmountO, this.currentPeekAmount);
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_219067_) {
      super.recreateFromPacket(p_219067_);
      this.yBodyRot = 0.0F;
      this.yBodyRotO = 0.0F;
   }

   @Override
   public int getMaxHeadXRot() {
      return 180;
   }

   @Override
   public int getMaxHeadYRot() {
      return 180;
   }

   @Override
   public void push(Entity p_33474_) {
   }

   public @Nullable Vec3 getRenderPosition(float p_149767_) {
      if (this.clientOldAttachPosition != null && this.clientSideTeleportInterpolation > 0) {
         double d0 = (this.clientSideTeleportInterpolation - p_149767_) / 6.0;
         d0 *= d0;
         d0 *= this.getScale();
         BlockPos blockpos = this.blockPosition();
         double d1 = (blockpos.getX() - this.clientOldAttachPosition.getX()) * d0;
         double d2 = (blockpos.getY() - this.clientOldAttachPosition.getY()) * d0;
         double d3 = (blockpos.getZ() - this.clientOldAttachPosition.getZ()) * d0;
         return new Vec3(-d1, -d2, -d3);
      } else {
         return null;
      }
   }

   @Override
   protected float sanitizeScale(float p_332844_) {
      return Math.min(p_332844_, 3.0F);
   }

   private void setVariant(Optional<DyeColor> p_262609_) {
      this.entityData.set(DATA_COLOR_ID, p_262609_.<Byte>map(p_262566_ -> (byte)p_262566_.getId()).orElse((byte)16));
   }

   public Optional<DyeColor> getVariant() {
      return Optional.ofNullable(this.getColor());
   }

   public @Nullable DyeColor getColor() {
      byte b0 = this.entityData.get(DATA_COLOR_ID);
      return b0 != 16 && b0 <= 15 ? DyeColor.byId(b0) : null;
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_395942_) {
      return p_395942_ == DataComponents.SHULKER_COLOR ? castComponentValue((DataComponentType<T>)p_395942_, this.getColor()) : super.get(p_395942_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_392524_) {
      this.applyImplicitComponentIfPresent(p_392524_, DataComponents.SHULKER_COLOR);
      super.applyImplicitComponents(p_392524_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_393862_, T p_393891_) {
      if (p_393862_ == DataComponents.SHULKER_COLOR) {
         this.setVariant(Optional.of(castComponentValue(DataComponents.SHULKER_COLOR, p_393891_)));
         return true;
      } else {
         return super.applyImplicitComponent(p_393862_, p_393891_);
      }
   }

   class ShulkerAttackGoal extends Goal {
      private int attackTime;

      public ShulkerAttackGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         LivingEntity livingentity = Shulker.this.getTarget();
         return livingentity != null && livingentity.isAlive() ? Shulker.this.level().getDifficulty() != Difficulty.PEACEFUL : false;
      }

      @Override
      public void start() {
         this.attackTime = 20;
         Shulker.this.setRawPeekAmount(100);
      }

      @Override
      public void stop() {
         Shulker.this.setRawPeekAmount(0);
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         if (Shulker.this.level().getDifficulty() != Difficulty.PEACEFUL) {
            this.attackTime--;
            LivingEntity livingentity = Shulker.this.getTarget();
            if (livingentity != null) {
               Shulker.this.getLookControl().setLookAt(livingentity, 180.0F, 180.0F);
               double d0 = Shulker.this.distanceToSqr(livingentity);
               if (d0 < 400.0) {
                  if (this.attackTime <= 0) {
                     this.attackTime = 20 + Shulker.this.random.nextInt(10) * 20 / 2;
                     Shulker.this.level()
                        .addFreshEntity(new ShulkerBullet(Shulker.this.level(), Shulker.this, livingentity, Shulker.this.getAttachFace().getAxis()));
                     Shulker.this.playSound(SoundEvents.SHULKER_SHOOT, 2.0F, (Shulker.this.random.nextFloat() - Shulker.this.random.nextFloat()) * 0.2F + 1.0F);
                  }
               } else {
                  Shulker.this.setTarget(null);
               }

               super.tick();
            }
         }
      }
   }

   static class ShulkerBodyRotationControl extends BodyRotationControl {
      public ShulkerBodyRotationControl(Mob p_149816_) {
         super(p_149816_);
      }

      @Override
      public void clientTick() {
      }
   }

   static class ShulkerDefenseAttackGoal extends NearestAttackableTargetGoal<LivingEntity> {
      public ShulkerDefenseAttackGoal(Shulker p_33496_) {
         super(p_33496_, LivingEntity.class, 10, true, false, (p_33501_, p_367887_) -> p_33501_ instanceof Enemy);
      }

      @Override
      public boolean canUse() {
         return this.mob.getTeam() == null ? false : super.canUse();
      }

      @Override
      protected AABB getTargetSearchArea(double p_33499_) {
         Direction direction = ((Shulker)this.mob).getAttachFace();
         if (direction.getAxis() == Direction.Axis.X) {
            return this.mob.getBoundingBox().inflate(4.0, p_33499_, p_33499_);
         } else {
            return direction.getAxis() == Direction.Axis.Z
               ? this.mob.getBoundingBox().inflate(p_33499_, p_33499_, 4.0)
               : this.mob.getBoundingBox().inflate(p_33499_, 4.0, p_33499_);
         }
      }
   }

   class ShulkerLookControl extends LookControl {
      public ShulkerLookControl(final Mob p_149820_) {
         super(p_149820_);
      }

      @Override
      protected void clampHeadRotationToBody() {
      }

      @Override
      protected Optional<Float> getYRotD() {
         Direction direction = Shulker.this.getAttachFace().getOpposite();
         Vector3f vector3f = direction.getRotation().transform(new Vector3f(Shulker.FORWARD));
         Vec3i vec3i = direction.getUnitVec3i();
         Vector3f vector3f1 = new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ());
         vector3f1.cross(vector3f);
         double d0 = this.wantedX - this.mob.getX();
         double d1 = this.wantedY - this.mob.getEyeY();
         double d2 = this.wantedZ - this.mob.getZ();
         Vector3f vector3f2 = new Vector3f((float)d0, (float)d1, (float)d2);
         float f = vector3f1.dot(vector3f2);
         float f1 = vector3f.dot(vector3f2);
         return !(Math.abs(f) > 1.0E-5F) && !(Math.abs(f1) > 1.0E-5F) ? Optional.empty() : Optional.of((float)(Mth.atan2(-f, f1) * 180.0 / (float) Math.PI));
      }

      @Override
      protected Optional<Float> getXRotD() {
         return Optional.of(0.0F);
      }
   }

   class ShulkerNearestAttackGoal extends NearestAttackableTargetGoal<Player> {
      public ShulkerNearestAttackGoal(final Shulker p_33505_) {
         super(p_33505_, Player.class, true);
      }

      @Override
      public boolean canUse() {
         return Shulker.this.level().getDifficulty() == Difficulty.PEACEFUL ? false : super.canUse();
      }

      @Override
      protected AABB getTargetSearchArea(double p_33508_) {
         Direction direction = ((Shulker)this.mob).getAttachFace();
         if (direction.getAxis() == Direction.Axis.X) {
            return this.mob.getBoundingBox().inflate(4.0, p_33508_, p_33508_);
         } else {
            return direction.getAxis() == Direction.Axis.Z
               ? this.mob.getBoundingBox().inflate(p_33508_, p_33508_, 4.0)
               : this.mob.getBoundingBox().inflate(p_33508_, 4.0, p_33508_);
         }
      }
   }

   class ShulkerPeekGoal extends Goal {
      private int peekTime;

      @Override
      public boolean canUse() {
         return Shulker.this.getTarget() == null
            && Shulker.this.random.nextInt(reducedTickDelay(40)) == 0
            && Shulker.this.canStayAt(Shulker.this.blockPosition(), Shulker.this.getAttachFace());
      }

      @Override
      public boolean canContinueToUse() {
         return Shulker.this.getTarget() == null && this.peekTime > 0;
      }

      @Override
      public void start() {
         this.peekTime = this.adjustedTickDelay(20 * (1 + Shulker.this.random.nextInt(3)));
         Shulker.this.setRawPeekAmount(30);
      }

      @Override
      public void stop() {
         if (Shulker.this.getTarget() == null) {
            Shulker.this.setRawPeekAmount(0);
         }
      }

      @Override
      public void tick() {
         this.peekTime--;
      }
   }
}
