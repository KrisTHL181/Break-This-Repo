package net.minecraft.world.entity.boss.enderdragon.phases;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class DragonStrafePlayerPhase extends AbstractDragonPhaseInstance {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int FIREBALL_CHARGE_AMOUNT = 5;
   private int fireballCharge;
   private @Nullable Path currentPath;
   private @Nullable Vec3 targetLocation;
   private @Nullable LivingEntity attackTarget;
   private boolean holdingPatternClockwise;

   public DragonStrafePlayerPhase(final EnderDragon dragon) {
      super(dragon);
   }

   @Override
   public void doServerTick(final ServerLevel level) {
      if (this.attackTarget == null) {
         LOGGER.warn("Skipping player strafe phase because no player was found");
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
      } else {
         if (this.currentPath != null && this.currentPath.isDone()) {
            double xTarget = this.attackTarget.getX();
            double zTarget = this.attackTarget.getZ();
            double xTargetDist = xTarget - this.dragon.getX();
            double zTargetDist = zTarget - this.dragon.getZ();
            double dist = Math.sqrt(xTargetDist * xTargetDist + zTargetDist * zTargetDist);
            double heightOffset = Math.min(0.4F + dist / 80.0 - 1.0, 10.0);
            this.targetLocation = new Vec3(xTarget, this.attackTarget.getY() + heightOffset, zTarget);
         }

         double distToTarget = this.targetLocation == null
            ? 0.0
            : this.targetLocation.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
         if (distToTarget < 100.0 || distToTarget > 22500.0) {
            this.findNewTarget();
         }

         double maxDist = 64.0;
         if (this.attackTarget.distanceToSqr(this.dragon) < 4096.0) {
            if (this.dragon.hasLineOfSight(this.attackTarget)) {
               this.fireballCharge++;
               Vec3 aim = new Vec3(this.attackTarget.getX() - this.dragon.getX(), 0.0, this.attackTarget.getZ() - this.dragon.getZ()).normalize();
               Vec3 dir = new Vec3(Mth.sin(this.dragon.getYRot() * (float) (Math.PI / 180.0)), 0.0, -Mth.cos(this.dragon.getYRot() * (float) (Math.PI / 180.0)))
                  .normalize();
               float dot = (float)dir.dot(aim);
               float angleDegs = (float)(Math.acos(dot) * 180.0F / (float)Math.PI);
               angleDegs += 0.5F;
               if (this.fireballCharge >= 5 && angleDegs >= 0.0F && angleDegs < 10.0F) {
                  double d = 1.0;
                  Vec3 viewVector = this.dragon.getViewVector(1.0F);
                  double startingX = this.dragon.head.getX() - viewVector.x * 1.0;
                  double startingY = this.dragon.head.getY(0.5) + 0.5;
                  double startingZ = this.dragon.head.getZ() - viewVector.z * 1.0;
                  double xdd = this.attackTarget.getX() - startingX;
                  double ydd = this.attackTarget.getY(0.5) - startingY;
                  double zdd = this.attackTarget.getZ() - startingZ;
                  Vec3 direction = new Vec3(xdd, ydd, zdd);
                  if (!this.dragon.isSilent()) {
                     level.levelEvent(null, 1017, this.dragon.blockPosition(), 0);
                  }

                  DragonFireball entity = new DragonFireball(level, this.dragon, direction.normalize());
                  entity.snapTo(startingX, startingY, startingZ, 0.0F, 0.0F);
                  level.addFreshEntity(entity);
                  this.fireballCharge = 0;
                  if (this.currentPath != null) {
                     while (!this.currentPath.isDone()) {
                        this.currentPath.advance();
                     }
                  }

                  this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
               }
            } else if (this.fireballCharge > 0) {
               this.fireballCharge--;
            }
         } else if (this.fireballCharge > 0) {
            this.fireballCharge--;
         }
      }
   }

   private void findNewTarget() {
      if (this.currentPath == null || this.currentPath.isDone()) {
         int currentNodeIndex = this.dragon.findClosestNode();
         int targetNodeIndex = currentNodeIndex;
         if (this.dragon.getRandom().nextInt(8) == 0) {
            this.holdingPatternClockwise = !this.holdingPatternClockwise;
            targetNodeIndex += 6;
         }

         if (this.holdingPatternClockwise) {
            targetNodeIndex++;
         } else {
            targetNodeIndex--;
         }

         if (this.dragon.getDragonFight() != null && this.dragon.getDragonFight().aliveCrystals() > 0) {
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
   public void begin() {
      this.fireballCharge = 0;
      this.targetLocation = null;
      this.currentPath = null;
      this.attackTarget = null;
   }

   public void setTarget(final LivingEntity target) {
      this.attackTarget = target;
      int currentNodeIndex = this.dragon.findClosestNode();
      int targetNodeIndex = this.dragon.findClosestNode(this.attackTarget.getX(), this.attackTarget.getY(), this.attackTarget.getZ());
      int finalXTarget = this.attackTarget.getBlockX();
      int finalZTarget = this.attackTarget.getBlockZ();
      double xd = finalXTarget - this.dragon.getX();
      double zd = finalZTarget - this.dragon.getZ();
      double sd = Math.sqrt(xd * xd + zd * zd);
      double ho = Math.min(0.4F + sd / 80.0 - 1.0, 10.0);
      int finalYTarget = Mth.floor(this.attackTarget.getY() + ho);
      Node finalNode = new Node(finalXTarget, finalYTarget, finalZTarget);
      this.currentPath = this.dragon.findPath(currentNodeIndex, targetNodeIndex, finalNode);
      if (this.currentPath != null) {
         this.currentPath.advance();
         this.navigateToNextPathNode();
      }
   }

   @Override
   public @Nullable Vec3 getFlyTargetLocation() {
      return this.targetLocation;
   }

   @Override
   public EnderDragonPhase<DragonStrafePlayerPhase> getPhase() {
      return EnderDragonPhase.STRAFE_PLAYER;
   }
}
