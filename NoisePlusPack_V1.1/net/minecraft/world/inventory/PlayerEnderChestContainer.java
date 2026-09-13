package net.minecraft.world.inventory;

import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class PlayerEnderChestContainer extends SimpleContainer {
   private @Nullable EnderChestBlockEntity activeChest;

   public PlayerEnderChestContainer() {
      super(27);
   }

   public void setActiveChest(EnderChestBlockEntity p_40106_) {
      this.activeChest = p_40106_;
   }

   public boolean isActiveChest(EnderChestBlockEntity p_150634_) {
      return this.activeChest == p_150634_;
   }

   public void fromSlots(ValueInput.TypedInputList<ItemStackWithSlot> p_410579_) {
      for (int i = 0; i < this.getContainerSize(); i++) {
         this.setItem(i, ItemStack.EMPTY);
      }

      for (ItemStackWithSlot itemstackwithslot : p_410579_) {
         if (itemstackwithslot.isValidInContainer(this.getContainerSize())) {
            this.setItem(itemstackwithslot.slot(), itemstackwithslot.stack());
         }
      }
   }

   public void storeAsSlots(ValueOutput.TypedOutputList<ItemStackWithSlot> p_409232_) {
      for (int i = 0; i < this.getContainerSize(); i++) {
         ItemStack itemstack = this.getItem(i);
         if (!itemstack.isEmpty()) {
            p_409232_.add(new ItemStackWithSlot(i, itemstack));
         }
      }
   }

   @Override
   public boolean stillValid(Player p_40104_) {
      return this.activeChest != null && !this.activeChest.stillValid(p_40104_) ? false : super.stillValid(p_40104_);
   }

   @Override
   public void startOpen(ContainerUser p_430167_) {
      if (this.activeChest != null) {
         this.activeChest.startOpen(p_430167_);
      }

      super.startOpen(p_430167_);
   }

   @Override
   public void stopOpen(ContainerUser p_429559_) {
      if (this.activeChest != null) {
         this.activeChest.stopOpen(p_429559_);
      }

      super.stopOpen(p_429559_);
      this.activeChest = null;
   }
}
