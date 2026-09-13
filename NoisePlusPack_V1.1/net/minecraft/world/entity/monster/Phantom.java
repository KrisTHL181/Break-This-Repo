package net.minecraft.world.entity.monster;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Phantom extends Mob implements Enemy {
   public static final float FLAP_DEGREES_PER_TICK = 7.448451F;
   public static final int TICKS_PER_FLAP = Mth.ceil(24.166098F);
   private static final EntityDataAccessor<Integer> ID_SIZE = SynchedEntityData.defineId(Phantom.class, EntityDataSerializers.INT);
   Vec3 moveTargetPoint = Vec3.ZERO;
   @Nullable BlockPos anchorPoint;
   Phantom.AttackPhase attackPhase = Phantom.AttackPhase.CIRCLE;

   public Phantom(EntityType<? extends Phantom> p_33101_, Level p_33102_) {
      super(p_33101_, p_33102_);
      this.xpReward = 5;
      this.moveControl = new Phantom.PhantomMoveControl(this);
      this.lookControl = new Phantom.PhantomLookControl(this);
   }

   @Override
   public boolean isFlapping() {
      return (this.getUniqueFlapTickOffset() + this.tickCount) % TICKS_PER_FLAP == 0;
   }

   @Override
   protected BodyRotationControl createBodyControl() {
      return new Phantom.PhantomBodyRotationControl(this);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new Phantom.PhantomAttackStrategyGoal());
      this.goalSelector.addGoal(2, new Phantom.PhantomSweepAttackGoal());
      this.goalSelector.addGoal(3, new Phantom.PhantomCircleAroundAnchorGoal());
      this.targetSelector.addGoal(1, new Phantom.PhantomAttackPlayerTargetGoal());
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_331815_) {
      super.defineSynchedData(p_331815_);
      p_331815_.define(ID_SIZE, 0);
   }

   public void setPhantomSize(int p_33109_) {
      this.entityData.set(ID_SIZE, Mth.clamp(p_33109_, 0, 64));
   }

   private void updatePhantomSizeInfo() {
      this.refreshDimensions();
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6 + this.getPhantomSize());
   }

   public int getPhantomSize() {
      return this.entityData.get(ID_SIZE);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_33134_) {
      if (ID_SIZE.equals(p_33134_)) {
         this.updatePhantomSizeInfo();
      }

      super.onSyncedDataUpdated(p_33134_);
   }

   public int getUniqueFlapTickOffset() {
      return this.getId() * 3;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         float f = Mth.cos((this.getUniqueFlapTickOffset() + this.tickCount) * 7.448451F * (float) (Math.PI / 180.0) + (float) Math.PI);
         float f1 = Mth.cos((this.getUniqueFlapTickOffset() + this.tickCount + 1) * 7.448451F * (float) (Math.PI / 180.0) + (float) Math.PI);
         if (f > 0.0F && f1 <= 0.0F) {
            this.level()
               .playLocalSound(
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.PHANTOM_FLAP,
                  this.getSoundSource(),
                  0.95F + this.random.nextFloat() * 0.05F,
                  0.95F + this.random.nextFloat() * 0.05F,
                  false
               );
         }

         float f2 = this.getBbWidth() * 1.48F;
         float f3 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * f2;
         float f4 = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)) * f2;
         float f5 = (0.3F + f * 0.45F) * this.getBbHeight() * 2.5F;
         this.level().addParticle(ParticleTypes.MYCELIUM, this.getX() + f3, this.getY() + f5, this.getZ() + f4, 0.0, 0.0, 0.0);
         this.level().addParticle(ParticleTypes.MYCELIUM, this.getX() - f3, this.getY() + f5, this.getZ() - f4, 0.0, 0.0, 0.0);
      }
   }

   @Override
   protected void checkFallDamage(double p_406874_, boolean p_406466_, BlockState p_407199_, BlockPos p_407005_) {
   }

   @Override
   public boolean onClimbable() {
      return false;
   }

   @Override
   public void travel(Vec3 p_406303_) {
      this.travelFlying(p_406303_, 0.2F);
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_33126_, DifficultyInstance p_33127_, EntitySpawnReason p_368653_, @Nullable SpawnGroupData p_33129_
   ) {
      this.anchorPoint = this.blockPosition().above(5);
      this.setPhantomSize(0);
      return super.finalizeSpawn(p_33126_, p_33127_, p_368653_, p_33129_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_406172_) {
      super.readAdditionalSaveData(p_406172_);
      this.anchorPoint = p_406172_.<BlockPos>read("anchor_pos", BlockPos.CODEC).orElse(null);
      this.setPhantomSize(p_406172_.getIntOr("size", 0));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_408922_) {
      super.addAdditionalSaveData(p_408922_);
      p_408922_.storeNullable("anchor_pos", BlockPos.CODEC, this.anchorPoint);
      p_408922_.putInt("size", this.getPhantomSize());
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double p_33107_) {
      return true;
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.PHANTOM_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_33152_) {
      return SoundEvents.PHANTOM_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.PHANTOM_DEATH;
   }

   @Override
   protected float getSoundVolume() {
      return 1.0F;
   }

   @Override
   public boolean canAttackType(EntityType<?> p_33111_) {
      return true;
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_333771_) {
      int i = this.getPhantomSize();
      EntityDimensions entitydimensions = super.getDefaultDimensions(p_333771_);
      return entitydimensions.scale(1.0F + 0.15F * i);
   }

   boolean canAttack(ServerLevel p_365188_, LivingEntity p_367013_, TargetingConditions p_364315_) {
      return p_364315_.test(p_365188_, this, p_367013_);
   }

   enum AttackPhase {
      CIRCLE,
      SWOOP;
   }

   class PhantomAttackPlayerTargetGoal extends Goal {
      private final TargetingConditions attackTargeting = TargetingConditions.forCombat().range(64.0);
      private int nextScanTick = reducedTickDelay(20);

      @Override
      public boolean canUse() {
         if (this.nextScanTick > 0) {
            this.nextScanTick--;
            return false;
         }

         this.nextScanTick = reducedTickDelay(60);
         ServerLevel serverlevel = getServerLevel(Phantom.this.level());
         List<Player> list = serverlevel.getNearbyPlayers(this.attackTargeting, Phantom.this, Phantom.this.getBoundingBox().inflate(16.0, 64.0, 16.0));
         if (!list.isEmpty()) {
            list.sort(Comparator.<Player, Double>comparing(Entity::getY).reversed());

            for (Player player : list) {
               if (Phantom.this.canAttack(serverlevel, player, TargetingConditions.DEFAULT)) {
                  Phantom.this.setTarget(player);
                  return true;
               }
            }
         }

         return false;
      }

      @Override
      public boolean canContinueToUse() {
         LivingEntity livingentity = Phantom.this.getTarget();
         return livingentity != null ? Phantom.this.canAttack(getServerLevel(Phantom.this.level()), livingentity, TargetingConditions.DEFAULT) : false;
      }
   }

   class PhantomAttackStrategyGoal extends Goal {
      private int nextSweepTick;

      @Override
      public boolean canUse() {
         LivingEntity livingentity = Phantom.this.getTarget();
         return livingentity != null ? Phantom.this.canAttack(getServerLevel(Phantom.this.level()), livingentity, TargetingConditions.DEFAULT) : false;
      }

      @Override
      public void start() {
         this.nextSweepTick = this.adjustedTickDelay(10);
         Phantom.this.attackPhase = Phantom.AttackPhase.CIRCLE;
         this.setAnchorAboveTarget();
      }

      @Override
      public void stop() {
         if (Phantom.this.anchorPoint != null) {
            Phantom.this.anchorPoint = Phantom.this.level()
               .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, Phantom.this.anchorPoint)
               .above(10 + Phantom.this.random.nextInt(20));
         }
      }

      @Override
      public void tick() {
         if (Phantom.this.attackPhase == Phantom.AttackPhase.CIRCLE) {
            this.nextSweepTick--;
            if (this.nextSweepTick <= 0) {
               Phantom.this.attackPhase = Phantom.AttackPhase.SWOOP;
               this.setAnchorAboveTarget();
               this.nextSweepTick = this.adjustedTickDelay((8 + Phantom.this.random.nextInt(4)) * 20);
               Phantom.this.playSound(SoundEvents.PHANTOM_SWOOP, 10.0F, 0.95F + Phantom.this.random.nextFloat() * 0.1F);
            }
         }
      }

      private void setAnchorAboveTarget() {
         if (Phantom.this.anchorPoint != null) {
            Phantom.this.anchorPoint = Phantom.this.getTarget().blockPosition().above(20 + Phantom.this.random.nextInt(20));
            if (Phantom.this.anchorPoint.getY() < Phantom.this.level().getSeaLevel()) {
               Phantom.this.anchorPoint = new BlockPos(Phantom.this.anchorPoint.getX(), Phantom.this.level().getSeaLevel() + 1, Phantom.this.anchorPoint.getZ());
            }
         }
      }
   }

   class PhantomBodyRotationControl extends BodyRotationControl {
      public PhantomBodyRotationControl(final Mob p_33216_) {
         super(p_33216_);
      }

      @Override
      public void clientTick() {
         Phantom.this.yHeadRot = Phantom.this.yBodyRot;
         Phantom.this.yBodyRot = Phantom.this.getYRot();
      }
   }

   class PhantomCircleAroundAnchorGoal extends Phantom.PhantomMoveTargetGoal {
      private float angle;
      private float distance;
      private float height;
      private float clockwise;

      @Override
      public boolean canUse() {
         return Phantom.this.getTarget() == null || Phantom.this.attackPhase == Phantom.AttackPhase.CIRCLE;
      }

      @Override
      public void start() {
         this.distance = 5.0F + Phantom.this.random.nextFloat() * 10.0F;
         this.height = -4.0F + Phantom.this.random.nextFloat() * 9.0F;
         this.clockwise = Phantom.this.random.nextBoolean() ? 1.0F : -1.0F;
         this.selectNext();
      }

      @Override
      public void tick() {
         if (Phantom.this.random.nextInt(this.adjustedTickDelay(350)) == 0) {
            this.height = -4.0F + Phantom.this.random.nextFloat() * 9.0F;
         }

         if (Phantom.this.random.nextInt(this.adjustedTickDelay(250)) == 0) {
            this.distance++;
            if (this.distance > 15.0F) {
               this.distance = 5.0F;
               this.clockwise = -this.clockwise;
            }
         }

         if (Phantom.this.random.nextInt(this.adjustedTickDelay(450)) == 0) {
            this.angle = Phantom.this.random.nextFloat() * 2.0F * (float) Math.PI;
            this.selectNext();
         }

         if (this.touchingTarget()) {
            this.selectNext();
         }

         if (Phantom.this.moveTargetPoint.y < Phantom.this.getY() && !Phantom.this.level().isEmptyBlock(Phantom.this.blockPosition().below(1))) {
            this.height = Math.max(1.0F, this.height);
            this.selectNext();
         }

         if (Phantom.this.moveTargetPoint.y > Phantom.this.getY() && !Phantom.this.level().isEmptyBlock(Phantom.this.blockPosition().above(1))) {
            this.height = Math.min(-1.0F, this.height);
            this.selectNext();
         }
      }

      private void selectNext() {
         if (Phantom.this.anchorPoint == null) {
            Phantom.this.anchorPoint = Phantom.this.blockPosition();
         }

         this.angle = this.angle + this.clockwise * 15.0F * (float) (Math.PI / 180.0);
         Phantom.this.moveTargetPoint = Vec3.atLowerCornerOf(Phantom.this.anchorPoint)
            .add(this.distance * Mth.cos(this.angle), -4.0F + this.height, this.distance * Mth.sin(this.angle));
      }
   }

   static class PhantomLookControl extends LookControl {
      public PhantomLookControl(Mob p_33235_) {
         super(p_33235_);
      }

      @Override
      public void tick() {
      }
   }

   class PhantomMoveControl extends MoveControl {
      private float speed = 0.1F;

      public PhantomMoveControl(final Mob p_33241_) {
         super(p_33241_);
      }

      @Override
      public void tick() {
         if (Phantom.this.horizontalCollision) {
            Phantom.this.setYRot(Phantom.this.getYRot() + 180.0F);
            this.speed = 0.1F;
         }

         double d0 = Phantom.this.moveTargetPoint.x - Phantom.this.getX();
         double d1 = Phantom.this.moveTargetPoint.y - Phantom.this.getY();
         double d2 = Phantom.this.moveTargetPoint.z - Phantom.this.getZ();
         double d3 = Math.sqrt(d0 * d0 + d2 * d2);
         if (Math.abs(d3) > 1.0E-5F) {
            double d4 = 1.0 - Math.abs(d1 * 0.7F) / d3;
            d0 *= d4;
            d2 *= d4;
            d3 = Math.sqrt(d0 * d0 + d2 * d2);
            double d5 = Math.sqrt(d0 * d0 + d2 * d2 + d1 * d1);
            float f = Phantom.this.getYRot();
            float f1 = (float)Mth.atan2(d2, d0);
            float f2 = Mth.wrapDegrees(Phantom.this.getYRot() + 90.0F);
            float f3 = Mth.wrapDegrees(f1 * (180.0F / (float)Math.PI));
            Phantom.this.setYRot(Mth.approachDegrees(f2, f3, 4.0F) - 90.0F);
            Phantom.this.yBodyRot = Phantom.this.getYRot();
            if (Mth.degreesDifferenceAbs(f, Phantom.this.getYRot()) < 3.0F) {
               this.speed = Mth.approach(this.speed, 1.8F, 0.005F * (1.8F / this.speed));
            } else {
               this.speed = Mth.approach(this.speed, 0.2F, 0.025F);
            }

            float f4 = (float)(-(Mth.atan2(-d1, d3) * 180.0 / (float) Math.PI));
            Phantom.this.setXRot(f4);
            float f5 = Phantom.this.getYRot() + 90.0F;
            double d6 = this.speed * Mth.cos(f5 * (float) (Math.PI / 180.0)) * Math.abs(d0 / d5);
            double d7 = this.speed * Mth.sin(f5 * (float) (Math.PI / 180.0)) * Math.abs(d2 / d5);
            double d8 = this.speed * Mth.sin(f4 * (float) (Math.PI / 180.0)) * Math.abs(d1 / d5);
            Vec3 vec3 = Phantom.this.getDeltaMovement();
            Phantom.this.setDeltaMovement(vec3.add(new Vec3(d6, d8, d7).subtract(vec3).scale(0.2)));
         }
      }
   }

   abstract class PhantomMoveTargetGoal extends Goal {
      public PhantomMoveTargetGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      protected boolean touchingTarget() {
         return Phantom.this.moveTargetPoint.distanceToSqr(Phantom.this.getX(), Phantom.this.getY(), Phantom.this.getZ()) < 4.0;
      }
   }

   class PhantomSweepAttackGoal extends Phantom.PhantomMoveTargetGoal {
      private static final int CAT_SEARCH_TICK_DELAY = 20;
      private boolean isScaredOfCat;
      private int catSearchTick;

      @Override
      public boolean canUse() {
         return Phantom.this.getTarget() != null && Phantom.this.attackPhase == Phantom.AttackPhase.SWOOP;
      }

      @Override
      public boolean canContinueToUse() {
         LivingEntity livingentity = Phantom.this.getTarget();
         if (livingentity == null) {
            return false;
         } else if (!livingentity.isAlive()) {
            return false;
         } else if (livingentity instanceof Player player && (livingentity.isSpectator() || player.isCreative())) {
            return false;
         } else {
            if (!this.canUse()) {
               return false;
            }

            if (Phantom.this.tickCount > this.catSearchTick) {
               this.catSearchTick = Phantom.this.tickCount + 20;
               List<Cat> list = Phantom.this.level()
                  .getEntitiesOfClass(Cat.class, Phantom.this.getBoundingBox().inflate(16.0), EntitySelector.ENTITY_STILL_ALIVE);

               for (Cat cat : list) {
                  cat.hiss();
               }

               this.isScaredOfCat = !list.isEmpty();
            }

            return !this.isScaredOfCat;
         }
      }

      @Override
      public void start() {
      }

      @Override
      public void stop() {
         Phantom.this.setTarget(null);
         Phantom.this.attackPhase = Phantom.AttackPhase.CIRCLE;
      }

      @Override
      public void tick() {
         LivingEntity livingentity = Phantom.this.getTarget();
         if (livingentity != null) {
            Phantom.this.moveTargetPoint = new Vec3(livingentity.getX(), livingentity.getY(0.5), livingentity.getZ());
            if (Phantom.this.getBoundingBox().inflate(0.2F).intersects(livingentity.getBoundingBox())) {
               Phantom.this.doHurtTarget(getServerLevel(Phantom.this.level()), livingentity);
               Phantom.this.attackPhase = Phantom.AttackPhase.CIRCLE;
               if (!Phantom.this.isSilent()) {
                  Phantom.this.level().levelEvent(1039, Phantom.this.blockPosition(), 0);
               }
            } else if (Phantom.this.horizontalCollision || Phantom.this.hurtTime > 0) {
               Phantom.this.attackPhase = Phantom.AttackPhase.CIRCLE;
            }
         }
      }
   }
}
