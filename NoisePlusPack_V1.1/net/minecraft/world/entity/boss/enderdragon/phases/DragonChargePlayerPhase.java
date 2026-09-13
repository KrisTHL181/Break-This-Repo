package net.minecraft.world.entity.boss.enderdragon.phases;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class DragonChargePlayerPhase extends AbstractDragonPhaseInstance {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int CHARGE_RECOVERY_TIME = 10;
   private @Nullable Vec3 targetLocation;
   private int timeSinceCharge;

   public DragonChargePlayerPhase(EnderDragon p_31206_) {
      super(p_31206_);
   }

   @Override
   public void doServerTick(ServerLevel p_369998_) {
      if (this.targetLocation == null) {
         LOGGER.warn("Aborting charge player as no target was set.");
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
      } else if (this.timeSinceCharge > 0 && this.timeSinceCharge++ >= 10) {
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
      } else {
         double d0 = this.targetLocation.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
         if (d0 < 100.0 || d0 > 22500.0 || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
            this.timeSinceCharge++;
         }
      }
   }

   @Override
   public void begin() {
      this.targetLocation = null;
      this.timeSinceCharge = 0;
   }

   public void setTarget(Vec3 p_31208_) {
      this.targetLocation = p_31208_;
   }

   @Override
   public float getFlySpeed() {
      return 3.0F;
   }

   @Override
   public @Nullable Vec3 getFlyTargetLocation() {
      return this.targetLocation;
   }

   @Override
   public EnderDragonPhase<DragonChargePlayerPhase> getPhase() {
      return EnderDragonPhase.CHARGING_PLAYER;
   }
}
