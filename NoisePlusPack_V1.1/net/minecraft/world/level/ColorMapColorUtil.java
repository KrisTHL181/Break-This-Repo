package net.minecraft.world.level;

public interface ColorMapColorUtil {
   static int get(double p_395315_, double p_395796_, int[] p_396507_, int p_395933_) {
      p_395796_ *= p_395315_;
      int i = (int)((1.0 - p_395315_) * 255.0);
      int j = (int)((1.0 - p_395796_) * 255.0);
      int k = j << 8 | i;
      return k >= p_396507_.length ? p_395933_ : p_396507_[k];
   }
}
