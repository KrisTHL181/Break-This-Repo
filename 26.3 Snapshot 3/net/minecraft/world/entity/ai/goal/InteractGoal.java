package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class InteractGoal extends LookAtPlayerGoal {
   public InteractGoal(final Mob mob, final Class<? extends LivingEntity> lookAtType, final float lookDistance) {
      super(mob, lookAtType, lookDistance);
      this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE));
   }

   public InteractGoal(final Mob mob, final Class<? extends LivingEntity> lookAtType, final float lookDistance, final float probability) {
      super(mob, lookAtType, lookDistance, probability);
      this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE));
   }
}
