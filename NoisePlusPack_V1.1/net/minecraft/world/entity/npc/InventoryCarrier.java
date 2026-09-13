package net.minecraft.world.entity.npc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface InventoryCarrier {
   String TAG_INVENTORY = "Inventory";

   SimpleContainer getInventory();

   static void pickUpItem(ServerLevel p_361504_, Mob p_219612_, InventoryCarrier p_219613_, ItemEntity p_219614_) {
      ItemStack itemstack = p_219614_.getItem();
      if (p_219612_.wantsToPickUp(p_361504_, itemstack)) {
         SimpleContainer simplecontainer = p_219613_.getInventory();
         boolean flag = simplecontainer.canAddItem(itemstack);
         if (!flag) {
            return;
         }

         p_219612_.onItemPickup(p_219614_);
         int i = itemstack.getCount();
         ItemStack itemstack1 = simplecontainer.addItem(itemstack);
         p_219612_.take(p_219614_, i - itemstack1.getCount());
         if (itemstack1.isEmpty()) {
            p_219614_.discard();
         } else {
            itemstack.setCount(itemstack1.getCount());
         }
      }
   }

   default void readInventoryFromTag(ValueInput p_406226_) {
      p_406226_.list("Inventory", ItemStack.CODEC).ifPresent(p_405544_ -> this.getInventory().fromItemList((ValueInput.TypedInputList<ItemStack>)p_405544_));
   }

   default void writeInventoryToTag(ValueOutput p_409377_) {
      this.getInventory().storeAsItemList(p_409377_.list("Inventory", ItemStack.CODEC));
   }
}
