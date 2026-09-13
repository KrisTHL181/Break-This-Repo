package net.minecraft.world.level.chunk;

public interface PaletteResize<T> {
   int onResize(int var1, T var2);

   static <T> PaletteResize<T> noResizeExpected() {
      return (p_431054_, p_427407_) -> {
         throw new IllegalArgumentException("Unexpected palette resize, bits = " + p_431054_ + ", added value = " + p_427407_);
      };
   }
}
