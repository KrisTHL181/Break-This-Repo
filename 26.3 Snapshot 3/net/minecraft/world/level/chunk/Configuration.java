package net.minecraft.world.level.chunk;

import java.util.List;

public interface Configuration {
   boolean alwaysRepack();

   int bitsInMemory();

   int bitsInStorage();

   <T> Palette<T> createPalette(Strategy<T> strategy, List<T> paletteEntries);

   record Global(int bitsInMemory, int bitsInStorage) implements Configuration {
      @Override
      public boolean alwaysRepack() {
         return true;
      }

      @Override
      public <T> Palette<T> createPalette(final Strategy<T> strategy, final List<T> paletteEntries) {
         return strategy.globalPalette();
      }
   }

   record Simple(Palette.Factory factory, int bits) implements Configuration {
      @Override
      public boolean alwaysRepack() {
         return false;
      }

      @Override
      public <T> Palette<T> createPalette(final Strategy<T> strategy, final List<T> paletteEntries) {
         return this.factory.create(this.bits, paletteEntries);
      }

      @Override
      public int bitsInMemory() {
         return this.bits;
      }

      @Override
      public int bitsInStorage() {
         return this.bits;
      }
   }
}
