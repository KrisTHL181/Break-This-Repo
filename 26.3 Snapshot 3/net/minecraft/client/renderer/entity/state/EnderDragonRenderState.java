package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

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

   public DragonFlightHistory.Sample getHistoricalPos(final int delay) {
      return this.flightHistory.get(delay, this.partialTicks);
   }

   public float getHeadPartYOffset(final int part, final DragonFlightHistory.Sample bodyPos, final DragonFlightHistory.Sample partPos) {
      double result;
      if (this.isLandingOrTakingOff) {
         result = part / Math.max(this.distanceToEgg / 4.0, 1.0);
      } else if (this.isSitting) {
         result = part;
      } else if (part == 6) {
         result = 0.0;
      } else {
         result = partPos.y() - bodyPos.y();
      }

      return (float)result;
   }
}
