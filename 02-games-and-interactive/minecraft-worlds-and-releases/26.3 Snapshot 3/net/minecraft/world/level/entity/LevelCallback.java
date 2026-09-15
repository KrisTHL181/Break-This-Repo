package net.minecraft.world.level.entity;

public interface LevelCallback<T> {
   void onCreated(T entity);

   void onDestroyed(T entity);

   void onTickingStart(T entity);

   void onTickingEnd(T entity);

   void onTrackingStart(T entity);

   void onTrackingEnd(T entity);

   void onSectionChange(T entity);
}
