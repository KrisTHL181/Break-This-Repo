package com.mojang.blaze3d.platform;

public record Transparency(boolean hasTransparent, boolean hasTranslucent) {
   public static final Transparency NONE = new Transparency(false, false);
   public static final Transparency TRANSPARENT = new Transparency(true, false);
   public static final Transparency TRANSLUCENT = new Transparency(false, true);
   public static final Transparency TRANSPARENT_AND_TRANSLUCENT = new Transparency(true, true);

   public static Transparency of(final boolean hasTransparent, final boolean hasTranslucent) {
      if (hasTransparent && hasTranslucent) {
         return TRANSPARENT_AND_TRANSLUCENT;
      } else if (hasTransparent) {
         return TRANSPARENT;
      } else {
         return hasTranslucent ? TRANSLUCENT : NONE;
      }
   }

   public Transparency or(final Transparency other) {
      return of(this.hasTransparent || other.hasTransparent, this.hasTranslucent || other.hasTranslucent);
   }

   public boolean isOpaque() {
      return !this.hasTransparent && !this.hasTranslucent;
   }
}
