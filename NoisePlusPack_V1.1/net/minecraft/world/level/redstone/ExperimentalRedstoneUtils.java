package net.minecraft.world.level.redstone;

import net.minecraft.core.Direction;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ExperimentalRedstoneUtils {
   public static @Nullable Orientation initialOrientation(Level p_360797_, @Nullable Direction p_367898_, @Nullable Direction p_368279_) {
      if (p_360797_.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS)) {
         Orientation orientation = Orientation.random(p_360797_.random).withSideBias(Orientation.SideBias.LEFT);
         if (p_368279_ != null) {
            orientation = orientation.withUp(p_368279_);
         }

         if (p_367898_ != null) {
            orientation = orientation.withFront(p_367898_);
         }

         return orientation;
      } else {
         return null;
      }
   }

   public static @Nullable Orientation withFront(@Nullable Orientation p_367198_, Direction p_365285_) {
      return p_367198_ == null ? null : p_367198_.withFront(p_365285_);
   }
}
