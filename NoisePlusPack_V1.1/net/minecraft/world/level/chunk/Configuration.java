package net.minecraft.world.level.chunk;

import java.util.List;

public interface Configuration {
   boolean alwaysRepack();

   int bitsInMemory();

   int bitsInStorage();

   <T> Palette<T> createPalette(Strategy<T> var1, List<T> var2);

   record Global(int bitsInMemory, int bitsInStorage) implements Configuration {
      @Override
      public boolean alwaysRepack() {
         return true;
      }

      @Override
      public <T> Palette<T> createPalette(Strategy<T> p_424848_, List<T> p_430068_) {
         return p_424848_.globalPalette();
      }
   }

   record Simple(Palette.Factory factory, int bits) implements Configuration {
      @Override
      public boolean alwaysRepack() {
         return false;
      }

      @Override
      public <T> Palette<T> createPalette(Strategy<T> p_430764_, List<T> p_431147_) {
         return this.factory.create(this.bits, p_431147_);
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
