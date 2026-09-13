package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.jspecify.annotations.Nullable;

public class WallClimberNavigation extends GroundPathNavigation {
   private @Nullable BlockPos pathToPosition;

   public WallClimberNavigation(final Mob mob, final Level level) {
      super(mob, level);
   }

   @Override
   public Path createPath(final BlockPos pos, final int reachRange) {
      this.pathToPosition = pos;
      return super.createPath(pos, reachRange);
   }

   @Override
   public Path createPath(final Entity target, final int reachRange) {
      this.pathToPosition = target.blockPosition();
      return super.createPath(target, reachRange);
   }

   @Override
   public boolean moveTo(final Entity target, final double speedModifier) {
      Path newPath = this.createPath(target, 0);
      if (newPath != null) {
         return this.moveTo(newPath, speedModifier);
      }

      this.pathToPosition = target.blockPosition();
      this.speedModifier = speedModifier;
      return true;
   }

   @Override
   public void tick() {
      if (!this.isDone()) {
         super.tick();
      } else {
         if (this.pathToPosition != null) {
            if (!this.pathToPosition.closerToCenterThan(this.mob.position(), this.mob.getBbWidth())
               && (
                  !(this.mob.getY() > this.pathToPosition.getY())
                     || !BlockPos.containing(this.pathToPosition.getX(), this.mob.getY(), this.pathToPosition.getZ())
                        .closerToCenterThan(this.mob.position(), this.mob.getBbWidth())
               )) {
               this.mob
                  .getMoveControl()
                  .setWantedPosition(this.pathToPosition.getX(), this.pathToPosition.getY(), this.pathToPosition.getZ(), this.speedModifier);
            } else {
               this.pathToPosition = null;
            }
         }
      }
   }
}
