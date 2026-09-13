package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SpearRetreat extends Behavior<PathfinderMob> {
   public static final int MIN_COOLDOWN_DISTANCE = 9;
   public static final int MAX_COOLDOWN_DISTANCE = 11;
   public static final int MAX_FLEEING_TIME = 100;
   double speedModifierWhenRepositioning;

   public SpearRetreat(double p_453222_) {
      super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryStatus.VALUE_PRESENT), 100);
      this.speedModifierWhenRepositioning = p_453222_;
   }

   private @Nullable LivingEntity getTarget(PathfinderMob p_452051_) {
      return p_452051_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
   }

   private boolean ableToAttack(PathfinderMob p_454307_) {
      return this.getTarget(p_454307_) != null && p_454307_.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
   }

   protected boolean checkExtraStartConditions(ServerLevel p_459120_, PathfinderMob p_456725_) {
      if (this.ableToAttack(p_456725_) && !p_456725_.isUsingItem()) {
         if (p_456725_.getBrain().getMemory(MemoryModuleType.SPEAR_STATUS).orElse(SpearAttack.SpearStatus.APPROACH) != SpearAttack.SpearStatus.RETREAT) {
            return false;
         }

         LivingEntity livingentity = this.getTarget(p_456725_);
         double d0 = p_456725_.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
         int i = p_456725_.isPassenger() ? 2 : 0;
         double d1 = Math.sqrt(d0);
         Vec3 vec3 = LandRandomPos.getPosAway(p_456725_, Math.max(0.0, 9 + i - d1), Math.max(1.0, 11 + i - d1), 7, livingentity.position());
         if (vec3 == null) {
            return false;
         }

         p_456725_.getBrain().setMemory(MemoryModuleType.SPEAR_FLEEING_POSITION, vec3);
         return true;
      } else {
         return false;
      }
   }

   protected void start(ServerLevel p_457784_, PathfinderMob p_456368_, long p_452415_) {
      p_456368_.setAggressive(true);
      p_456368_.getBrain().setMemory(MemoryModuleType.SPEAR_FLEEING_TIME, 0);
      super.start(p_457784_, p_456368_, p_452415_);
   }

   protected boolean canStillUse(ServerLevel p_456176_, PathfinderMob p_451628_, long p_460888_) {
      return p_451628_.getBrain().getMemory(MemoryModuleType.SPEAR_FLEEING_TIME).orElse(100) < 100
         && p_451628_.getBrain().getMemory(MemoryModuleType.SPEAR_FLEEING_POSITION).isPresent()
         && !p_451628_.getNavigation().isDone()
         && this.ableToAttack(p_451628_);
   }

   protected void tick(ServerLevel p_451139_, PathfinderMob p_450496_, long p_453244_) {
      LivingEntity livingentity = this.getTarget(p_450496_);
      float f = p_450496_.getRootVehicle() instanceof Mob mob ? mob.chargeSpeedModifier() : 1.0F;
      p_450496_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingentity, true));
      p_450496_.getBrain().setMemory(MemoryModuleType.SPEAR_FLEEING_TIME, p_450496_.getBrain().getMemory(MemoryModuleType.SPEAR_FLEEING_TIME).orElse(0) + 1);
      p_450496_.getBrain()
         .getMemory(MemoryModuleType.SPEAR_FLEEING_POSITION)
         .ifPresent(p_457950_ -> p_450496_.getNavigation().moveTo(p_457950_.x, p_457950_.y, p_457950_.z, f * this.speedModifierWhenRepositioning));
   }

   protected void stop(ServerLevel p_459644_, PathfinderMob p_454564_, long p_460007_) {
      p_454564_.getNavigation().stop();
      p_454564_.setAggressive(false);
      p_454564_.stopUsingItem();
      p_454564_.getBrain().eraseMemory(MemoryModuleType.SPEAR_FLEEING_TIME);
      p_454564_.getBrain().eraseMemory(MemoryModuleType.SPEAR_FLEEING_POSITION);
      p_454564_.getBrain().eraseMemory(MemoryModuleType.SPEAR_STATUS);
   }
}
