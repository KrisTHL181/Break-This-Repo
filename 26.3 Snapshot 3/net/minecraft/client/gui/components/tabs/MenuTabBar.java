package net.minecraft.client.gui.components.tabs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class MenuTabBar extends TabNavigationBar {
   private static final int HEIGHT = 24;
   private static final int MAX_WIDTH = 400;
   private static final int MARGIN = 14;

   public MenuTabBar(
      final int x,
      final int y,
      final int width,
      final int height,
      final TabManager tabManager,
      final ImmutableList<TabButton> tabButtons,
      final ImmutableList<Tab> tabs
   ) {
      super(x, y, width, height, tabManager, tabButtons, tabs);
   }

   public static MenuTabBar.Builder builder(final TabManager tabManager, final int width) {
      return new MenuTabBar.Builder(tabManager, width);
   }

   @Override
   protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      graphics.blit(
         RenderPipelines.GUI_TEXTURED,
         Screen.HEADER_SEPARATOR,
         0,
         this.layout.getY() + this.layout.getHeight() - 2,
         0.0F,
         0.0F,
         ((TabButton)this.tabButtons.getFirst()).getX(),
         2,
         32,
         2
      );
      int afterLastTab = ((TabButton)this.tabButtons.get(this.tabButtons.size() - 1)).getRight();
      graphics.blit(
         RenderPipelines.GUI_TEXTURED,
         Screen.HEADER_SEPARATOR,
         afterLastTab,
         this.layout.getY() + this.layout.getHeight() - 2,
         0.0F,
         0.0F,
         this.width,
         2,
         32,
         2
      );
      super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
   }

   @Override
   public void arrangeElements(final int width) {
      this.width = width;
      int tabsWidth = Math.min(400, width) - 28;
      int tabWidth = Mth.roundToward(tabsWidth / this.tabs.size(), 2);
      UnmodifiableIterator var4 = this.tabButtons.iterator();

      while (var4.hasNext()) {
         TabButton button = (TabButton)var4.next();
         button.setWidth(tabWidth);
      }

      this.layout.arrangeElements();
      this.layout.setX(Mth.roundToward((width - tabsWidth) / 2, 2));
      this.layout.setY(0);
   }

   public static class Builder extends TabNavigationBar.Builder {
      private Builder(final TabManager tabManager, final int width) {
         super(tabManager, 0, 0, width, 24);
      }

      public MenuTabBar.Builder addTab(final Tab tab) {
         super.addTab(new MenuTabBar.MenuTabButton(this.tabManager, tab, 0, this.height), tab);
         return this;
      }

      public MenuTabBar.Builder addTabs(final Tab... tabs) {
         for (Tab tab : tabs) {
            this.addTab(tab);
         }

         return this;
      }

      public MenuTabBar build() {
         return new MenuTabBar(this.x, this.y, this.width, this.height, this.tabManager, ImmutableList.copyOf(this.tabButtons), ImmutableList.copyOf(this.tabs));
      }
   }

   public static class MenuTabButton extends TabButton {
      private static final WidgetSprites SPRITES = new WidgetSprites(
         Identifier.withDefaultNamespace("widget/tab_selected"),
         Identifier.withDefaultNamespace("widget/tab"),
         Identifier.withDefaultNamespace("widget/tab_selected_highlighted"),
         Identifier.withDefaultNamespace("widget/tab_highlighted")
      );
      private static final int SELECTED_OFFSET = 3;
      private static final int TEXT_MARGIN = 1;
      private static final int UNDERLINE_HEIGHT = 1;
      private static final int UNDERLINE_MARGIN_X = 4;
      private static final int UNDERLINE_MARGIN_BOTTOM = 2;

      public MenuTabButton(final TabManager tabManager, final Tab tab, final int width, final int height) {
         super(tabManager, tab, width, height);
      }

      @Override
      protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
         graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED, SPRITES.get(this.isSelected(), this.isHoveredOrFocused()), this.getX(), this.getY(), this.width, this.height
         );
         Font font = Minecraft.getInstance().font;
         int underlineColor = this.active ? -1 : -6250336;
         if (this.isSelected()) {
            this.renderMenuBackground(graphics, this.getX() + 2, this.getY() + 2, this.getRight() - 2, this.getBottom());
            this.renderFocusUnderline(graphics, font, underlineColor);
         }

         this.renderLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
         this.handleCursor(graphics);
      }

      protected void renderMenuBackground(final GuiGraphicsExtractor graphics, final int x0, final int y0, final int x1, final int y1) {
         Screen.extractMenuBackgroundTexture(graphics, Screen.MENU_BACKGROUND, x0, y0, 0.0F, 0.0F, x1 - x0, y1 - y0);
      }

      private void renderLabel(final ActiveTextCollector output) {
         int left = this.getX() + 1;
         int top = this.getY() + (this.isSelected() ? 0 : 3);
         int right = this.getX() + this.getWidth() - 1;
         int bottom = this.getY() + this.getHeight();
         output.acceptScrollingWithDefaultCenter(this.getMessage(), left, right, top, bottom);
      }

      private void renderFocusUnderline(final GuiGraphicsExtractor graphics, final Font font, final int color) {
         int width = Math.min(font.width(this.getMessage()), this.getWidth() - 4);
         int left = this.getX() + (this.getWidth() - width) / 2;
         int top = this.getY() + this.getHeight() - 2;
         graphics.fill(left, top, left + width, top + 1, color);
      }
   }
}
