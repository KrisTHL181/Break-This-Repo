package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EnderDragonRenderState extends EntityRenderState {
   public float flapTime;
   public float deathTime;
   public boolean hasRedOverlay;
   public @Nullable Vec3 beamOffset;
   public boolean isLandingOrTakingOff;
   public boolean isSitting;
   public double distanceToEgg;
   public float partialTicks;
   public final DragonFlightHistory flightHistory = new DragonFlightHistory();

   public DragonFlightHistory.Sample getHistoricalPos(int p_368884_) {
      return this.flightHistory.get(p_368884_, this.partialTicks);
   }

   public float getHeadPartYOffset(int p_362696_, DragonFlightHistory.Sample p_368463_, DragonFlightHistory.Sample p_361357_) {
      double d0;
      if (this.isLandingOrTakingOff) {
         d0 = p_362696_ / Math.max(this.distanceToEgg / 4.0, 1.0);
      } else if (this.isSitting) {
         d0 = p_362696_;
      } else if (p_362696_ == 6) {
         d0 = 0.0;
      } else {
         d0 = p_361357_.y() - p_368463_.y();
      }

      return (float)d0;
   }
}
