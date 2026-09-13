package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AvoidEntityGoal<T extends LivingEntity> extends Goal {
   protected final PathfinderMob mob;
   private final double walkSpeedModifier;
   private final double sprintSpeedModifier;
   protected @Nullable T toAvoid;
   protected final float maxDist;
   protected @Nullable Path path;
   protected final PathNavigation pathNav;
   protected final Class<T> avoidClass;
   protected final Predicate<? super LivingEntity> avoidPredicate;
   protected final Predicate<? super LivingEntity> predicateOnAvoidEntity;
   private final TargetingConditions avoidEntityTargeting;

   public AvoidEntityGoal(PathfinderMob p_25027_, Class<T> p_25028_, float p_25029_, double p_25030_, double p_25031_) {
      this(p_25027_, p_25028_, p_25052_ -> true, p_25029_, p_25030_, p_25031_, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
   }

   public AvoidEntityGoal(
      PathfinderMob p_25040_,
      Class<T> p_25041_,
      Predicate<LivingEntity> p_25042_,
      float p_25043_,
      double p_25044_,
      double p_25045_,
      Predicate<? super LivingEntity> p_25046_
   ) {
      this.mob = p_25040_;
      this.avoidClass = p_25041_;
      this.avoidPredicate = p_25042_;
      this.maxDist = p_25043_;
      this.walkSpeedModifier = p_25044_;
      this.sprintSpeedModifier = p_25045_;
      this.predicateOnAvoidEntity = p_25046_;
      this.pathNav = p_25040_.getNavigation();
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      this.avoidEntityTargeting = TargetingConditions.forCombat()
         .range(p_25043_)
         .selector((p_359091_, p_359092_) -> p_25046_.test(p_359091_) && p_25042_.test(p_359091_));
   }

   public AvoidEntityGoal(PathfinderMob p_25033_, Class<T> p_25034_, float p_25035_, double p_25036_, double p_25037_, Predicate<? super LivingEntity> p_25038_) {
      this(p_25033_, p_25034_, p_25049_ -> true, p_25035_, p_25036_, p_25037_, p_25038_);
   }

   @Override
   public boolean canUse() {
      this.toAvoid = getServerLevel(this.mob)
         .getNearestEntity(
            this.mob.level().getEntitiesOfClass(this.avoidClass, this.mob.getBoundingBox().inflate(this.maxDist, 3.0, this.maxDist), p_148078_ -> true),
            this.avoidEntityTargeting,
            this.mob,
            this.mob.getX(),
            this.mob.getY(),
            this.mob.getZ()
         );
      if (this.toAvoid == null) {
         return false;
      }

      Vec3 vec3 = DefaultRandomPos.getPosAway(this.mob, 16, 7, this.toAvoid.position());
      if (vec3 == null) {
         return false;
      }

      if (this.toAvoid.distanceToSqr(vec3.x, vec3.y, vec3.z) < this.toAvoid.distanceToSqr(this.mob)) {
         return false;
      }

      this.path = this.pathNav.createPath(vec3.x, vec3.y, vec3.z, 0);
      return this.path != null;
   }

   @Override
   public boolean canContinueToUse() {
      return !this.pathNav.isDone();
   }

   @Override
   public void start() {
      this.pathNav.moveTo(this.path, this.walkSpeedModifier);
   }

   @Override
   public void stop() {
      this.toAvoid = null;
   }

   @Override
   public void tick() {
      if (this.mob.distanceToSqr(this.toAvoid) < 49.0) {
         this.mob.getNavigation().setSpeedModifier(this.sprintSpeedModifier);
      } else {
         this.mob.getNavigation().setSpeedModifier(this.walkSpeedModifier);
      }
   }
}
