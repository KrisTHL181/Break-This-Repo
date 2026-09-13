package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Ghast extends Mob implements Enemy {
   private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING = SynchedEntityData.defineId(Ghast.class, EntityDataSerializers.BOOLEAN);
   private static final byte DEFAULT_EXPLOSION_POWER = 1;
   private int explosionPower = 1;

   public Ghast(EntityType<? extends Ghast> p_32725_, Level p_32726_) {
      super(p_32725_, p_32726_);
      this.xpReward = 5;
      this.moveControl = new Ghast.GhastMoveControl(this, false, () -> false);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(5, new Ghast.RandomFloatAroundGoal(this));
      this.goalSelector.addGoal(7, new Ghast.GhastLookGoal(this));
      this.goalSelector.addGoal(7, new Ghast.GhastShootFireballGoal(this));
      this.targetSelector
         .addGoal(
            1,
            new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (p_449681_, p_449682_) -> Math.abs(p_449681_.getY() - this.getY()) <= 4.0)
         );
   }

   public boolean isCharging() {
      return this.entityData.get(DATA_IS_CHARGING);
   }

   public void setCharging(boolean p_32759_) {
      this.entityData.set(DATA_IS_CHARGING, p_32759_);
   }

   public int getExplosionPower() {
      return this.explosionPower;
   }

   private static boolean isReflectedFireball(DamageSource p_238408_) {
      return p_238408_.getDirectEntity() instanceof LargeFireball && p_238408_.getEntity() instanceof Player;
   }

   @Override
   public boolean isInvulnerableTo(ServerLevel p_363213_, DamageSource p_238289_) {
      return this.isInvulnerable() && !p_238289_.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || !isReflectedFireball(p_238289_) && super.isInvulnerableTo(p_363213_, p_238289_);
   }

   @Override
   protected void checkFallDamage(double p_410052_, boolean p_410300_, BlockState p_407336_, BlockPos p_409059_) {
   }

   @Override
   public boolean onClimbable() {
      return false;
   }

   @Override
   public void travel(Vec3 p_407013_) {
      this.travelFlying(p_407013_, 0.02F);
   }

   @Override
   public boolean hurtServer(ServerLevel p_365264_, DamageSource p_366880_, float p_369426_) {
      if (isReflectedFireball(p_366880_)) {
         super.hurtServer(p_365264_, p_366880_, 1000.0F);
         return true;
      } else {
         return this.isInvulnerableTo(p_365264_, p_366880_) ? false : super.hurtServer(p_365264_, p_366880_, p_369426_);
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_334321_) {
      super.defineSynchedData(p_334321_);
      p_334321_.define(DATA_IS_CHARGING, false);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 10.0)
         .add(Attributes.FOLLOW_RANGE, 100.0)
         .add(Attributes.CAMERA_DISTANCE, 8.0)
         .add(Attributes.FLYING_SPEED, 0.06);
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.GHAST_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_32750_) {
      return SoundEvents.GHAST_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.GHAST_DEATH;
   }

   @Override
   protected float getSoundVolume() {
      return 5.0F;
   }

   public static boolean checkGhastSpawnRules(
      EntityType<Ghast> p_218985_, LevelAccessor p_218986_, EntitySpawnReason p_366739_, BlockPos p_218988_, RandomSource p_218989_
   ) {
      return p_218986_.getDifficulty() != Difficulty.PEACEFUL
         && p_218989_.nextInt(20) == 0
         && checkMobSpawnRules(p_218985_, p_218986_, p_366739_, p_218988_, p_218989_);
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 1;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_407589_) {
      super.addAdditionalSaveData(p_407589_);
      p_407589_.putByte("ExplosionPower", (byte)this.explosionPower);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_409268_) {
      super.readAdditionalSaveData(p_409268_);
      this.explosionPower = p_409268_.getByteOr("ExplosionPower", (byte)1);
   }

   @Override
   public boolean supportQuadLeashAsHolder() {
      return true;
   }

   @Override
   public double leashElasticDistance() {
      return 10.0;
   }

   @Override
   public double leashSnapDistance() {
      return 16.0;
   }

   public static void faceMovementDirection(Mob p_407227_) {
      if (p_407227_.getTarget() == null) {
         Vec3 vec3 = p_407227_.getDeltaMovement();
         p_407227_.setYRot(-((float)Mth.atan2(vec3.x, vec3.z)) * (180.0F / (float)Math.PI));
         p_407227_.yBodyRot = p_407227_.getYRot();
      } else {
         LivingEntity livingentity = p_407227_.getTarget();
         double d0 = 64.0;
         if (livingentity.distanceToSqr(p_407227_) < 4096.0) {
            double d1 = livingentity.getX() - p_407227_.getX();
            double d2 = livingentity.getZ() - p_407227_.getZ();
            p_407227_.setYRot(-((float)Mth.atan2(d1, d2)) * (180.0F / (float)Math.PI));
            p_407227_.yBodyRot = p_407227_.getYRot();
         }
      }
   }

   public static class GhastLookGoal extends Goal {
      private final Mob ghast;

      public GhastLookGoal(Mob p_410360_) {
         this.ghast = p_410360_;
         this.setFlags(EnumSet.of(Goal.Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         return true;
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         Ghast.faceMovementDirection(this.ghast);
      }
   }

   public static class GhastMoveControl extends MoveControl {
      private final Mob ghast;
      private int floatDuration;
      private final boolean careful;
      private final BooleanSupplier shouldBeStopped;

      public GhastMoveControl(Mob p_410451_, boolean p_409437_, BooleanSupplier p_409135_) {
         super(p_410451_);
         this.ghast = p_410451_;
         this.careful = p_409437_;
         this.shouldBeStopped = p_409135_;
      }

      @Override
      public void tick() {
         if (this.shouldBeStopped.getAsBoolean()) {
            this.operation = MoveControl.Operation.WAIT;
            this.ghast.stopInPlace();
         }

         if (this.operation == MoveControl.Operation.MOVE_TO && this.floatDuration-- <= 0) {
            this.floatDuration = this.floatDuration + this.ghast.getRandom().nextInt(5) + 2;
            Vec3 vec3 = new Vec3(this.wantedX - this.ghast.getX(), this.wantedY - this.ghast.getY(), this.wantedZ - this.ghast.getZ());
            if (this.canReach(vec3)) {
               this.ghast
                  .setDeltaMovement(
                     this.ghast.getDeltaMovement().add(vec3.normalize().scale(this.ghast.getAttributeValue(Attributes.FLYING_SPEED) * 5.0 / 3.0))
                  );
            } else {
               this.operation = MoveControl.Operation.WAIT;
            }
         }
      }

      private boolean canReach(Vec3 p_32771_) {
         AABB aabb = this.ghast.getBoundingBox();
         AABB aabb1 = aabb.move(p_32771_);
         if (this.careful) {
            for (BlockPos blockpos : BlockPos.betweenClosed(aabb1.inflate(1.0))) {
               if (!this.blockTraversalPossible(this.ghast.level(), null, null, blockpos, false, false)) {
                  return false;
               }
            }
         }

         boolean flag = this.ghast.isInWater();
         boolean flag1 = this.ghast.isInLava();
         Vec3 vec3 = this.ghast.position();
         Vec3 vec31 = vec3.add(p_32771_);
         return BlockGetter.forEachBlockIntersectedBetween(
            vec3,
            vec31,
            aabb1,
            (p_449688_, p_449689_) -> aabb.intersects(p_449688_) ? true : this.blockTraversalPossible(this.ghast.level(), vec3, vec31, p_449688_, flag, flag1)
         );
      }

      private boolean blockTraversalPossible(
         BlockGetter p_408906_, @Nullable Vec3 p_409930_, @Nullable Vec3 p_410665_, BlockPos p_406655_, boolean p_410169_, boolean p_408341_
      ) {
         BlockState blockstate = p_408906_.getBlockState(p_406655_);
         if (blockstate.isAir()) {
            return true;
         }

         boolean flag = p_409930_ != null && p_410665_ != null;
         boolean flag1 = flag
            ? !this.ghast
               .collidedWithShapeMovingFrom(p_409930_, p_410665_, blockstate.getCollisionShape(p_408906_, p_406655_).move(new Vec3(p_406655_)).toAabbs())
            : blockstate.getCollisionShape(p_408906_, p_406655_).isEmpty();
         if (!this.careful) {
            return flag1;
         }

         if (blockstate.is(BlockTags.HAPPY_GHAST_AVOIDS)) {
            return false;
         }

         FluidState fluidstate = p_408906_.getFluidState(p_406655_);
         if (!fluidstate.isEmpty() && (!flag || this.ghast.collidedWithFluid(fluidstate, p_406655_, p_409930_, p_410665_))) {
            if (fluidstate.is(FluidTags.WATER)) {
               return p_410169_;
            }

            if (fluidstate.is(FluidTags.LAVA)) {
               return p_408341_;
            }
         }

         return flag1;
      }
   }

   static class GhastShootFireballGoal extends Goal {
      private final Ghast ghast;
      public int chargeTime;

      public GhastShootFireballGoal(Ghast p_32776_) {
         this.ghast = p_32776_;
      }

      @Override
      public boolean canUse() {
         return this.ghast.getTarget() != null;
      }

      @Override
      public void start() {
         this.chargeTime = 0;
      }

      @Override
      public void stop() {
         this.ghast.setCharging(false);
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         LivingEntity livingentity = this.ghast.getTarget();
         if (livingentity != null) {
            double d0 = 64.0;
            if (livingentity.distanceToSqr(this.ghast) < 4096.0 && this.ghast.hasLineOfSight(livingentity)) {
               Level level = this.ghast.level();
               this.chargeTime++;
               if (this.chargeTime == 10 && !this.ghast.isSilent()) {
                  level.levelEvent(null, 1015, this.ghast.blockPosition(), 0);
               }

               if (this.chargeTime == 20) {
                  double d1 = 4.0;
                  Vec3 vec3 = this.ghast.getViewVector(1.0F);
                  double d2 = livingentity.getX() - (this.ghast.getX() + vec3.x * 4.0);
                  double d3 = livingentity.getY(0.5) - (0.5 + this.ghast.getY(0.5));
                  double d4 = livingentity.getZ() - (this.ghast.getZ() + vec3.z * 4.0);
                  Vec3 vec31 = new Vec3(d2, d3, d4);
                  if (!this.ghast.isSilent()) {
                     level.levelEvent(null, 1016, this.ghast.blockPosition(), 0);
                  }

                  LargeFireball largefireball = new LargeFireball(level, this.ghast, vec31.normalize(), this.ghast.getExplosionPower());
                  largefireball.setPos(this.ghast.getX() + vec3.x * 4.0, this.ghast.getY(0.5) + 0.5, largefireball.getZ() + vec3.z * 4.0);
                  level.addFreshEntity(largefireball);
                  this.chargeTime = -40;
               }
            } else if (this.chargeTime > 0) {
               this.chargeTime--;
            }

            this.ghast.setCharging(this.chargeTime > 10);
         }
      }
   }

   public static class RandomFloatAroundGoal extends Goal {
      private static final int MAX_ATTEMPTS = 64;
      private final Mob ghast;
      private final int distanceToBlocks;

      public RandomFloatAroundGoal(Mob p_410331_) {
         this(p_410331_, 0);
      }

      public RandomFloatAroundGoal(Mob p_408161_, int p_409935_) {
         this.ghast = p_408161_;
         this.distanceToBlocks = p_409935_;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         MoveControl movecontrol = this.ghast.getMoveControl();
         if (!movecontrol.hasWanted()) {
            return true;
         }

         double d0 = movecontrol.getWantedX() - this.ghast.getX();
         double d1 = movecontrol.getWantedY() - this.ghast.getY();
         double d2 = movecontrol.getWantedZ() - this.ghast.getZ();
         double d3 = d0 * d0 + d1 * d1 + d2 * d2;
         return d3 < 1.0 || d3 > 3600.0;
      }

      @Override
      public boolean canContinueToUse() {
         return false;
      }

      @Override
      public void start() {
         Vec3 vec3 = getSuitableFlyToPosition(this.ghast, this.distanceToBlocks);
         this.ghast.getMoveControl().setWantedPosition(vec3.x(), vec3.y(), vec3.z(), 1.0);
      }

      public static Vec3 getSuitableFlyToPosition(Mob p_409499_, int p_410248_) {
         Level level = p_409499_.level();
         RandomSource randomsource = p_409499_.getRandom();
         Vec3 vec3 = p_409499_.position();
         Vec3 vec31 = null;

         for (int i = 0; i < 64; i++) {
            vec31 = chooseRandomPositionWithRestriction(p_409499_, vec3, randomsource);
            if (vec31 != null && isGoodTarget(level, vec31, p_410248_)) {
               return vec31;
            }
         }

         if (vec31 == null) {
            vec31 = chooseRandomPosition(vec3, randomsource);
         }

         BlockPos blockpos = BlockPos.containing(vec31);
         int j = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockpos.getX(), blockpos.getZ());
         if (j < blockpos.getY() && j > level.getMinY()) {
            vec31 = new Vec3(vec31.x(), p_409499_.getY() - Math.abs(p_409499_.getY() - vec31.y()), vec31.z());
         }

         return vec31;
      }

      private static boolean isGoodTarget(Level p_406666_, Vec3 p_408132_, int p_410376_) {
         if (p_410376_ <= 0) {
            return true;
         }

         BlockPos blockpos = BlockPos.containing(p_408132_);
         if (!p_406666_.getBlockState(blockpos).isAir()) {
            return false;
         }

         for (Direction direction : Direction.values()) {
            for (int i = 1; i < p_410376_; i++) {
               BlockPos blockpos1 = blockpos.relative(direction, i);
               if (!p_406666_.getBlockState(blockpos1).isAir()) {
                  return true;
               }
            }
         }

         return false;
      }

      private static Vec3 chooseRandomPosition(Vec3 p_406802_, RandomSource p_408060_) {
         double d0 = p_406802_.x() + (p_408060_.nextFloat() * 2.0F - 1.0F) * 16.0F;
         double d1 = p_406802_.y() + (p_408060_.nextFloat() * 2.0F - 1.0F) * 16.0F;
         double d2 = p_406802_.z() + (p_408060_.nextFloat() * 2.0F - 1.0F) * 16.0F;
         return new Vec3(d0, d1, d2);
      }

      private static @Nullable Vec3 chooseRandomPositionWithRestriction(Mob p_410298_, Vec3 p_408608_, RandomSource p_406675_) {
         Vec3 vec3 = chooseRandomPosition(p_408608_, p_406675_);
         return p_410298_.hasHome() && !p_410298_.isWithinHome(vec3) ? null : vec3;
      }
   }
}
