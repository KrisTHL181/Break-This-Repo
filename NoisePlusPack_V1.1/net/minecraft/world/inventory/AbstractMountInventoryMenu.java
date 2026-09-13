package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractMountInventoryMenu extends AbstractContainerMenu {
   protected final Container mountContainer;
   protected final LivingEntity mount;
   protected final int SLOT_SADDLE = 0;
   protected final int SLOT_BODY_ARMOR = 1;
   protected final int SLOT_INVENTORY_START = 2;
   protected static final int INVENTORY_ROWS = 3;

   protected AbstractMountInventoryMenu(int p_451193_, Inventory p_453764_, Container p_451444_, LivingEntity p_453510_) {
      super(null, p_451193_);
      this.mountContainer = p_451444_;
      this.mount = p_453510_;
      p_451444_.startOpen(p_453764_.player);
   }

   protected abstract boolean hasInventoryChanged(Container var1);

   @Override
   public boolean stillValid(Player p_459837_) {
      return !this.hasInventoryChanged(this.mountContainer)
         && this.mountContainer.stillValid(p_459837_)
         && this.mount.isAlive()
         && p_459837_.isWithinEntityInteractionRange(this.mount, 4.0);
   }

   @Override
   public void removed(Player p_454265_) {
      super.removed(p_454265_);
      this.mountContainer.stopOpen(p_454265_);
   }

   @Override
   public ItemStack quickMoveStack(Player p_456705_, int p_457058_) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = this.slots.get(p_457058_);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         int i = 2 + this.mountContainer.getContainerSize();
         if (p_457058_ < i) {
            if (!this.moveItemStackTo(itemstack1, i, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(1).mayPlace(itemstack1) && !this.getSlot(1).hasItem()) {
            if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(0).mayPlace(itemstack1) && !this.getSlot(0).hasItem()) {
            if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.mountContainer.getContainerSize() == 0 || !this.moveItemStackTo(itemstack1, 2, i, false)) {
            int j = i + 27;
            int k = j + 9;
            if (p_457058_ >= j && p_457058_ < k) {
               if (!this.moveItemStackTo(itemstack1, i, j, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (p_457058_ >= i && p_457058_ < j) {
               if (!this.moveItemStackTo(itemstack1, j, k, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(itemstack1, j, j, false)) {
               return ItemStack.EMPTY;
            }

            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return itemstack;
   }

   public static int getInventorySize(int p_457364_) {
      return p_457364_ * 3;
   }
}
