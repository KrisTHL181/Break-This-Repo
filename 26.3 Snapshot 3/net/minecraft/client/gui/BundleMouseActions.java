package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;

public class BundleMouseActions implements ItemSlotMouseAction {
   private final Minecraft minecraft;
   private final ScrollWheelHandler scrollWheelHandler;

   public BundleMouseActions(final Minecraft minecraft) {
      this.minecraft = minecraft;
      this.scrollWheelHandler = new ScrollWheelHandler();
   }

   @Override
   public boolean matches(final Slot slot) {
      return slot.getItem().is(ItemTags.BUNDLES);
   }

   @Override
   public boolean onMouseScrolled(final double scrollX, final double scrollY, final int slotIndex, final ItemStack itemStack) {
      int amountOfShownItems = BundleItem.getNumberOfItemsToShow(itemStack);
      if (amountOfShownItems == 0) {
         return false;
      }

      Vector2i wheelXY = this.scrollWheelHandler.onMouseScroll(scrollX, scrollY);
      int wheel = wheelXY.y == 0 ? -wheelXY.x : wheelXY.y;
      if (wheel != 0) {
         int selectedItem = BundleItem.getSelectedItemIndex(itemStack);
         int updatedSelectedItem = ScrollWheelHandler.getNextScrollWheelSelection(wheel, selectedItem, amountOfShownItems);
         if (selectedItem != updatedSelectedItem) {
            this.toggleSelectedBundleItem(itemStack, slotIndex, updatedSelectedItem);
         }
      }

      return true;
   }

   @Override
   public void onStopHovering(final Slot hoveredSlot) {
      this.unselectedBundleItem(hoveredSlot.getItem(), hoveredSlot.index);
   }

   @Override
   public void onSlotClicked(final Slot slot, final ContainerInput containerInput) {
      if (containerInput == ContainerInput.QUICK_MOVE || containerInput == ContainerInput.SWAP) {
         this.unselectedBundleItem(slot.getItem(), slot.index);
      }
   }

   private void toggleSelectedBundleItem(final ItemStack bundleItem, final int slotIndex, final int selectedItem) {
      if (this.minecraft.getConnection() != null && selectedItem < BundleItem.getNumberOfItemsToShow(bundleItem)) {
         ClientPacketListener connection = this.minecraft.getConnection();
         BundleItem.toggleSelectedItem(bundleItem, selectedItem);
         connection.send(new ServerboundSelectBundleItemPacket(slotIndex, selectedItem));
      }
   }

   public void unselectedBundleItem(final ItemStack bundleItem, final int slotIndex) {
      this.toggleSelectedBundleItem(bundleItem, slotIndex, -1);
   }
}
