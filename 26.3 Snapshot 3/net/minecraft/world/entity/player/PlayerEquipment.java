package net.minecraft.world.entity.player;

import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class PlayerEquipment extends EntityEquipment {
   private final Player player;

   public PlayerEquipment(final Player player) {
      this.player = player;
   }

   @Override
   public ItemStack set(final EquipmentSlot slot, final ItemStack itemStack) {
      return slot == EquipmentSlot.MAINHAND ? this.player.getInventory().setSelectedItem(itemStack) : super.set(slot, itemStack);
   }

   @Override
   public ItemStack get(final EquipmentSlot slot) {
      return slot == EquipmentSlot.MAINHAND ? this.player.getInventory().getSelectedItem() : super.get(slot);
   }

   @Override
   public boolean isEmpty() {
      return this.player.getInventory().getSelectedItem().isEmpty() && super.isEmpty();
   }
}
