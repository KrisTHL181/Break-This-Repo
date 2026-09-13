package net.minecraft.client.resources.palette;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntUnaryOperator;
import net.minecraft.util.ARGB;

public record PaletteMapping(Int2IntMap palette) implements IntUnaryOperator {
   public static final PaletteMapping NONE = new PaletteMapping(Int2IntMaps.EMPTY_MAP);

   public static PaletteMapping create(final Palette base, final Palette target) {
      if (target.size() != base.size()) {
         throw new IllegalArgumentException("PaletteMapping has different sizes: " + base.size() + " != " + target.size());
      }

      Int2IntMap palette = new Int2IntOpenHashMap(base.size());

      for (int i = 0; i < base.size(); i++) {
         int key = base.get(i);
         if (ARGB.alpha(key) != 0) {
            palette.put(ARGB.opaque(key), target.get(i));
         }
      }

      return new PaletteMapping(palette);
   }

   public int apply(final int baseColor) {
      int baseAlpha = ARGB.alpha(baseColor);
      if (baseAlpha == 0) {
         return baseColor;
      }

      int baseRgb = ARGB.opaque(baseColor);
      int targetRgb = this.palette.getOrDefault(baseRgb, baseRgb);
      int valueAlpha = ARGB.alpha(targetRgb);
      return ARGB.color(baseAlpha * valueAlpha / 255, targetRgb);
   }
}
