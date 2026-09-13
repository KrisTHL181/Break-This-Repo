package net.minecraft.client.gui.screens.inventory;

import java.util.Objects;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.ItemStack;

public class LecternScreen extends BookViewScreen implements MenuAccess<LecternMenu> {
   private static final int MENU_BUTTON_MARGIN = 4;
   private static final int MENU_BUTTON_SIZE = 98;
   private static final Component TAKE_BOOK_LABEL = Component.translatable("lectern.take_book");
   private final LecternMenu menu;
   private final ContainerListener listener = new ContainerListener() {
      @Override
      public void slotChanged(final AbstractContainerMenu container, final int slotIndex, final ItemStack itemStack) {
         LecternScreen.this.bookChanged();
      }

      @Override
      public void dataChanged(final AbstractContainerMenu container, final int id, final int value) {
         if (id == 0) {
            LecternScreen.this.pageChanged();
         }
      }
   };

   public LecternScreen(final LecternMenu menu, final Inventory inventory, final Component title) {
      this.menu = menu;
   }

   public LecternMenu getMenu() {
      return this.menu;
   }

   @Override
   protected void init() {
      super.init();
      this.menu.addSlotListener(this.listener);
   }

   @Override
   public void onClose() {
      this.minecraft.player.closeContainer();
      super.onClose();
   }

   @Override
   public void removed() {
      super.removed();
      this.menu.removeSlotListener(this.listener);
   }

   @Override
   protected void createMenuControls() {
      if (this.minecraft.player.mayBuild()) {
         int buttonY = this.menuControlsTop();
         int middle = this.width / 2;
         this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).pos(middle - 98 - 2, buttonY).width(98).build());
         this.addRenderableWidget(Button.builder(TAKE_BOOK_LABEL, button -> this.sendButtonClick(3)).pos(middle + 2, buttonY).width(98).build());
      } else {
         super.createMenuControls();
      }
   }

   @Override
   protected void pageBack() {
      this.sendButtonClick(1);
   }

   @Override
   protected void pageForward() {
      this.sendButtonClick(2);
   }

   @Override
   protected boolean forcePage(final int page) {
      if (page != this.menu.getPage()) {
         this.sendButtonClick(100 + page);
         return true;
      } else {
         return false;
      }
   }

   private void sendButtonClick(final int button) {
      this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private void bookChanged() {
      ItemStack book = this.menu.getBook();
      this.setBookAccess(Objects.requireNonNullElse(BookViewScreen.BookAccess.fromItem(book), BookViewScreen.EMPTY_ACCESS));
   }

   private void pageChanged() {
      this.setPage(this.menu.getPage());
   }

   @Override
   protected void closeContainerOnServer() {
      this.minecraft.player.closeContainer();
   }
}
