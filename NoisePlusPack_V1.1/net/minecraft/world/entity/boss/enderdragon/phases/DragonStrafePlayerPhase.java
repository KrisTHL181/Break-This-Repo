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

   public DragonStrafePlayerPhase(EnderDragon p_31357_) {
      super(p_31357_);
   }

   @Override
   public void doServerTick(ServerLevel p_362629_) {
      if (this.attackTarget == null) {
         LOGGER.warn("Skipping player strafe phase because no player was found");
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
      } else {
         if (this.currentPath != null && this.currentPath.isDone()) {
            double d0 = this.attackTarget.getX();
            double d1 = this.attackTarget.getZ();
            double d2 = d0 - this.dragon.getX();
            double d3 = d1 - this.dragon.getZ();
            double d4 = Math.sqrt(d2 * d2 + d3 * d3);
            double d5 = Math.min(0.4F + d4 / 80.0 - 1.0, 10.0);
            this.targetLocation = new Vec3(d0, this.attackTarget.getY() + d5, d1);
         }

         double d12 = this.targetLocation == null ? 0.0 : this.targetLocation.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
         if (d12 < 100.0 || d12 > 22500.0) {
            this.findNewTarget();
         }

         double d13 = 64.0;
         if (this.attackTarget.distanceToSqr(this.dragon) < 4096.0) {
            if (this.dragon.hasLineOfSight(this.attackTarget)) {
               this.fireballCharge++;
               Vec3 vec32 = new Vec3(this.attackTarget.getX() - this.dragon.getX(), 0.0, this.attackTarget.getZ() - this.dragon.getZ()).normalize();
               Vec3 vec3 = new Vec3(
                     Mth.sin(this.dragon.getYRot() * (float) (Math.PI / 180.0)), 0.0, -Mth.cos(this.dragon.getYRot() * (float) (Math.PI / 180.0))
                  )
                  .normalize();
               float f1 = (float)vec3.dot(vec32);
               float f = (float)(Math.acos(f1) * 180.0 / (float) Math.PI);
               f += 0.5F;
               if (this.fireballCharge >= 5 && f >= 0.0F && f < 10.0F) {
                  double d14 = 1.0;
                  Vec3 vec33 = this.dragon.getViewVector(1.0F);
                  double d6 = this.dragon.head.getX() - vec33.x * 1.0;
                  double d7 = this.dragon.head.getY(0.5) + 0.5;
                  double d8 = this.dragon.head.getZ() - vec33.z * 1.0;
                  double d9 = this.attackTarget.getX() - d6;
                  double d10 = this.attackTarget.getY(0.5) - d7;
                  double d11 = this.attackTarget.getZ() - d8;
                  Vec3 vec31 = new Vec3(d9, d10, d11);
                  if (!this.dragon.isSilent()) {
                     p_362629_.levelEvent(null, 1017, this.dragon.blockPosition(), 0);
                  }

                  DragonFireball dragonfireball = new DragonFireball(p_362629_, this.dragon, vec31.normalize());
                  dragonfireball.snapTo(d6, d7, d8, 0.0F, 0.0F);
                  p_362629_.addFreshEntity(dragonfireball);
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
         int i = this.dragon.findClosestNode();
         int j = i;
         if (this.dragon.getRandom().nextInt(8) == 0) {
            this.holdingPatternClockwise = !this.holdingPatternClockwise;
            j = i + 6;
         }

         if (this.holdingPatternClockwise) {
            j++;
         } else {
            j--;
         }

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
         if (this.currentPath != null) {
            this.currentPath.advance();
         }
      }

      this.navigateToNextPathNode();
   }

   private void navigateToNextPathNode() {
      if (this.currentPath != null && !this.currentPath.isDone()) {
         Vec3i vec3i = this.currentPath.getNextNodePos();
         this.currentPath.advance();
         double d0 = vec3i.getX();
         double d2 = vec3i.getZ();

         double d1;
         do {
            d1 = vec3i.getY() + this.dragon.getRandom().nextFloat() * 20.0F;
         } while (d1 < vec3i.getY());

         this.targetLocation = new Vec3(d0, d1, d2);
      }
   }

   @Override
   public void begin() {
      this.fireballCharge = 0;
      this.targetLocation = null;
      this.currentPath = null;
      this.attackTarget = null;
   }

   public void setTarget(LivingEntity p_31359_) {
      this.attackTarget = p_31359_;
      int i = this.dragon.findClosestNode();
      int j = this.dragon.findClosestNode(this.attackTarget.getX(), this.attackTarget.getY(), this.attackTarget.getZ());
      int k = this.attackTarget.getBlockX();
      int l = this.attackTarget.getBlockZ();
      double d0 = k - this.dragon.getX();
      double d1 = l - this.dragon.getZ();
      double d2 = Math.sqrt(d0 * d0 + d1 * d1);
      double d3 = Math.min(0.4F + d2 / 80.0 - 1.0, 10.0);
      int i1 = Mth.floor(this.attackTarget.getY() + d3);
      Node node = new Node(k, i1, l);
      this.currentPath = this.dragon.findPath(i, j, node);
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
