package net.minecraft.client.gui.spectator;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.spectator.categories.SpectatorPage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public class SpectatorMenu {
   private static final Identifier CLOSE_SPRITE = Identifier.withDefaultNamespace("spectator/close");
   private static final Identifier SCROLL_LEFT_SPRITE = Identifier.withDefaultNamespace("spectator/scroll_left");
   private static final Identifier SCROLL_RIGHT_SPRITE = Identifier.withDefaultNamespace("spectator/scroll_right");
   private static final SpectatorMenuItem CLOSE_ITEM = new SpectatorMenu.CloseSpectatorItem();
   private static final SpectatorMenuItem SCROLL_LEFT = new SpectatorMenu.ScrollMenuItem(-1, true);
   private static final SpectatorMenuItem SCROLL_RIGHT_ENABLED = new SpectatorMenu.ScrollMenuItem(1, true);
   private static final SpectatorMenuItem SCROLL_RIGHT_DISABLED = new SpectatorMenu.ScrollMenuItem(1, false);
   private static final int MAX_PER_PAGE = 8;
   private static final Component CLOSE_MENU_TEXT = Component.translatable("spectatorMenu.close");
   private static final Component PREVIOUS_PAGE_TEXT = Component.translatable("spectatorMenu.previous_page");
   private static final Component NEXT_PAGE_TEXT = Component.translatable("spectatorMenu.next_page");
   public static final SpectatorMenuItem EMPTY_SLOT = new SpectatorMenuItem() {
      @Override
      public void selectItem(final SpectatorMenu menu) {
      }

      @Override
      public Component getName() {
         return CommonComponents.EMPTY;
      }

      @Override
      public void extractIcon(final GuiGraphicsExtractor graphics, final float brightness, final float alpha) {
      }

      @Override
      public boolean isEnabled() {
         return false;
      }
   };
   private final SpectatorMenuListener listener;
   private SpectatorMenuCategory category;
   private int selectedSlot = -1;
   private int page;

   public SpectatorMenu(final SpectatorMenuListener listener) {
      this.category = new RootSpectatorMenuCategory();
      this.listener = listener;
   }

   public SpectatorMenuItem getItem(final int slot) {
      int index = slot + this.page * 6;
      if (this.page > 0 && slot == 0) {
         return SCROLL_LEFT;
      } else if (slot == 7) {
         return index < this.category.getItems().size() ? SCROLL_RIGHT_ENABLED : SCROLL_RIGHT_DISABLED;
      } else if (slot == 8) {
         return CLOSE_ITEM;
      } else {
         return index >= 0 && index < this.category.getItems().size()
            ? (SpectatorMenuItem)MoreObjects.firstNonNull(this.category.getItems().get(index), EMPTY_SLOT)
            : EMPTY_SLOT;
      }
   }

   public List<SpectatorMenuItem> getItems() {
      List<SpectatorMenuItem> items = Lists.newArrayList();

      for (int i = 0; i <= 8; i++) {
         items.add(this.getItem(i));
      }

      return items;
   }

   public SpectatorMenuItem getSelectedItem() {
      return this.getItem(this.selectedSlot);
   }

   public SpectatorMenuCategory getSelectedCategory() {
      return this.category;
   }

   public void selectSlot(final int slot) {
      SpectatorMenuItem item = this.getItem(slot);
      if (item != EMPTY_SLOT) {
         if (this.selectedSlot == slot && item.isEnabled()) {
            item.selectItem(this);
         } else {
            this.selectedSlot = slot;
         }
      }
   }

   public void exit() {
      this.listener.onSpectatorMenuClosed(this);
   }

   public int getSelectedSlot() {
      return this.selectedSlot;
   }

   public void selectCategory(final SpectatorMenuCategory category) {
      this.category = category;
      this.selectedSlot = -1;
      this.page = 0;
   }

   public SpectatorPage getCurrentPage() {
      return new SpectatorPage(this.getItems(), this.selectedSlot);
   }

   private static class CloseSpectatorItem implements SpectatorMenuItem {
      @Override
      public void selectItem(final SpectatorMenu menu) {
         menu.exit();
      }

      @Override
      public Component getName() {
         return SpectatorMenu.CLOSE_MENU_TEXT;
      }

      @Override
      public void extractIcon(final GuiGraphicsExtractor graphics, final float brightness, final float alpha) {
         graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED, SpectatorMenu.CLOSE_SPRITE, 0, 0, 16, 16, ARGB.colorFromFloat(alpha, brightness, brightness, brightness)
         );
      }

      @Override
      public boolean isEnabled() {
         return true;
      }
   }

   private static class ScrollMenuItem implements SpectatorMenuItem {
      private final int direction;
      private final boolean enabled;

      public ScrollMenuItem(final int direction, final boolean enabled) {
         this.direction = direction;
         this.enabled = enabled;
      }

      @Override
      public void selectItem(final SpectatorMenu menu) {
         menu.page = menu.page + this.direction;
      }

      @Override
      public Component getName() {
         return this.direction < 0 ? SpectatorMenu.PREVIOUS_PAGE_TEXT : SpectatorMenu.NEXT_PAGE_TEXT;
      }

      @Override
      public void extractIcon(final GuiGraphicsExtractor graphics, final float brightness, final float alpha) {
         int color = ARGB.colorFromFloat(alpha, brightness, brightness, brightness);
         if (this.direction < 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SpectatorMenu.SCROLL_LEFT_SPRITE, 0, 0, 16, 16, color);
         } else {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SpectatorMenu.SCROLL_RIGHT_SPRITE, 0, 0, 16, 16, color);
         }
      }

      @Override
      public boolean isEnabled() {
         return this.enabled;
      }
   }
}
