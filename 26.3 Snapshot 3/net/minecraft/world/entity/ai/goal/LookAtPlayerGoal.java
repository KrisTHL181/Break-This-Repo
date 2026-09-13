package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public class LookAtPlayerGoal extends Goal {
   public static final float DEFAULT_PROBABILITY = 0.02F;
   protected final Mob mob;
   protected @Nullable Entity lookAt;
   protected final float lookDistance;
   private int lookTime;
   protected final float probability;
   private final boolean onlyHorizontal;
   protected final Class<? extends LivingEntity> lookAtType;
   protected final TargetingConditions lookAtContext;

   public LookAtPlayerGoal(final Mob mob, final Class<? extends LivingEntity> lookAtType, final float lookDistance) {
      this(mob, lookAtType, lookDistance, 0.02F);
   }

   public LookAtPlayerGoal(final Mob mob, final Class<? extends LivingEntity> lookAtType, final float lookDistance, final float probability) {
      this(mob, lookAtType, lookDistance, probability, false);
   }

   public LookAtPlayerGoal(
      final Mob mob, final Class<? extends LivingEntity> lookAtType, final float lookDistance, final float probability, final boolean onlyHorizontal
   ) {
      this.mob = mob;
      this.lookAtType = lookAtType;
      this.lookDistance = lookDistance;
      this.probability = probability;
      this.onlyHorizontal = onlyHorizontal;
      this.setFlags(EnumSet.of(Goal.Flag.LOOK));
      if (lookAtType == Player.class) {
         Predicate<Entity> selector = EntitySelector.notRiding(mob);
         this.lookAtContext = TargetingConditions.forNonCombat().range(lookDistance).selector((target, level) -> selector.test(target));
      } else {
         this.lookAtContext = TargetingConditions.forNonCombat().range(lookDistance);
      }
   }

   @Override
   public boolean canUse() {
      if (this.mob.getRandom().nextFloat() >= this.probability) {
         return false;
      }

      if (this.mob.getTarget() != null) {
         this.lookAt = this.mob.getTarget();
      }

      ServerLevel level = getServerLevel(this.mob);
      if (this.lookAtType == Player.class) {
         this.lookAt = level.getNearestPlayer(this.lookAtContext, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
      } else {
         this.lookAt = level.getNearestEntity(
            this.mob.level().getEntitiesOfClass(this.lookAtType, this.mob.getBoundingBox().inflate(this.lookDistance, 3.0, this.lookDistance), entity -> true),
            this.lookAtContext,
            this.mob,
            this.mob.getX(),
            this.mob.getEyeY(),
            this.mob.getZ()
         );
      }

      return this.lookAt != null;
   }

   @Override
   public boolean canContinueToUse() {
      if (!this.lookAt.isAlive()) {
         return false;
      } else {
         return this.mob.distanceToSqr(this.lookAt) > this.lookDistance * this.lookDistance ? false : this.lookTime > 0;
      }
   }

   @Override
   public void start() {
      this.lookTime = this.adjustedTickDelay(40 + this.mob.getRandom().nextInt(40));
   }

   @Override
   public void stop() {
      this.lookAt = null;
   }

   @Override
   public void tick() {
      if (this.lookAt.isAlive()) {
         double targetY = this.onlyHorizontal ? this.mob.getEyeY() : this.lookAt.getEyeY();
         this.mob.getLookControl().setLookAt(this.lookAt.getX(), targetY, this.lookAt.getZ());
         this.lookTime--;
      }
   }
}
