package net.minecraft.world.level.chunk;

public interface PaletteResize<T> {
   int onResize(int bits, T lastAddedValue);

   static <T> PaletteResize<T> noResizeExpected() {
      return (bits, lastAddedValue) -> {
         throw new IllegalArgumentException("Unexpected palette resize, bits = " + bits + ", added value = " + lastAddedValue);
      };
   }
}
