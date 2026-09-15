package net.minecraft.world.level.entity;

public interface LevelCallback<T> {
   void onCreated(T var1);

   void onDestroyed(T var1);

   void onTickingStart(T var1);

   void onTickingEnd(T var1);

   void onTrackingStart(T var1);

   void onTrackingEnd(T var1);

   void onSectionChange(T var1);
}
