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

   public Ghast(final EntityType<? extends Ghast> type, final Level level) {
      super(type, level);
      this.xpReward = 5;
      this.moveControl = new Ghast.GhastMoveControl<>(this, false, () -> false);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(5, new Ghast.RandomFloatAroundGoal(this));
      this.goalSelector.addGoal(7, new Ghast.GhastLookGoal(this));
      this.goalSelector.addGoal(7, new Ghast.GhastShootFireballGoal(this));
      this.targetSelector
         .addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (target, level) -> Math.abs(target.getY() - this.getY()) <= 4.0));
   }

   public boolean isCharging() {
      return this.entityData.get(DATA_IS_CHARGING);
   }

   public void setCharging(final boolean onOff) {
      this.entityData.set(DATA_IS_CHARGING, onOff);
   }

   public int getExplosionPower() {
      return this.explosionPower;
   }

   private static boolean isReflectedFireball(final DamageSource source) {
      return source.getDirectEntity() instanceof LargeFireball && source.getEntity() instanceof Player;
   }

   @Override
   public boolean isInvulnerableTo(final ServerLevel level, final DamageSource source) {
      return this.isInvulnerable() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || !isReflectedFireball(source) && super.isInvulnerableTo(level, source);
   }

   @Override
   protected void checkFallDamage(final double ya, final boolean onGround, final BlockState onState, final BlockPos pos) {
   }

   @Override
   public boolean onClimbable() {
      return false;
   }

   @Override
   public void travel(final Vec3 input) {
      this.travelFlying(input, 0.02F);
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      if (isReflectedFireball(source)) {
         super.hurtServer(level, source, 1000.0F);
         return true;
      } else {
         return this.isInvulnerableTo(level, source) ? false : super.hurtServer(level, source, damage);
      }
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_IS_CHARGING, false);
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
   protected SoundEvent getHurtSound(final DamageSource source) {
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
      final EntityType<Ghast> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      return level.getDifficulty() != Difficulty.PEACEFUL && random.nextInt(20) == 0 && checkMobSpawnRules(type, level, spawnReason, pos, random);
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 1;
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putByte("ExplosionPower", (byte)this.explosionPower);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.explosionPower = input.getByteOr("ExplosionPower", (byte)1);
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

   public static void faceMovementDirection(final Mob ghast) {
      if (ghast.getTarget() == null) {
         Vec3 movement = ghast.getDeltaMovement();
         ghast.setYRot(-((float)Mth.atan2(movement.x, movement.z)) * (180.0F / (float)Math.PI));
         ghast.yBodyRot = ghast.getYRot();
      } else {
         LivingEntity target = ghast.getTarget();
         double maxDist = 64.0;
         if (target.distanceToSqr(ghast) < 4096.0) {
            double xdd = target.getX() - ghast.getX();
            double zdd = target.getZ() - ghast.getZ();
            ghast.setYRot(-((float)Mth.atan2(xdd, zdd)) * (180.0F / (float)Math.PI));
            ghast.yBodyRot = ghast.getYRot();
         }
      }
   }

   public static class GhastLookGoal extends Goal {
      private final Mob ghast;

      public GhastLookGoal(final Mob ghast) {
         this.ghast = ghast;
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

   public static class GhastMoveControl<T extends Mob> extends MoveControl<T> {
      private int floatDuration;
      private final boolean careful;
      private final BooleanSupplier shouldBeStopped;

      public GhastMoveControl(final T ghast, final boolean careful, final BooleanSupplier shouldBeStopped) {
         super(ghast);
         this.careful = careful;
         this.shouldBeStopped = shouldBeStopped;
      }

      @Override
      public void tick() {
         if (this.shouldBeStopped.getAsBoolean()) {
            this.operation = MoveControl.Operation.WAIT;
            this.mob.stopInPlace();
         }

         if (this.operation == MoveControl.Operation.MOVE_TO) {
            if (this.floatDuration-- <= 0) {
               this.floatDuration = this.floatDuration + this.mob.getRandom().nextInt(5) + 2;
               Vec3 travel = new Vec3(this.wantedX - this.mob.getX(), this.wantedY - this.mob.getY(), this.wantedZ - this.mob.getZ());
               if (this.canReach(travel)) {
                  this.mob
                     .setDeltaMovement(
                        this.mob.getDeltaMovement().add(travel.normalize().scale(this.mob.getAttributeValue(Attributes.FLYING_SPEED) * 5.0 / 3.0))
                     );
               } else {
                  this.operation = MoveControl.Operation.WAIT;
               }
            }
         }
      }

      private boolean canReach(final Vec3 travel) {
         AABB aabb = this.mob.getBoundingBox();
         AABB aabbAtDestination = aabb.move(travel);
         if (this.careful) {
            for (BlockPos pos : BlockPos.betweenClosed(aabbAtDestination.inflate(1.0))) {
               if (!this.blockTraversalPossible(this.mob.level(), null, null, pos, false, false)) {
                  return false;
               }
            }
         }

         boolean isInWater = this.mob.isInWater();
         boolean isInLava = this.mob.isInLava();
         Vec3 start = this.mob.position();
         Vec3 end = start.add(travel);
         return BlockGetter.forEachBlockIntersectedBetween(
            start,
            end,
            aabbAtDestination,
            (blockPos, i) -> aabb.intersects(blockPos) ? true : this.blockTraversalPossible(this.mob.level(), start, end, blockPos, isInWater, isInLava)
         );
      }

      private boolean blockTraversalPossible(
         final BlockGetter level,
         final @Nullable Vec3 start,
         final @Nullable Vec3 end,
         final BlockPos pos,
         final boolean canPathThroughWater,
         final boolean canPathThroughLava
      ) {
         BlockState state = level.getBlockState(pos);
         if (state.isAir()) {
            return true;
         }

         boolean preciseBlockCollisions = start != null && end != null;
         boolean pathNoCollisions = preciseBlockCollisions
            ? !this.mob.collidedWithShapeMovingFrom(start, end, state.getCollisionShape(level, pos).move(new Vec3(pos)).toAabbs())
            : state.getCollisionShape(level, pos).isEmpty();
         if (!this.careful) {
            return pathNoCollisions;
         }

         if (state.is(BlockTags.HAPPY_GHAST_AVOIDS)) {
            return false;
         }

         FluidState fluidState = level.getFluidState(pos);
         if (!fluidState.isEmpty() && (!preciseBlockCollisions || this.mob.collidedWithFluid(fluidState, pos, start, end))) {
            if (fluidState.is(FluidTags.WATER)) {
               return canPathThroughWater;
            }

            if (fluidState.is(FluidTags.LAVA)) {
               return canPathThroughLava;
            }
         }

         return pathNoCollisions;
      }
   }

   private static class GhastShootFireballGoal extends Goal {
      private final Ghast ghast;
      public int chargeTime;

      public GhastShootFireballGoal(final Ghast ghast) {
         this.ghast = ghast;
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
         LivingEntity target = this.ghast.getTarget();
         if (target != null) {
            double maxDist = 64.0;
            if (target.distanceToSqr(this.ghast) < 4096.0 && this.ghast.hasLineOfSight(target)) {
               Level level = this.ghast.level();
               this.chargeTime++;
               if (this.chargeTime == 10 && !this.ghast.isSilent()) {
                  level.levelEvent(null, 1015, this.ghast.blockPosition(), 0);
               }

               if (this.chargeTime == 20) {
                  double d = 4.0;
                  Vec3 viewVector = this.ghast.getViewVector(1.0F);
                  double xdd = target.getX() - (this.ghast.getX() + viewVector.x * 4.0);
                  double ydd = target.getY(0.5) - (0.5 + this.ghast.getY(0.5));
                  double zdd = target.getZ() - (this.ghast.getZ() + viewVector.z * 4.0);
                  Vec3 direction = new Vec3(xdd, ydd, zdd);
                  if (!this.ghast.isSilent()) {
                     level.levelEvent(null, 1016, this.ghast.blockPosition(), 0);
                  }

                  LargeFireball entity = new LargeFireball(level, this.ghast, direction.normalize(), this.ghast.getExplosionPower());
                  entity.setPos(this.ghast.getX() + viewVector.x * 4.0, this.ghast.getY(0.5) + 0.5, entity.getZ() + viewVector.z * 4.0);
                  level.addFreshEntity(entity);
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

      public RandomFloatAroundGoal(final Mob ghast) {
         this(ghast, 0);
      }

      public RandomFloatAroundGoal(final Mob ghast, final int distanceToBlocks) {
         this.ghast = ghast;
         this.distanceToBlocks = distanceToBlocks;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         MoveControl moveControl = this.ghast.getMoveControl();
         if (!moveControl.hasWanted()) {
            return true;
         }

         double xd = moveControl.getWantedX() - this.ghast.getX();
         double yd = moveControl.getWantedY() - this.ghast.getY();
         double zd = moveControl.getWantedZ() - this.ghast.getZ();
         double dd = xd * xd + yd * yd + zd * zd;
         return dd < 1.0 || dd > 3600.0;
      }

      @Override
      public boolean canContinueToUse() {
         return false;
      }

      @Override
      public void start() {
         Vec3 result = getSuitableFlyToPosition(this.ghast, this.distanceToBlocks);
         this.ghast.getMoveControl().setWantedPosition(result.x(), result.y(), result.z(), 1.0);
      }

      public static Vec3 getSuitableFlyToPosition(final Mob mob, final int distanceToBlocks) {
         Level level = mob.level();
         RandomSource random = mob.getRandom();
         Vec3 center = mob.position();
         Vec3 result = null;

         for (int i = 0; i < 64; i++) {
            result = chooseRandomPositionWithRestriction(mob, center, random);
            if (result != null && isGoodTarget(level, result, distanceToBlocks)) {
               return result;
            }
         }

         if (result == null) {
            result = chooseRandomPosition(center, random);
         }

         BlockPos pos = BlockPos.containing(result);
         int heightY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
         if (heightY < pos.getY() && heightY > level.getMinY()) {
            result = new Vec3(result.x(), mob.getY() - Math.abs(mob.getY() - result.y()), result.z());
         }

         return result;
      }

      private static boolean isGoodTarget(final Level level, final Vec3 target, final int distanceToBlocks) {
         if (distanceToBlocks <= 0) {
            return true;
         }

         BlockPos pos = BlockPos.containing(target);
         if (!level.getBlockState(pos).isAir()) {
            return false;
         }

         for (Direction dir : Direction.values()) {
            for (int i = 1; i < distanceToBlocks; i++) {
               BlockPos offset = pos.relative(dir, i);
               if (!level.getBlockState(offset).isAir()) {
                  return true;
               }
            }
         }

         return false;
      }

      private static Vec3 chooseRandomPosition(final Vec3 center, final RandomSource random) {
         double xTarget = center.x() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
         double yTarget = center.y() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
         double zTarget = center.z() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
         return new Vec3(xTarget, yTarget, zTarget);
      }

      private static @Nullable Vec3 chooseRandomPositionWithRestriction(final Mob mob, final Vec3 center, final RandomSource random) {
         Vec3 target = chooseRandomPosition(center, random);
         return mob.hasHome() && !mob.isWithinHome(target) ? null : target;
      }
   }
}
