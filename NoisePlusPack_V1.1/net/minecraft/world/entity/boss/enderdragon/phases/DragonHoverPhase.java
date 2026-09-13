package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DragonHoverPhase extends AbstractDragonPhaseInstance {
   private @Nullable Vec3 targetLocation;

   public DragonHoverPhase(EnderDragon p_31246_) {
      super(p_31246_);
   }

   @Override
   public void doServerTick(ServerLevel p_364623_) {
      if (this.targetLocation == null) {
         this.targetLocation = this.dragon.position();
      }
   }

   @Override
   public boolean isSitting() {
      return true;
   }

   @Override
   public void begin() {
      this.targetLocation = null;
   }

   @Override
   public float getFlySpeed() {
      return 1.0F;
   }

   @Override
   public @Nullable Vec3 getFlyTargetLocation() {
      return this.targetLocation;
   }

   @Override
   public EnderDragonPhase<DragonHoverPhase> getPhase() {
      return EnderDragonPhase.HOVERING;
   }
}
