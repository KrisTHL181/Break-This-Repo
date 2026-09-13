package net.minecraft.client.gui.spectator;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.spectator.categories.SpectatorPage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpectatorMenu {
   static final Identifier CLOSE_SPRITE = Identifier.withDefaultNamespace("spectator/close");
   static final Identifier SCROLL_LEFT_SPRITE = Identifier.withDefaultNamespace("spectator/scroll_left");
   static final Identifier SCROLL_RIGHT_SPRITE = Identifier.withDefaultNamespace("spectator/scroll_right");
   private static final SpectatorMenuItem CLOSE_ITEM = new SpectatorMenu.CloseSpectatorItem();
   private static final SpectatorMenuItem SCROLL_LEFT = new SpectatorMenu.ScrollMenuItem(-1, true);
   private static final SpectatorMenuItem SCROLL_RIGHT_ENABLED = new SpectatorMenu.ScrollMenuItem(1, true);
   private static final SpectatorMenuItem SCROLL_RIGHT_DISABLED = new SpectatorMenu.ScrollMenuItem(1, false);
   private static final int MAX_PER_PAGE = 8;
   static final Component CLOSE_MENU_TEXT = Component.translatable("spectatorMenu.close");
   static final Component PREVIOUS_PAGE_TEXT = Component.translatable("spectatorMenu.previous_page");
   static final Component NEXT_PAGE_TEXT = Component.translatable("spectatorMenu.next_page");
   public static final SpectatorMenuItem EMPTY_SLOT = new SpectatorMenuItem() {
      @Override
      public void selectItem(SpectatorMenu p_101812_) {
      }

      @Override
      public Component getName() {
         return CommonComponents.EMPTY;
      }

      @Override
      public void renderIcon(GuiGraphics p_283652_, float p_101809_, float p_363989_) {
      }

      @Override
      public boolean isEnabled() {
         return false;
      }
   };
   private final SpectatorMenuListener listener;
   private SpectatorMenuCategory category;
   private int selectedSlot = -1;
   int page;

   public SpectatorMenu(SpectatorMenuListener p_101785_) {
      this.category = new RootSpectatorMenuCategory();
      this.listener = p_101785_;
   }

   public SpectatorMenuItem getItem(int p_101788_) {
      int i = p_101788_ + this.page * 6;
      if (this.page > 0 && p_101788_ == 0) {
         return SCROLL_LEFT;
      } else if (p_101788_ == 7) {
         return i < this.category.getItems().size() ? SCROLL_RIGHT_ENABLED : SCROLL_RIGHT_DISABLED;
      } else if (p_101788_ == 8) {
         return CLOSE_ITEM;
      } else {
         return i >= 0 && i < this.category.getItems().size()
            ? (SpectatorMenuItem)MoreObjects.firstNonNull(this.category.getItems().get(i), EMPTY_SLOT)
            : EMPTY_SLOT;
      }
   }

   public List<SpectatorMenuItem> getItems() {
      List<SpectatorMenuItem> list = Lists.newArrayList();

      for (int i = 0; i <= 8; i++) {
         list.add(this.getItem(i));
      }

      return list;
   }

   public SpectatorMenuItem getSelectedItem() {
      return this.getItem(this.selectedSlot);
   }

   public SpectatorMenuCategory getSelectedCategory() {
      return this.category;
   }

   public void selectSlot(int p_101798_) {
      SpectatorMenuItem spectatormenuitem = this.getItem(p_101798_);
      if (spectatormenuitem != EMPTY_SLOT) {
         if (this.selectedSlot == p_101798_ && spectatormenuitem.isEnabled()) {
            spectatormenuitem.selectItem(this);
         } else {
            this.selectedSlot = p_101798_;
         }
      }
   }

   public void exit() {
      this.listener.onSpectatorMenuClosed(this);
   }

   public int getSelectedSlot() {
      return this.selectedSlot;
   }

   public void selectCategory(SpectatorMenuCategory p_101795_) {
      this.category = p_101795_;
      this.selectedSlot = -1;
      this.page = 0;
   }

   public SpectatorPage getCurrentPage() {
      return new SpectatorPage(this.getItems(), this.selectedSlot);
   }

   @OnlyIn(Dist.CLIENT)
   static class CloseSpectatorItem implements SpectatorMenuItem {
      @Override
      public void selectItem(SpectatorMenu p_101823_) {
         p_101823_.exit();
      }

      @Override
      public Component getName() {
         return SpectatorMenu.CLOSE_MENU_TEXT;
      }

      @Override
      public void renderIcon(GuiGraphics p_283113_, float p_282295_, float p_361644_) {
         p_283113_.blitSprite(
            RenderPipelines.GUI_TEXTURED, SpectatorMenu.CLOSE_SPRITE, 0, 0, 16, 16, ARGB.colorFromFloat(p_361644_, p_282295_, p_282295_, p_282295_)
         );
      }

      @Override
      public boolean isEnabled() {
         return true;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ScrollMenuItem implements SpectatorMenuItem {
      private final int direction;
      private final boolean enabled;

      public ScrollMenuItem(int p_101829_, boolean p_101830_) {
         this.direction = p_101829_;
         this.enabled = p_101830_;
      }

      @Override
      public void selectItem(SpectatorMenu p_101836_) {
         p_101836_.page = p_101836_.page + this.direction;
      }

      @Override
      public Component getName() {
         return this.direction < 0 ? SpectatorMenu.PREVIOUS_PAGE_TEXT : SpectatorMenu.NEXT_PAGE_TEXT;
      }

      @Override
      public void renderIcon(GuiGraphics p_281376_, float p_282065_, float p_360875_) {
         int i = ARGB.colorFromFloat(p_360875_, p_282065_, p_282065_, p_282065_);
         if (this.direction < 0) {
            p_281376_.blitSprite(RenderPipelines.GUI_TEXTURED, SpectatorMenu.SCROLL_LEFT_SPRITE, 0, 0, 16, 16, i);
         } else {
            p_281376_.blitSprite(RenderPipelines.GUI_TEXTURED, SpectatorMenu.SCROLL_RIGHT_SPRITE, 0, 0, 16, 16, i);
         }
      }

      @Override
      public boolean isEnabled() {
         return this.enabled;
      }
   }
}
