package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class NearestAttackableTargetGoal<T extends LivingEntity> extends TargetGoal {
   private static final int DEFAULT_RANDOM_INTERVAL = 10;
   protected final Class<T> targetType;
   protected final int randomInterval;
   protected @Nullable LivingEntity target;
   protected final TargetingConditions targetConditions;

   public NearestAttackableTargetGoal(final Mob mob, final Class<T> targetType, final boolean mustSee) {
      this(mob, targetType, 10, mustSee, false, null);
   }

   public NearestAttackableTargetGoal(final Mob mob, final Class<T> targetType, final boolean mustSee, final TargetingConditions.Selector selector) {
      this(mob, targetType, 10, mustSee, false, selector);
   }

   public NearestAttackableTargetGoal(final Mob mob, final Class<T> targetType, final boolean mustSee, final boolean mustReach) {
      this(mob, targetType, 10, mustSee, mustReach, null);
   }

   public NearestAttackableTargetGoal(
      final Mob mob,
      final Class<T> targetType,
      final int randomInterval,
      final boolean mustSee,
      final boolean mustReach,
      final TargetingConditions.@Nullable Selector selector
   ) {
      super(mob, mustSee, mustReach);
      this.targetType = targetType;
      this.randomInterval = reducedTickDelay(randomInterval);
      this.setFlags(EnumSet.of(Goal.Flag.TARGET));
      this.targetConditions = TargetingConditions.forCombat().range(this.getFollowDistance()).selector(selector);
   }

   @Override
   public boolean canUse() {
      if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
         return false;
      }

      this.findTarget();
      return this.target != null;
   }

   protected AABB getTargetSearchArea(final double followDistance) {
      return this.mob.getBoundingBox().inflate(followDistance, followDistance, followDistance);
   }

   protected void findTarget() {
      ServerLevel level = getServerLevel(this.mob);
      if (this.targetType != Player.class && this.targetType != ServerPlayer.class) {
         this.target = level.getNearestEntity(
            this.mob.level().getEntitiesOfClass(this.targetType, this.getTargetSearchArea(this.getFollowDistance()), entity -> true),
            this.getTargetConditions(),
            this.mob,
            this.mob.getX(),
            this.mob.getEyeY(),
            this.mob.getZ()
         );
      } else {
         this.target = level.getNearestPlayer(this.getTargetConditions(), this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
      }
   }

   @Override
   public void start() {
      this.mob.setTarget(this.target);
      super.start();
   }

   public void setTarget(final @Nullable LivingEntity target) {
      this.target = target;
   }

   private TargetingConditions getTargetConditions() {
      return this.targetConditions.range(this.getFollowDistance());
   }
}
