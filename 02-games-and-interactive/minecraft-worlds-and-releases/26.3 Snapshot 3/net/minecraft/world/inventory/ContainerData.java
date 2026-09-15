package net.minecraft.world.inventory;

public interface ContainerData {
   int get(final int dataId);

   void set(final int dataId, final int value);

   int getCount();
}
