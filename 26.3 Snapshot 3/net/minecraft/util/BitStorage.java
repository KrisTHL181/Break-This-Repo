package net.minecraft.util;

import java.util.function.IntConsumer;

public interface BitStorage {
   int getAndSet(int index, int value);

   void set(int index, int value);

   int get(int index);

   long[] getRaw();

   int getSize();

   int getBits();

   void getAll(IntConsumer output);

   void unpack(int[] output);

   BitStorage copy();
}
