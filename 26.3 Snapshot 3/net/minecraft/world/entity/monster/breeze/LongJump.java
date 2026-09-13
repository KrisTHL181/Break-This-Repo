package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.LongJumpUtil;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LongJump extends Behavior<Breeze> {
   private static final int REQUIRED_AIR_BLOCKS_ABOVE = 4;
   private static final int JUMP_COOLDOWN_TICKS = 10;
   private static final int JUMP_COOLDOWN_WHEN_HURT_TICKS = 2;
   private static final int INHALING_DURATION_TICKS = Math.round(10.0F);
   private static final float DEFAULT_FOLLOW_RANGE = 24.0F;
   private static final float DEFAULT_MAX_JUMP_VELOCITY = 1.4F;
   private static final float MAX_JUMP_VELOCITY_MULTIPLIER = 0.058333334F;
   private static final ObjectArrayList<Integer> ALLOWED_ANGLES = new ObjectArrayList(Lists.newArrayList(new Integer[]{40, 55, 60, 75, 80}));

   @VisibleForTesting
   public LongJump() {
      super(
         Map.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.BREEZE_JUMP_COOLDOWN,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_JUMP_INHALING,
            MemoryStatus.REGISTERED,
            MemoryModuleType.BREEZE_JUMP_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.BREEZE_SHOOT,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_LEAVING_WATER,
            MemoryStatus.REGISTERED
         ),
         200
      );
   }

   public static boolean canRun(final ServerLevel level, final Breeze breeze) {
      if (!breeze.onGround() && !breeze.isInWater()) {
         return false;
      }

      if (Swim.shouldSwim(breeze)) {
         return false;
      }

      if (breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_JUMP_TARGET, MemoryStatus.VALUE_PRESENT)) {
         return true;
      }

      LivingEntity attackTarget = breeze.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
      if (attackTarget == null) {
         return false;
      }

      if (outOfAggroRange(breeze, attackTarget)) {
         breeze.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
         return false;
      }

      if (tooCloseForJump(breeze, attackTarget)) {
         return false;
      }

      if (!canJumpFromCurrentPosition(level, breeze)) {
         return false;
      }

      BlockPos targetPos = snapToSurface(breeze, BreezeUtil.randomPointBehindTarget(attackTarget, breeze.getRandom()));
      if (targetPos == null) {
         return false;
      }

      BlockState bs = level.getBlockState(targetPos.below());
      if (breeze.getType().isBlockDangerous(bs)) {
         return false;
      }

      if (!BreezeUtil.hasLineOfSight(breeze, Vec3.atCenterOf(targetPos)) && !BreezeUtil.hasLineOfSight(breeze, Vec3.atCenterOf(targetPos.above(4)))) {
         return false;
      }

      breeze.getBrain().setMemory(MemoryModuleType.BREEZE_JUMP_TARGET, targetPos);
      return true;
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Breeze breeze) {
      return canRun(level, breeze);
   }

   protected boolean canStillUse(final ServerLevel level, final Breeze breeze, final long timestamp) {
      return breeze.getPose() != Pose.STANDING && !breeze.getBrain().hasMemoryValue(MemoryModuleType.BREEZE_JUMP_COOLDOWN);
   }

   protected void start(final ServerLevel level, final Breeze breeze, final long timestamp) {
      if (breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_JUMP_INHALING, MemoryStatus.VALUE_ABSENT)) {
         breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_JUMP_INHALING, Unit.INSTANCE, INHALING_DURATION_TICKS);
      }

      breeze.setPose(Pose.INHALING);
      level.playSound(null, breeze, SoundEvents.BREEZE_CHARGE, SoundSource.HOSTILE, 1.0F, 1.0F);
      breeze.getBrain()
         .getMemory(MemoryModuleType.BREEZE_JUMP_TARGET)
         .ifPresent(targetPos -> breeze.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(targetPos)));
   }

   protected void tick(final ServerLevel level, final Breeze breeze, final long timestamp) {
      boolean inWater = breeze.isInWater();
      if (!inWater && breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_LEAVING_WATER, MemoryStatus.VALUE_PRESENT)) {
         breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_LEAVING_WATER);
      }

      if (isFinishedInhaling(breeze)) {
         Vec3 velocityVector = breeze.getBrain()
            .getMemory(MemoryModuleType.BREEZE_JUMP_TARGET)
            .flatMap(targetPos -> calculateOptimalJumpVector(breeze, breeze.getRandom(), Vec3.atBottomCenterOf(targetPos)))
            .orElse(null);
         if (velocityVector == null) {
            breeze.setPose(Pose.STANDING);
            return;
         }

         if (inWater) {
            breeze.getBrain().setMemory(MemoryModuleType.BREEZE_LEAVING_WATER, Unit.INSTANCE);
         }

         breeze.playSound(SoundEvents.BREEZE_JUMP, 1.0F, 1.0F);
         breeze.setPose(Pose.LONG_JUMPING);
         breeze.setYRot(breeze.yBodyRot);
         breeze.setDiscardFriction(true);
         breeze.setDeltaMovement(velocityVector);
      } else if (isFinishedJumping(breeze)) {
         breeze.playSound(SoundEvents.BREEZE_LAND, 1.0F, 1.0F);
         breeze.setPose(Pose.STANDING);
         breeze.setDiscardFriction(false);
         boolean wasHurt = breeze.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
         breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_JUMP_COOLDOWN, Unit.INSTANCE, wasHurt ? 2L : 10L);
         breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, 100L);
      }
   }

   protected void stop(final ServerLevel level, final Breeze breeze, final long timestamp) {
      if (breeze.getPose() == Pose.LONG_JUMPING || breeze.getPose() == Pose.INHALING) {
         breeze.setPose(Pose.STANDING);
      }

      breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_JUMP_TARGET);
      breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_JUMP_INHALING);
      breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_LEAVING_WATER);
   }

   private static boolean isFinishedInhaling(final Breeze breeze) {
      return breeze.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_INHALING).isEmpty() && breeze.getPose() == Pose.INHALING;
   }

   private static boolean isFinishedJumping(final Breeze breeze) {
      boolean isJumping = breeze.getPose() == Pose.LONG_JUMPING;
      boolean landedOnGround = breeze.onGround();
      boolean landedInWater = breeze.isInWater() && breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_LEAVING_WATER, MemoryStatus.VALUE_ABSENT);
      return isJumping && (landedOnGround || landedInWater);
   }

   private static @Nullable BlockPos snapToSurface(final LivingEntity entity, final Vec3 target) {
      ClipContext collisionBelow = new ClipContext(target, target.relative(Direction.DOWN, 10.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
      HitResult surfaceBelow = entity.level().clip(collisionBelow);
      if (surfaceBelow.getType() == HitResult.Type.BLOCK) {
         return BlockPos.containing(surfaceBelow.getLocation()).above();
      }

      ClipContext collisionAbove = new ClipContext(target, target.relative(Direction.UP, 10.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
      HitResult surfaceAbove = entity.level().clip(collisionAbove);
      return surfaceAbove.getType() == HitResult.Type.BLOCK ? BlockPos.containing(surfaceAbove.getLocation()).above() : null;
   }

   private static boolean outOfAggroRange(final Breeze breeze, final LivingEntity attackTarget) {
      return !attackTarget.closerThan(breeze, breeze.getAttributeValue(Attributes.FOLLOW_RANGE));
   }

   private static boolean tooCloseForJump(final Breeze breeze, final LivingEntity attackTarget) {
      return attackTarget.distanceTo(breeze) - 4.0F <= 0.0F;
   }

   private static boolean canJumpFromCurrentPosition(final ServerLevel level, final Breeze breeze) {
      BlockPos currentPos = breeze.blockPosition();
      if (level.getBlockState(currentPos).is(Blocks.HONEY_BLOCK)) {
         return false;
      }

      for (int i = 1; i <= 4; i++) {
         BlockPos offsetPos = currentPos.relative(Direction.UP, i);
         if (!level.getBlockState(offsetPos).isAir() && !level.getFluidState(offsetPos).is(FluidTags.WATER)) {
            return false;
         }
      }

      return true;
   }

   private static Optional<Vec3> calculateOptimalJumpVector(final Breeze body, final RandomSource random, final Vec3 targetPos) {
      for (int angle : Util.shuffledCopy(ALLOWED_ANGLES, random)) {
         float maxJumpVelocity = 0.058333334F * (float)body.getAttributeValue(Attributes.FOLLOW_RANGE);
         Optional<Vec3> velocityVector = LongJumpUtil.calculateJumpVectorForAngle(body, targetPos, maxJumpVelocity, angle, false);
         if (velocityVector.isPresent()) {
            if (body.hasEffect(MobEffects.JUMP_BOOST)) {
               double jumpEffectAmplifier = velocityVector.get().normalize().y * body.getJumpBoostPower();
               return velocityVector.map(v -> v.add(0.0, jumpEffectAmplifier, 0.0));
            }

            return velocityVector;
         }
      }

      return Optional.empty();
   }
}
