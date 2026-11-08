package net.minecraft.world.level.chunk;

public interface CarverOutput {
   void carve(int x, int y, int z);

   int minY();

   int maxY();
}
