package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.jspecify.annotations.Nullable;

public class WallClimberNavigation extends GroundPathNavigation {
   private @Nullable BlockPos pathToPosition;

   public WallClimberNavigation(Mob p_26580_, Level p_26581_) {
      super(p_26580_, p_26581_);
   }

   @Override
   public Path createPath(BlockPos p_26589_, int p_26590_) {
      this.pathToPosition = p_26589_;
      return super.createPath(p_26589_, p_26590_);
   }

   @Override
   public Path createPath(Entity p_26586_, int p_26587_) {
      this.pathToPosition = p_26586_.blockPosition();
      return super.createPath(p_26586_, p_26587_);
   }

   @Override
   public boolean moveTo(Entity p_26583_, double p_26584_) {
      Path path = this.createPath(p_26583_, 0);
      if (path != null) {
         return this.moveTo(path, p_26584_);
      }

      this.pathToPosition = p_26583_.blockPosition();
      this.speedModifier = p_26584_;
      return true;
   }

   @Override
   public void tick() {
      if (!this.isDone()) {
         super.tick();
      } else if (this.pathToPosition != null) {
         if (this.pathToPosition.closerToCenterThan(this.mob.position(), this.mob.getBbWidth())
            || this.mob.getY() > this.pathToPosition.getY()
               && BlockPos.containing(this.pathToPosition.getX(), this.mob.getY(), this.pathToPosition.getZ())
                  .closerToCenterThan(this.mob.position(), this.mob.getBbWidth())) {
            this.pathToPosition = null;
         } else {
            this.mob.getMoveControl().setWantedPosition(this.pathToPosition.getX(), this.pathToPosition.getY(), this.pathToPosition.getZ(), this.speedModifier);
         }
      }
   }
}
