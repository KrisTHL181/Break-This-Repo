package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractRecipeBookScreen<T extends RecipeBookMenu> extends AbstractContainerScreen<T> implements RecipeUpdateListener {
   private final RecipeBookComponent<?> recipeBookComponent;
   private boolean widthTooNarrow;

   public AbstractRecipeBookScreen(T p_365857_, RecipeBookComponent<?> p_370150_, Inventory p_369435_, Component p_366314_) {
      super(p_365857_, p_369435_, p_366314_);
      this.recipeBookComponent = p_370150_;
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
      ScreenPosition screenposition = this.getRecipeBookButtonPosition();
      this.addRenderableWidget(new ImageButton(screenposition.x(), screenposition.y(), 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, p_367715_ -> {
         this.recipeBookComponent.toggleVisibility();
         this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
         ScreenPosition screenposition1 = this.getRecipeBookButtonPosition();
         p_367715_.setPosition(screenposition1.x(), screenposition1.y());
         this.onRecipeBookButtonClick();
      }));
      this.addWidget(this.recipeBookComponent);
   }

   protected void onRecipeBookButtonClick() {
   }

   @Override
   public void render(GuiGraphics p_369592_, int p_362399_, int p_367501_, float p_361085_) {
      if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
         this.renderBackground(p_369592_, p_362399_, p_367501_, p_361085_);
      } else {
         super.renderContents(p_369592_, p_362399_, p_367501_, p_361085_);
      }

      p_369592_.nextStratum();
      this.recipeBookComponent.render(p_369592_, p_362399_, p_367501_, p_361085_);
      p_369592_.nextStratum();
      this.renderCarriedItem(p_369592_, p_362399_, p_367501_);
      this.renderSnapbackItem(p_369592_);
      this.renderTooltip(p_369592_, p_362399_, p_367501_);
      this.recipeBookComponent.renderTooltip(p_369592_, p_362399_, p_367501_, this.hoveredSlot);
   }

   @Override
   protected void renderSlots(GuiGraphics p_361216_, int p_452650_, int p_453121_) {
      super.renderSlots(p_361216_, p_452650_, p_453121_);
      this.recipeBookComponent.renderGhostRecipe(p_361216_, this.isBiggerResultSlot());
   }

   protected boolean isBiggerResultSlot() {
      return true;
   }

   @Override
   public boolean charTyped(CharacterEvent p_427241_) {
      return this.recipeBookComponent.charTyped(p_427241_) ? true : super.charTyped(p_427241_);
   }

   @Override
   public boolean keyPressed(KeyEvent p_430781_) {
      return this.recipeBookComponent.keyPressed(p_430781_) ? true : super.keyPressed(p_430781_);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_427917_, boolean p_423683_) {
      if (this.recipeBookComponent.mouseClicked(p_427917_, p_423683_)) {
         this.setFocused(this.recipeBookComponent);
         return true;
      } else {
         return this.widthTooNarrow && this.recipeBookComponent.isVisible() ? true : super.mouseClicked(p_427917_, p_423683_);
      }
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent p_423602_, double p_422571_, double p_423110_) {
      return this.recipeBookComponent.mouseDragged(p_423602_, p_422571_, p_423110_) ? true : super.mouseDragged(p_423602_, p_422571_, p_423110_);
   }

   @Override
   protected boolean isHovering(int p_365583_, int p_365247_, int p_369651_, int p_367039_, double p_362562_, double p_363251_) {
      return (!this.widthTooNarrow || !this.recipeBookComponent.isVisible())
         && super.isHovering(p_365583_, p_365247_, p_369651_, p_367039_, p_362562_, p_363251_);
   }

   @Override
   protected boolean hasClickedOutside(double p_361722_, double p_361253_, int p_370022_, int p_369977_) {
      boolean flag = p_361722_ < p_370022_ || p_361253_ < p_369977_ || p_361722_ >= p_370022_ + this.imageWidth || p_361253_ >= p_369977_ + this.imageHeight;
      return this.recipeBookComponent.hasClickedOutside(p_361722_, p_361253_, this.leftPos, this.topPos, this.imageWidth, this.imageHeight) && flag;
   }

   @Override
   protected void slotClicked(Slot p_366850_, int p_365209_, int p_363173_, ClickType p_369612_) {
      super.slotClicked(p_366850_, p_365209_, p_363173_, p_369612_);
      this.recipeBookComponent.slotClicked(p_366850_);
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
   public void fillGhostRecipe(RecipeDisplay p_367089_) {
      this.recipeBookComponent.fillGhostRecipe(p_367089_);
   }
}
