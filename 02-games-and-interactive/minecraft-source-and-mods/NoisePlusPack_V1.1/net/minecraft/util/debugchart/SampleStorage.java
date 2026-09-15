package net.minecraft.util.debugchart;

public interface SampleStorage {
   int capacity();

   int size();

   long get(int var1);

   long get(int var1, int var2);

   void reset();
}
