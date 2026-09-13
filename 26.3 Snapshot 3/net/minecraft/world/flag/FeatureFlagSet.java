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

   private FeatureFlagSet(final @Nullable FeatureFlagUniverse universe, final long mask) {
      this.universe = universe;
      this.mask = mask;
   }

   static FeatureFlagSet create(final FeatureFlagUniverse universe, final Collection<FeatureFlag> flags) {
      if (flags.isEmpty()) {
         return EMPTY;
      }

      long mask = computeMask(universe, 0L, flags);
      return new FeatureFlagSet(universe, mask);
   }

   public static FeatureFlagSet of() {
      return EMPTY;
   }

   public static FeatureFlagSet of(final FeatureFlag flag) {
      return new FeatureFlagSet(flag.universe, flag.mask);
   }

   public static FeatureFlagSet of(final FeatureFlag flag, final FeatureFlag... flags) {
      long mask = flags.length == 0 ? flag.mask : computeMask(flag.universe, flag.mask, Arrays.asList(flags));
      return new FeatureFlagSet(flag.universe, mask);
   }

   private static long computeMask(final FeatureFlagUniverse universe, long mask, final Iterable<FeatureFlag> flags) {
      for (FeatureFlag f : flags) {
         if (universe != f.universe) {
            throw new IllegalStateException("Mismatched feature universe, expected '" + universe + "', but got '" + f.universe + "'");
         }

         mask |= f.mask;
      }

      return mask;
   }

   public boolean contains(final FeatureFlag flag) {
      return this.universe != flag.universe ? false : (this.mask & flag.mask) != 0L;
   }

   public boolean isEmpty() {
      return this.equals(EMPTY);
   }

   public boolean isSubsetOf(final FeatureFlagSet set) {
      if (this.universe == null) {
         return true;
      } else {
         return this.universe != set.universe ? false : (this.mask & ~set.mask) == 0L;
      }
   }

   public boolean intersects(final FeatureFlagSet set) {
      return this.universe != null && set.universe != null && this.universe == set.universe ? (this.mask & set.mask) != 0L : false;
   }

   public FeatureFlagSet join(final FeatureFlagSet other) {
      if (this.universe == null) {
         return other;
      } else if (other.universe == null) {
         return this;
      } else if (this.universe != other.universe) {
         throw new IllegalArgumentException("Mismatched set elements: '" + this.universe + "' != '" + other.universe + "'");
      } else {
         return new FeatureFlagSet(this.universe, this.mask | other.mask);
      }
   }

   public FeatureFlagSet subtract(final FeatureFlagSet other) {
      if (this.universe == null || other.universe == null) {
         return this;
      }

      if (this.universe != other.universe) {
         throw new IllegalArgumentException("Mismatched set elements: '" + this.universe + "' != '" + other.universe + "'");
      }

      long newMask = this.mask & ~other.mask;
      return newMask == 0L ? EMPTY : new FeatureFlagSet(this.universe, newMask);
   }

   @Override
   public boolean equals(final Object o) {
      return this == o ? true : o instanceof FeatureFlagSet that && this.universe == that.universe && this.mask == that.mask;
   }

   @Override
   public int hashCode() {
      return (int)HashCommon.mix(this.mask);
   }
}
