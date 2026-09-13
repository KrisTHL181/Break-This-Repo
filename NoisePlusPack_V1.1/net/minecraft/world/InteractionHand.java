package net.minecraft.world;

import net.minecraft.world.entity.EquipmentSlot;

public enum InteractionHand {
   MAIN_HAND,
   OFF_HAND;

   public EquipmentSlot asEquipmentSlot() {
      return this == MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
   }
}
