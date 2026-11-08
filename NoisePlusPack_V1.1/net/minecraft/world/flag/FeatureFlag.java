package net.minecraft.world.flag;

public class FeatureFlag {
   final FeatureFlagUniverse universe;
   final long mask;

   FeatureFlag(FeatureFlagUniverse p_249115_, int p_251067_) {
      this.universe = p_249115_;
      this.mask = 1L << p_251067_;
   }
}
