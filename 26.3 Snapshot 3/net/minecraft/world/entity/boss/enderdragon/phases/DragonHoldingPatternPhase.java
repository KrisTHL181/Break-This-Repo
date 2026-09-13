package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DragonHoldingPatternPhase extends AbstractDragonPhaseInstance {
   private static final TargetingConditions NEW_TARGET_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight();
   private @Nullable Path currentPath;
   private @Nullable Vec3 targetLocation;
   private boolean clockwise;

   public DragonHoldingPatternPhase(final EnderDragon dragon) {
      super(dragon);
   }

   @Override
   public EnderDragonPhase<DragonHoldingPatternPhase> getPhase() {
      return EnderDragonPhase.HOLDING_PATTERN;
   }

   @Override
   public void doServerTick(final ServerLevel level) {
      double distToTarget = this.targetLocation == null ? 0.0 : this.targetLocation.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
      if (distToTarget < 100.0 || distToTarget > 22500.0 || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
         this.findNewTarget(level);
      }
   }

   @Override
   public void begin() {
      this.currentPath = null;
      this.targetLocation = null;
   }

   @Override
   public @Nullable Vec3 getFlyTargetLocation() {
      return this.targetLocation;
   }

   private void findNewTarget(final ServerLevel level) {
      if (this.currentPath != null && this.currentPath.isDone()) {
         BlockPos egg = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EnderDragonFight.getPodiumLocation(this.dragon.getFightOrigin()));
         int crystals = this.dragon.getDragonFight() == null ? 0 : this.dragon.getDragonFight().aliveCrystals();
         if (this.dragon.getRandom().nextInt(crystals + 3) == 0) {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING_APPROACH);
            return;
         }

         Player playerNearestToEgg = level.getNearestPlayer(NEW_TARGET_TARGETING, this.dragon, egg.getX(), egg.getY(), egg.getZ());
         double distSqr;
         if (playerNearestToEgg != null) {
            distSqr = egg.distToCenterSqr(playerNearestToEgg.position()) / 512.0;
         } else {
            distSqr = 64.0;
         }

         if (playerNearestToEgg != null && (this.dragon.getRandom().nextInt((int)(distSqr + 2.0)) == 0 || this.dragon.getRandom().nextInt(crystals + 2) == 0)) {
            this.strafePlayer(playerNearestToEgg);
            return;
         }
      }

      if (this.currentPath == null || this.currentPath.isDone()) {
         int currentNodeIndex = this.dragon.findClosestNode();
         int targetNodeIndex = currentNodeIndex;
         if (this.dragon.getRandom().nextInt(8) == 0) {
            this.clockwise = !this.clockwise;
            targetNodeIndex += 6;
         }

         if (this.clockwise) {
            targetNodeIndex++;
         } else {
            targetNodeIndex--;
         }

         if (this.dragon.getDragonFight() != null && this.dragon.getDragonFight().aliveCrystals() >= 0) {
            targetNodeIndex %= 12;
            if (targetNodeIndex < 0) {
               targetNodeIndex += 12;
            }
         } else {
            targetNodeIndex -= 12;
            targetNodeIndex &= 7;
            targetNodeIndex += 12;
         }

         this.currentPath = this.dragon.findPath(currentNodeIndex, targetNodeIndex, null);
         if (this.currentPath != null) {
            this.currentPath.advance();
         }
      }

      this.navigateToNextPathNode();
   }

   private void strafePlayer(final Player playerNearestToEgg) {
      this.dragon.getPhaseManager().setPhase(EnderDragonPhase.STRAFE_PLAYER);
      this.dragon.getPhaseManager().getPhase(EnderDragonPhase.STRAFE_PLAYER).setTarget(playerNearestToEgg);
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

   @Override
   public void onCrystalDestroyed(final EndCrystal crystal, final BlockPos pos, final DamageSource source, final @Nullable Player player) {
      if (player != null && this.dragon.canAttack(player)) {
         this.strafePlayer(player);
      }
   }
}
