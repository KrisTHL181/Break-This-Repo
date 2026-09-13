package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

public abstract class AbstractRecipeBookScreen<T extends RecipeBookMenu> extends AbstractContainerScreen<T> implements RecipeUpdateListener {
   private final RecipeBookComponent<?> recipeBookComponent;
   private boolean widthTooNarrow;

   public AbstractRecipeBookScreen(final T menu, final RecipeBookComponent<?> recipeBookComponent, final Inventory inventory, final Component title) {
      super(menu, inventory, title);
      this.recipeBookComponent = recipeBookComponent;
   }

   @Override
   protected void init() {
      super.init();
      this.widthTooNarrow = this.width < 379;
      this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow);
      this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
      this.initButton();
   }

   protected abstract ScreenPosition getRecipeBookButtonPosition();

   private void initButton() {
      ScreenPosition buttonPos = this.getRecipeBookButtonPosition();
      this.addRenderableWidget(new ImageButton(buttonPos.x(), buttonPos.y(), 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> {
         this.recipeBookComponent.toggleVisibility();
         this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
         ScreenPosition updatedButtonPos = this.getRecipeBookButtonPosition();
         button.setPosition(updatedButtonPos.x(), updatedButtonPos.y());
         this.onRecipeBookButtonClick();
      }));
      this.addWidget(this.recipeBookComponent);
   }

   protected void onRecipeBookButtonClick() {
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
         this.extractBackground(graphics, mouseX, mouseY, a);
      } else {
         super.extractContents(graphics, mouseX, mouseY, a);
      }

      graphics.nextStratum();
      this.recipeBookComponent.extractRenderState(graphics, mouseX, mouseY, a);
      graphics.nextStratum();
      this.extractCarriedItem(graphics, mouseX, mouseY);
      this.extractTooltip(graphics, mouseX, mouseY);
      this.recipeBookComponent.extractTooltip(graphics, mouseX, mouseY, this.hoveredSlot);
   }

   @Override
   protected void extractSlots(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
      super.extractSlots(graphics, mouseX, mouseY);
      this.recipeBookComponent.extractGhostRecipe(graphics, this.isBiggerResultSlot());
   }

   protected boolean isBiggerResultSlot() {
      return true;
   }

   @Override
   public boolean charTyped(final CharacterEvent event) {
      return this.recipeBookComponent.charTyped(event) ? true : super.charTyped(event);
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      return this.recipeBookComponent.keyPressed(event) ? true : super.keyPressed(event);
   }

   @Override
   public boolean isInputCaptured() {
      return super.isInputCaptured() || this.recipeBookComponent.capturesInput();
   }

   @Override
   public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
      if (this.recipeBookComponent.mouseClicked(event, doubleClick)) {
         this.setFocused(this.recipeBookComponent);
         return true;
      } else {
         return this.widthTooNarrow && this.recipeBookComponent.isVisible() ? true : super.mouseClicked(event, doubleClick);
      }
   }

   @Override
   public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
      return this.recipeBookComponent.mouseDragged(event, dx, dy) ? true : super.mouseDragged(event, dx, dy);
   }

   @Override
   protected boolean isHovering(final int left, final int top, final int w, final int h, final double xm, final double ym) {
      return (!this.widthTooNarrow || !this.recipeBookComponent.isVisible()) && super.isHovering(left, top, w, h, xm, ym);
   }

   @Override
   protected boolean hasClickedOutside(final double mx, final double my, final int xo, final int yo) {
      boolean clickedOutside = mx < xo || my < yo || mx >= xo + this.imageWidth || my >= yo + this.imageHeight;
      return this.recipeBookComponent.hasClickedOutside(mx, my, this.leftPos, this.topPos, this.imageWidth, this.imageHeight) && clickedOutside;
   }

   @Override
   protected void slotClicked(final Slot slot, final int slotId, final int buttonNum, final ContainerInput containerInput) {
      super.slotClicked(slot, slotId, buttonNum, containerInput);
      this.recipeBookComponent.slotClicked(slot);
   }

   @Override
   public void containerTick() {
      super.containerTick();
      this.recipeBookComponent.tick();
   }

   @Override
   public void recipesUpdated() {
      this.recipeBookComponent.recipesUpdated();
   }

   @Override
   public void fillGhostRecipe(final RecipeDisplay display) {
      this.recipeBookComponent.fillGhostRecipe(display);
   }
}
