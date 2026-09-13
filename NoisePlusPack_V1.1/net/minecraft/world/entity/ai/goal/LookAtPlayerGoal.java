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

   public LookAtPlayerGoal(Mob p_25520_, Class<? extends LivingEntity> p_25521_, float p_25522_) {
      this(p_25520_, p_25521_, p_25522_, 0.02F);
   }

   public LookAtPlayerGoal(Mob p_25524_, Class<? extends LivingEntity> p_25525_, float p_25526_, float p_25527_) {
      this(p_25524_, p_25525_, p_25526_, p_25527_, false);
   }

   public LookAtPlayerGoal(Mob p_148118_, Class<? extends LivingEntity> p_148119_, float p_148120_, float p_148121_, boolean p_148122_) {
      this.mob = p_148118_;
      this.lookAtType = p_148119_;
      this.lookDistance = p_148120_;
      this.probability = p_148121_;
      this.onlyHorizontal = p_148122_;
      this.setFlags(EnumSet.of(Goal.Flag.LOOK));
      if (p_148119_ == Player.class) {
         Predicate<Entity> predicate = EntitySelector.notRiding(p_148118_);
         this.lookAtContext = TargetingConditions.forNonCombat().range(p_148120_).selector((p_359094_, p_359095_) -> predicate.test(p_359094_));
      } else {
         this.lookAtContext = TargetingConditions.forNonCombat().range(p_148120_);
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

      ServerLevel serverlevel = getServerLevel(this.mob);
      if (this.lookAtType == Player.class) {
         this.lookAt = serverlevel.getNearestPlayer(this.lookAtContext, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
      } else {
         this.lookAt = serverlevel.getNearestEntity(
            this.mob
               .level()
               .getEntitiesOfClass(this.lookAtType, this.mob.getBoundingBox().inflate(this.lookDistance, 3.0, this.lookDistance), p_148124_ -> true),
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
         double d0 = this.onlyHorizontal ? this.mob.getEyeY() : this.lookAt.getEyeY();
         this.mob.getLookControl().setLookAt(this.lookAt.getX(), d0, this.lookAt.getZ());
         this.lookTime--;
      }
   }
}
