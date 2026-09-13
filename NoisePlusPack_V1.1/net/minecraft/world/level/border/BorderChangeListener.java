package net.minecraft.world.level.border;

public interface BorderChangeListener {
   void onSetSize(WorldBorder var1, double var2);

   void onLerpSize(WorldBorder var1, double var2, double var4, long var6, long var8);

   void onSetCenter(WorldBorder var1, double var2, double var4);

   void onSetWarningTime(WorldBorder var1, int var2);

   void onSetWarningBlocks(WorldBorder var1, int var2);

   void onSetDamagePerBlock(WorldBorder var1, double var2);

   void onSetSafeZone(WorldBorder var1, double var2);
}
