package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

public class StonecutterScreen extends AbstractContainerScreen<StonecutterMenu> {
   private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller");
   private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
   private static final Identifier RECIPE_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
   private static final Identifier RECIPE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
   private static final Identifier RECIPE_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe");
   private static final Identifier BG_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/stonecutter.png");
   private static final int SCROLLER_WIDTH = 12;
   private static final int SCROLLER_HEIGHT = 15;
   private static final int RECIPES_COLUMNS = 4;
   private static final int RECIPES_ROWS = 3;
   private static final int RECIPES_IMAGE_SIZE_WIDTH = 16;
   private static final int RECIPES_IMAGE_SIZE_HEIGHT = 18;
   private static final int SCROLLER_FULL_HEIGHT = 54;
   private static final int RECIPES_X = 52;
   private static final int RECIPES_Y = 14;
   private float scrollOffs;
   private boolean scrolling;
   private int startIndex;
   private boolean displayRecipes;

   public StonecutterScreen(final StonecutterMenu menu, final Inventory inventory, final Component title) {
      super(menu, inventory, title);
      menu.registerUpdateListener(this::containerChanged);
      this.titleLabelY--;
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      int xo = this.leftPos;
      int yo = this.topPos;
      graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      int sy = (int)(41.0F * this.scrollOffs);
      Identifier sprite = this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
      int scrollerXStart = xo + 119;
      int scrollerYStart = yo + 15;
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, scrollerXStart, scrollerYStart + sy, 12, 15);
      if (mouseX >= scrollerXStart && mouseY >= scrollerYStart && mouseX < scrollerXStart + 12 && mouseY < scrollerYStart + 54) {
         if (this.isScrollBarActive()) {
            graphics.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
         } else {
            graphics.requestCursor(CursorTypes.NOT_ALLOWED);
         }
      }

      int x = this.leftPos + 52;
      int y = this.topPos + 14;
      int endIndex = this.startIndex + 12;
      this.extractButtons(graphics, mouseX, mouseY, x, y, endIndex);
      this.extractRecipes(graphics, x, y, endIndex);
   }

   @Override
   protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
      super.extractTooltip(graphics, mouseX, mouseY);
      if (this.displayRecipes) {
         int edgeLeft = this.leftPos + 52;
         int edgeTop = this.topPos + 14;
         int endIndex = this.startIndex + 12;
         SelectableRecipe.SingleInputSet<StonecutterRecipe> visibleRecipes = this.menu.getVisibleRecipes();

         for (int index = this.startIndex; index < endIndex && index < visibleRecipes.size(); index++) {
            int posIndex = index - this.startIndex;
            int itemLeft = edgeLeft + posIndex % 4 * 16;
            int itemRight = edgeTop + posIndex / 4 * 18 + 2;
            if (mouseX >= itemLeft && mouseX < itemLeft + 16 && mouseY >= itemRight && mouseY < itemRight + 18) {
               ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
               SlotDisplay buttonIcon = visibleRecipes.entries().get(index).recipe().optionDisplay();
               graphics.setTooltipForNextFrame(this.font, buttonIcon.resolveForFirstStack(context), mouseX, mouseY);
            }
         }
      }
   }

   private void extractButtons(final GuiGraphicsExtractor graphics, final int xm, final int ym, final int x, final int y, final int endIndex) {
      for (int index = this.startIndex; index < endIndex && index < this.menu.getNumberOfVisibleRecipes(); index++) {
         int posIndex = index - this.startIndex;
         int posX = x + posIndex % 4 * 16;
         int row = posIndex / 4;
         int posY = y + row * 18 + 2;
         Identifier sprite;
         if (index == this.menu.getSelectedRecipeIndex()) {
            sprite = RECIPE_SELECTED_SPRITE;
         } else if (xm >= posX && ym >= posY && xm < posX + 16 && ym < posY + 18) {
            sprite = RECIPE_HIGHLIGHTED_SPRITE;
         } else {
            sprite = RECIPE_SPRITE;
         }

         int textureY = posY - 1;
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, posX, textureY, 16, 18);
         if (xm >= posX && ym >= textureY && xm < posX + 16 && ym < textureY + 18) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
         }
      }
   }

   private void extractRecipes(final GuiGraphicsExtractor graphics, final int x, final int y, final int endIndex) {
      SelectableRecipe.SingleInputSet<StonecutterRecipe> visibleRecipes = this.menu.getVisibleRecipes();
      ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);

      for (int index = this.startIndex; index < endIndex && index < visibleRecipes.size(); index++) {
         int posIndex = index - this.startIndex;
         int posX = x + posIndex % 4 * 16;
         int row = posIndex / 4;
         int posY = y + row * 18 + 2;
         SlotDisplay buttonIcon = visibleRecipes.entries().get(index).recipe().optionDisplay();
         graphics.item(buttonIcon.resolveForFirstStack(context), posX, posY);
      }
   }

   @Override
   public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
      if (this.displayRecipes) {
         int xo = this.leftPos + 52;
         int yo = this.topPos + 14;
         int endIndex = this.startIndex + 12;

         for (int index = this.startIndex; index < endIndex; index++) {
            int posIndex = index - this.startIndex;
            double xx = event.x() - (xo + posIndex % 4 * 16);
            double yy = event.y() - (yo + posIndex / 4 * 18);
            if (xx >= 0.0 && yy >= 0.0 && xx < 16.0 && yy < 18.0 && this.menu.clickMenuButton(this.minecraft.player, index)) {
               Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
               this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
               return true;
            }
         }

         xo = this.leftPos + 119;
         yo = this.topPos + 9;
         if (event.x() >= xo && event.x() < xo + 12 && event.y() >= yo && event.y() < yo + 54) {
            this.scrolling = true;
         }
      }

      return super.mouseClicked(event, doubleClick);
   }

   @Override
   public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
      if (this.scrolling && this.isScrollBarActive()) {
         int yscr = this.topPos + 14;
         int yscr2 = yscr + 54;
         this.scrollOffs = ((float)event.y() - yscr - 7.5F) / (yscr2 - yscr - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         this.startIndex = (int)(this.scrollOffs * this.getOffscreenRows() + 0.5) * 4;
         return true;
      } else {
         return super.mouseDragged(event, dx, dy);
      }
   }

   @Override
   public boolean mouseReleased(final MouseButtonEvent event) {
      this.scrolling = false;
      return super.mouseReleased(event);
   }

   @Override
   public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
      if (super.mouseScrolled(x, y, scrollX, scrollY)) {
         return true;
      }

      if (this.isScrollBarActive()) {
         int offscreenRows = this.getOffscreenRows();
         float scrolledDelta = (float)scrollY / offscreenRows;
         this.scrollOffs = Mth.clamp(this.scrollOffs - scrolledDelta, 0.0F, 1.0F);
         this.startIndex = (int)(this.scrollOffs * offscreenRows + 0.5) * 4;
      }

      return true;
   }

   private boolean isScrollBarActive() {
      return this.displayRecipes && this.menu.getNumberOfVisibleRecipes() > 12;
   }

   protected int getOffscreenRows() {
      return (this.menu.getNumberOfVisibleRecipes() + 4 - 1) / 4 - 3;
   }

   private void containerChanged() {
      this.displayRecipes = this.menu.hasInputItem();
      this.scrollOffs = 0.0F;
      this.startIndex = 0;
   }
}
