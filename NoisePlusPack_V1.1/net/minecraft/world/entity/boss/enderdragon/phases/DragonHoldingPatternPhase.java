package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DragonHoldingPatternPhase extends AbstractDragonPhaseInstance {
   private static final TargetingConditions NEW_TARGET_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight();
   private @Nullable Path currentPath;
   private @Nullable Vec3 targetLocation;
   private boolean clockwise;

   public DragonHoldingPatternPhase(EnderDragon p_31230_) {
      super(p_31230_);
   }

   @Override
   public EnderDragonPhase<DragonHoldingPatternPhase> getPhase() {
      return EnderDragonPhase.HOLDING_PATTERN;
   }

   @Override
   public void doServerTick(ServerLevel p_366645_) {
      double d0 = this.targetLocation == null ? 0.0 : this.targetLocation.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
      if (d0 < 100.0 || d0 > 22500.0 || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
         this.findNewTarget(p_366645_);
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

   private void findNewTarget(ServerLevel p_369217_) {
      if (this.currentPath != null && this.currentPath.isDone()) {
         BlockPos blockpos = p_369217_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(this.dragon.getFightOrigin()));
         int i = this.dragon.getDragonFight() == null ? 0 : this.dragon.getDragonFight().getCrystalsAlive();
         if (this.dragon.getRandom().nextInt(i + 3) == 0) {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING_APPROACH);
            return;
         }

         Player player = p_369217_.getNearestPlayer(NEW_TARGET_TARGETING, this.dragon, blockpos.getX(), blockpos.getY(), blockpos.getZ());
         double d0;
         if (player != null) {
            d0 = blockpos.distToCenterSqr(player.position()) / 512.0;
         } else {
            d0 = 64.0;
         }

         if (player != null && (this.dragon.getRandom().nextInt((int)(d0 + 2.0)) == 0 || this.dragon.getRandom().nextInt(i + 2) == 0)) {
            this.strafePlayer(player);
            return;
         }
      }

      if (this.currentPath == null || this.currentPath.isDone()) {
         int j = this.dragon.findClosestNode();
         int k = j;
         if (this.dragon.getRandom().nextInt(8) == 0) {
            this.clockwise = !this.clockwise;
            k = j + 6;
         }

         if (this.clockwise) {
            k++;
         } else {
            k--;
         }

         if (this.dragon.getDragonFight() != null && this.dragon.getDragonFight().getCrystalsAlive() >= 0) {
            k %= 12;
            if (k < 0) {
               k += 12;
            }
         } else {
            k -= 12;
            k &= 7;
            k += 12;
         }

         this.currentPath = this.dragon.findPath(j, k, null);
         if (this.currentPath != null) {
            this.currentPath.advance();
         }
      }

      this.navigateToNextPathNode();
   }

   private void strafePlayer(Player p_31237_) {
      this.dragon.getPhaseManager().setPhase(EnderDragonPhase.STRAFE_PLAYER);
      this.dragon.getPhaseManager().getPhase(EnderDragonPhase.STRAFE_PLAYER).setTarget(p_31237_);
   }

   private void navigateToNextPathNode() {
      if (this.currentPath != null && !this.currentPath.isDone()) {
         Vec3i vec3i = this.currentPath.getNextNodePos();
         this.currentPath.advance();
         double d0 = vec3i.getX();
         double d1 = vec3i.getZ();

         double d2;
         do {
            d2 = vec3i.getY() + this.dragon.getRandom().nextFloat() * 20.0F;
         } while (d2 < vec3i.getY());

         this.targetLocation = new Vec3(d0, d2, d1);
      }
   }

   @Override
   public void onCrystalDestroyed(EndCrystal p_31232_, BlockPos p_31233_, DamageSource p_31234_, @Nullable Player p_31235_) {
      if (p_31235_ != null && this.dragon.canAttack(p_31235_)) {
         this.strafePlayer(p_31235_);
      }
   }
}
