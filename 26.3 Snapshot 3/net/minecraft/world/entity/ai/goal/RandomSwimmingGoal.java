package net.minecraft.world.entity.ai.goal;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RandomSwimmingGoal extends RandomStrollGoal {
   public RandomSwimmingGoal(final PathfinderMob mob, final double speedModifier, final int interval) {
      super(mob, speedModifier, interval);
   }

   @Override
   protected @Nullable Vec3 getPosition() {
      return BehaviorUtils.getRandomSwimmablePos(this.mob, 10, 7);
   }
}
