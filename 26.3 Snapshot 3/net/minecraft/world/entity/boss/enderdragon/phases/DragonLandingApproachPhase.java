package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DragonLandingApproachPhase extends AbstractDragonPhaseInstance {
   private static final TargetingConditions NEAR_EGG_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight();
   private @Nullable Path currentPath;
   private @Nullable Vec3 targetLocation;

   public DragonLandingApproachPhase(final EnderDragon dragon) {
      super(dragon);
   }

   @Override
   public EnderDragonPhase<DragonLandingApproachPhase> getPhase() {
      return EnderDragonPhase.LANDING_APPROACH;
   }

   @Override
   public void begin() {
      this.currentPath = null;
      this.targetLocation = null;
   }

   @Override
   public void doServerTick(final ServerLevel level) {
      double distToTarget = this.targetLocation == null ? 0.0 : this.targetLocation.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
      if (distToTarget < 100.0 || distToTarget > 22500.0 || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
         this.findNewTarget(level);
      }
   }

   @Override
   public @Nullable Vec3 getFlyTargetLocation() {
      return this.targetLocation;
   }

   private void findNewTarget(final ServerLevel level) {
      if (this.currentPath == null || this.currentPath.isDone()) {
         int currentNodeIndex = this.dragon.findClosestNode();
         BlockPos egg = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EnderDragonFight.getPodiumLocation(this.dragon.getFightOrigin()));
         Player playerNearestToEgg = level.getNearestPlayer(NEAR_EGG_TARGETING, this.dragon, egg.getX(), egg.getY(), egg.getZ());
         int targetNodeIndex;
         if (playerNearestToEgg != null) {
            Vec3 aim = new Vec3(playerNearestToEgg.getX(), 0.0, playerNearestToEgg.getZ()).normalize();
            targetNodeIndex = this.dragon.findClosestNode(-aim.x * 40.0, 105.0, -aim.z * 40.0);
         } else {
            targetNodeIndex = this.dragon.findClosestNode(40.0, egg.getY(), 0.0);
         }

         Node finalNode = new Node(egg.getX(), egg.getY(), egg.getZ());
         this.currentPath = this.dragon.findPath(currentNodeIndex, targetNodeIndex, finalNode);
         if (this.currentPath != null) {
            this.currentPath.advance();
         }
      }

      this.navigateToNextPathNode();
      if (this.currentPath != null && this.currentPath.isDone()) {
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING);
      }
   }

   private void navigateToNextPathNode() {
      if (this.currentPath != null && !this.currentPath.isDone()) {
         Vec3i current = this.currentPath.getNextNodePos();
         this.currentPath.advance();
         double xTarget = current.getX();
         double zTarget = current.getZ();

         double yTarget;
         do {
            yTarget = current.getY() + this.dragon.getRandom().nextFloat() * 20.0F;
         } while (yTarget < current.getY());

         this.targetLocation = new Vec3(xTarget, yTarget, zTarget);
      }
   }
}
