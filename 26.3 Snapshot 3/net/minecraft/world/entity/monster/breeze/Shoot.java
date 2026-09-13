package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.BreezeWindCharge;
import net.minecraft.world.item.ItemStack;

public class Shoot extends Behavior<Breeze> {
   private static final int ATTACK_RANGE_MAX_SQRT = 256;
   private static final int UNCERTAINTY_BASE = 5;
   private static final int UNCERTAINTY_MULTIPLIER = 4;
   private static final float PROJECTILE_MOVEMENT_SCALE = 0.7F;
   private static final int SHOOT_INITIAL_DELAY_TICKS = Math.round(15.0F);
   private static final int SHOOT_RECOVER_DELAY_TICKS = Math.round(4.0F);
   private static final int SHOOT_COOLDOWN_TICKS = Math.round(10.0F);

   @VisibleForTesting
   public Shoot() {
      super(
         ImmutableMap.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.BREEZE_SHOOT_COOLDOWN,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_SHOOT_CHARGING,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_SHOOT_RECOVERING,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_SHOOT,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_JUMP_TARGET,
            MemoryStatus.VALUE_ABSENT
         ),
         SHOOT_INITIAL_DELAY_TICKS + 1 + SHOOT_RECOVER_DELAY_TICKS
      );
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Breeze breeze) {
      return breeze.getPose() != Pose.STANDING
         ? false
         : breeze.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).map(target -> isTargetWithinRange(breeze, target)).map(withinRange -> {
            if (!withinRange) {
               breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
            }

            return (Boolean)withinRange;
         }).orElse(false);
   }

   protected boolean canStillUse(final ServerLevel level, final Breeze body, final long timestamp) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && body.getBrain().hasMemoryValue(MemoryModuleType.BREEZE_SHOOT);
   }

   protected void start(final ServerLevel level, final Breeze breeze, final long timestamp) {
      breeze.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> breeze.setPose(Pose.SHOOTING));
      breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_CHARGING, Unit.INSTANCE, SHOOT_INITIAL_DELAY_TICKS);
      breeze.playSound(SoundEvents.BREEZE_INHALE, 1.0F, 1.0F);
   }

   protected void stop(final ServerLevel level, final Breeze breeze, final long timestamp) {
      if (breeze.getPose() == Pose.SHOOTING) {
         breeze.setPose(Pose.STANDING);
      }

      breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_COOLDOWN, Unit.INSTANCE, SHOOT_COOLDOWN_TICKS);
      breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
   }

   protected void tick(final ServerLevel level, final Breeze breeze, final long timestamp) {
      Brain<Breeze> brain = breeze.getBrain();
      LivingEntity target = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
      if (target != null) {
         breeze.lookAt(EntityAnchorArgument.Anchor.EYES, target.position());
         if (!brain.getMemory(MemoryModuleType.BREEZE_SHOOT_CHARGING).isPresent() && !brain.getMemory(MemoryModuleType.BREEZE_SHOOT_RECOVERING).isPresent()) {
            brain.setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_RECOVERING, Unit.INSTANCE, SHOOT_RECOVER_DELAY_TICKS);
            double xd = target.getX() - breeze.getX();
            double yd = target.getY(target.isPassenger() ? 0.8 : 0.3) - breeze.getFiringYPosition();
            double zd = target.getZ() - breeze.getZ();
            Projectile.spawnProjectileUsingShoot(
               new BreezeWindCharge(breeze, level), level, ItemStack.EMPTY, xd, yd, zd, 0.7F, 5 - level.getDifficulty().getId() * 4
            );
            breeze.playSound(SoundEvents.BREEZE_SHOOT, 1.5F, 1.0F);
         }
      }
   }

   private static boolean isTargetWithinRange(final Breeze body, final LivingEntity target) {
      double distanceSqrt = body.position().distanceToSqr(target.position());
      return distanceSqrt < 256.0;
   }
}
