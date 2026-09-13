package net.minecraft.world.entity.player;

import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class PlayerEquipment extends EntityEquipment {
   private final Player player;

   public PlayerEquipment(Player p_396069_) {
      this.player = p_396069_;
   }

   @Override
   public ItemStack set(EquipmentSlot p_395109_, ItemStack p_393751_) {
      return p_395109_ == EquipmentSlot.MAINHAND ? this.player.getInventory().setSelectedItem(p_393751_) : super.set(p_395109_, p_393751_);
   }

   @Override
   public ItemStack get(EquipmentSlot p_396072_) {
      return p_396072_ == EquipmentSlot.MAINHAND ? this.player.getInventory().getSelectedItem() : super.get(p_396072_);
   }

   @Override
   public boolean isEmpty() {
      return this.player.getInventory().getSelectedItem().isEmpty() && super.isEmpty();
   }
}
