package net.minecraft.world.level.entity;

import net.minecraft.world.entity.Entity;

public interface EntityInLevelCallback {
   EntityInLevelCallback NULL = new EntityInLevelCallback() {
      @Override
      public void onMove() {
      }

      @Override
      public void onRemove(Entity.RemovalReason p_156805_) {
      }
   };

   void onMove();

   void onRemove(Entity.RemovalReason var1);
}
