package net.minecraft.util;

import java.util.function.IntConsumer;

public interface BitStorage {
   int getAndSet(int var1, int var2);

   void set(int var1, int var2);

   int get(int var1);

   long[] getRaw();

   int getSize();

   int getBits();

   void getAll(IntConsumer var1);

   void unpack(int[] var1);

   BitStorage copy();
}
