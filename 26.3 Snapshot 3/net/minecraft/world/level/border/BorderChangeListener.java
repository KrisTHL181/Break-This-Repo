package net.minecraft.world.level.border;

public interface BorderChangeListener {
   void onSetSize(WorldBorder border, double newSize);

   void onLerpSize(WorldBorder border, double fromSize, double targetSize, long ticks, long gameTime);

   void onSetCenter(WorldBorder border, double x, double z);

   void onSetWarningTime(WorldBorder border, int time);

   void onSetWarningBlocks(WorldBorder border, int blocks);

   void onSetDamagePerBlock(WorldBorder border, double damagePerBlock);

   void onSetSafeZone(WorldBorder border, double safeZone);
}
