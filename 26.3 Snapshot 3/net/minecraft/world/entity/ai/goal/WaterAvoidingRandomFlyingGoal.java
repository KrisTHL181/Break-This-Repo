package net.minecraft.world.entity.ai.goal;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class WaterAvoidingRandomFlyingGoal extends WaterAvoidingRandomStrollGoal {
   public WaterAvoidingRandomFlyingGoal(final PathfinderMob mob, final double speedModifier) {
      super(mob, speedModifier);
   }

   @Override
   protected @Nullable Vec3 getPosition() {
      Vec3 wanderDirection = this.mob.getViewVector(0.0F);
      int xzDist = 8;
      Vec3 groundBasedPosition = HoverRandomPos.getPos(this.mob, 8, 7, wanderDirection.x, wanderDirection.z, (float) (Math.PI / 2), 3, 1);
      return groundBasedPosition != null
         ? groundBasedPosition
         : AirAndWaterRandomPos.getPos(this.mob, 8, 4, -2, wanderDirection.x, wanderDirection.z, (float) (Math.PI / 2));
   }
}
