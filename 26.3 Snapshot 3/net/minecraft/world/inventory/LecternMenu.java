package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LecternMenu extends AbstractContainerMenu {
   private static final int DATA_COUNT = 1;
   private static final int SLOT_COUNT = 1;
   public static final int BUTTON_PREV_PAGE = 1;
   public static final int BUTTON_NEXT_PAGE = 2;
   public static final int BUTTON_TAKE_BOOK = 3;
   public static final int BUTTON_PAGE_JUMP_RANGE_START = 100;
   private final Container lectern;
   private final ContainerData lecternData;

   public LecternMenu(final int containerId) {
      this(containerId, new SimpleContainer(1), new SimpleContainerData(1));
   }

   public LecternMenu(final int containerId, final Container lectern, final ContainerData lecternData) {
      super(MenuType.LECTERN, containerId);
      checkContainerSize(lectern, 1);
      checkContainerDataCount(lecternData, 1);
      this.lectern = lectern;
      this.lecternData = lecternData;
      this.addSlot(new Slot(lectern, 0, 0, 0) {
         @Override
         public void setChanged() {
            super.setChanged();
            LecternMenu.this.slotsChanged(this.container);
         }
      });
      this.addDataSlots(lecternData);
   }

   @Override
   public boolean clickMenuButton(final Player player, final int buttonId) {
      if (buttonId >= 100) {
         int pageToSet = buttonId - 100;
         this.setData(0, pageToSet);
         return true;
      }

      switch (buttonId) {
         case 1: {
            int currentPage = this.lecternData.get(0);
            this.setData(0, currentPage - 1);
            return true;
         }
         case 2: {
            int currentPage = this.lecternData.get(0);
            this.setData(0, currentPage + 1);
            return true;
         }
         case 3:
            if (!player.mayBuild()) {
               return false;
            }

            ItemStack book = this.lectern.removeItemNoUpdate(0);
            this.lectern.setChanged();
            if (!player.getInventory().add(book)) {
               player.drop(book, false);
            }

            return true;
         default:
            return false;
      }
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      return ItemStack.EMPTY;
   }

   @Override
   public void setData(final int id, final int value) {
      super.setData(id, value);
      this.broadcastChanges();
   }

   @Override
   public boolean stillValid(final Player player) {
      return this.lectern.stillValid(player);
   }

   public ItemStack getBook() {
      return this.lectern.getItem(0);
   }

   public int getPage() {
      return this.lecternData.get(0);
   }
}
