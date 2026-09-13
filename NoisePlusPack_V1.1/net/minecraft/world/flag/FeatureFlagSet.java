package net.minecraft.world.flag;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.Collection;
import org.jspecify.annotations.Nullable;

public final class FeatureFlagSet {
   private static final FeatureFlagSet EMPTY = new FeatureFlagSet(null, 0L);
   public static final int MAX_CONTAINER_SIZE = 64;
   private final @Nullable FeatureFlagUniverse universe;
   private final long mask;

   private FeatureFlagSet(@Nullable FeatureFlagUniverse p_250433_, long p_251523_) {
      this.universe = p_250433_;
      this.mask = p_251523_;
   }

   static FeatureFlagSet create(FeatureFlagUniverse p_251573_, Collection<FeatureFlag> p_251037_) {
      if (p_251037_.isEmpty()) {
         return EMPTY;
      }

      long i = computeMask(p_251573_, 0L, p_251037_);
      return new FeatureFlagSet(p_251573_, i);
   }

   public static FeatureFlagSet of() {
      return EMPTY;
   }

   public static FeatureFlagSet of(FeatureFlag p_252331_) {
      return new FeatureFlagSet(p_252331_.universe, p_252331_.mask);
   }

   public static FeatureFlagSet of(FeatureFlag p_251008_, FeatureFlag... p_249805_) {
      long i = p_249805_.length == 0 ? p_251008_.mask : computeMask(p_251008_.universe, p_251008_.mask, Arrays.asList(p_249805_));
      return new FeatureFlagSet(p_251008_.universe, i);
   }

   private static long computeMask(FeatureFlagUniverse p_249684_, long p_250982_, Iterable<FeatureFlag> p_251734_) {
      for (FeatureFlag featureflag : p_251734_) {
         if (p_249684_ != featureflag.universe) {
            throw new IllegalStateException("Mismatched feature universe, expected '" + p_249684_ + "', but got '" + featureflag.universe + "'");
         }

         p_250982_ |= featureflag.mask;
      }

      return p_250982_;
   }

   public boolean contains(FeatureFlag p_249521_) {
      return this.universe != p_249521_.universe ? false : (this.mask & p_249521_.mask) != 0L;
   }

   public boolean isEmpty() {
      return this.equals(EMPTY);
   }

   public boolean isSubsetOf(FeatureFlagSet p_249164_) {
      if (this.universe == null) {
         return true;
      } else {
         return this.universe != p_249164_.universe ? false : (this.mask & ~p_249164_.mask) == 0L;
      }
   }

   public boolean intersects(FeatureFlagSet p_332490_) {
      return this.universe != null && p_332490_.universe != null && this.universe == p_332490_.universe ? (this.mask & p_332490_.mask) != 0L : false;
   }

   public FeatureFlagSet join(FeatureFlagSet p_251527_) {
      if (this.universe == null) {
         return p_251527_;
      } else if (p_251527_.universe == null) {
         return this;
      } else if (this.universe != p_251527_.universe) {
         throw new IllegalArgumentException("Mismatched set elements: '" + this.universe + "' != '" + p_251527_.universe + "'");
      } else {
         return new FeatureFlagSet(this.universe, this.mask | p_251527_.mask);
      }
   }

   public FeatureFlagSet subtract(FeatureFlagSet p_328325_) {
      if (this.universe == null || p_328325_.universe == null) {
         return this;
      }

      if (this.universe != p_328325_.universe) {
         throw new IllegalArgumentException("Mismatched set elements: '" + this.universe + "' != '" + p_328325_.universe + "'");
      }

      long i = this.mask & ~p_328325_.mask;
      return i == 0L ? EMPTY : new FeatureFlagSet(this.universe, i);
   }

   @Override
   public boolean equals(Object p_248691_) {
      return this == p_248691_
         ? true
         : p_248691_ instanceof FeatureFlagSet featureflagset && this.universe == featureflagset.universe && this.mask == featureflagset.mask;
   }

   @Override
   public int hashCode() {
      return (int)HashCommon.mix(this.mask);
   }
}
