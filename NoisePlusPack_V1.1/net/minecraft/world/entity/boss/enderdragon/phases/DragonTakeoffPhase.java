package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DragonTakeoffPhase extends AbstractDragonPhaseInstance {
   private boolean firstTick;
   private @Nullable Path currentPath;
   private @Nullable Vec3 targetLocation;

   public DragonTakeoffPhase(EnderDragon p_31370_) {
      super(p_31370_);
   }

   @Override
   public void doServerTick(ServerLevel p_362190_) {
      if (!this.firstTick && this.currentPath != null) {
         BlockPos blockpos = p_362190_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(this.dragon.getFightOrigin()));
         if (!blockpos.closerToCenterThan(this.dragon.position(), 10.0)) {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
         }
      } else {
         this.firstTick = false;
         this.findNewTarget();
      }
   }

   @Override
   public void begin() {
      this.firstTick = true;
      this.currentPath = null;
      this.targetLocation = null;
   }

   private void findNewTarget() {
      int i = this.dragon.findClosestNode();
      Vec3 vec3 = this.dragon.getHeadLookVector(1.0F);
      int j = this.dragon.findClosestNode(-vec3.x * 40.0, 105.0, -vec3.z * 40.0);
      if (this.dragon.getDragonFight() != null && this.dragon.getDragonFight().getCrystalsAlive() > 0) {
         j %= 12;
         if (j < 0) {
            j += 12;
         }
      } else {
         j -= 12;
         j &= 7;
         j += 12;
      }

      this.currentPath = this.dragon.findPath(i, j, null);
      this.navigateToNextPathNode();
   }

   private void navigateToNextPathNode() {
      if (this.currentPath != null) {
         this.currentPath.advance();
         if (!this.currentPath.isDone()) {
            Vec3i vec3i = this.currentPath.getNextNodePos();
            this.currentPath.advance();

            double d0;
            do {
               d0 = vec3i.getY() + this.dragon.getRandom().nextFloat() * 20.0F;
            } while (d0 < vec3i.getY());

            this.targetLocation = new Vec3(vec3i.getX(), d0, vec3i.getZ());
         }
      }
   }

   @Override
   public @Nullable Vec3 getFlyTargetLocation() {
      return this.targetLocation;
   }

   @Override
   public EnderDragonPhase<DragonTakeoffPhase> getPhase() {
      return EnderDragonPhase.TAKEOFF;
   }
}
