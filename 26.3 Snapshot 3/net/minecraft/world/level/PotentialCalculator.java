package net.minecraft.world.level;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;

public class PotentialCalculator {
   private final List<PotentialCalculator.PointCharge> charges = Lists.newArrayList();

   public void addCharge(final BlockPos pos, final double charge) {
      if (charge != 0.0) {
         this.charges.add(new PotentialCalculator.PointCharge(pos, charge));
      }
   }

   public double getPotentialEnergyChange(final BlockPos pos, final double charge) {
      if (charge == 0.0) {
         return 0.0;
      }

      double potentialChange = 0.0;

      for (PotentialCalculator.PointCharge point : this.charges) {
         potentialChange += point.getPotentialChange(pos);
      }

      return potentialChange * charge;
   }

   private static class PointCharge {
      private final BlockPos pos;
      private final double charge;

      public PointCharge(final BlockPos pos, final double charge) {
         this.pos = pos;
         this.charge = charge;
      }

      public double getPotentialChange(final BlockPos pos) {
         double distSqr = this.pos.distSqr(pos);
         return distSqr == 0.0 ? Double.POSITIVE_INFINITY : this.charge / Math.sqrt(distSqr);
      }
   }
}
