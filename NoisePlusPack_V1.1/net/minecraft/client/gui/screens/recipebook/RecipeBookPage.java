package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RecipeBookPage {
   public static final int ITEMS_PER_PAGE = 20;
   private static final WidgetSprites PAGE_FORWARD_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("recipe_book/page_forward"), Identifier.withDefaultNamespace("recipe_book/page_forward_highlighted")
   );
   private static final WidgetSprites PAGE_BACKWARD_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("recipe_book/page_backward"), Identifier.withDefaultNamespace("recipe_book/page_backward_highlighted")
   );
   private static final Component NEXT_PAGE_TEXT = Component.translatable("gui.recipebook.next_page");
   private static final Component PREVIOUS_PAGE_TEXT = Component.translatable("gui.recipebook.previous_page");
   private static final int TURN_PAGE_SPRITE_WIDTH = 12;
   private static final int TURN_PAGE_SPRITE_HEIGHT = 17;
   private final List<RecipeButton> buttons = Lists.newArrayListWithCapacity(20);
   private @Nullable RecipeButton hoveredButton;
   private final OverlayRecipeComponent overlay;
   private Minecraft minecraft;
   private final RecipeBookComponent<?> parent;
   private List<RecipeCollection> recipeCollections = ImmutableList.of();
   private @Nullable ImageButton forwardButton;
   private @Nullable ImageButton backButton;
   private int totalPages;
   private int currentPage;
   private ClientRecipeBook recipeBook;
   private @Nullable RecipeDisplayId lastClickedRecipe;
   private @Nullable RecipeCollection lastClickedRecipeCollection;
   private boolean isFiltering;

   public RecipeBookPage(RecipeBookComponent<?> p_367288_, SlotSelectTime p_360942_, boolean p_361010_) {
      this.parent = p_367288_;
      this.overlay = new OverlayRecipeComponent(p_360942_, p_361010_);

      for (int i = 0; i < 20; i++) {
         this.buttons.add(new RecipeButton(p_360942_));
      }
   }

   public void init(Minecraft p_100429_, int p_100430_, int p_100431_) {
      this.minecraft = p_100429_;
      this.recipeBook = p_100429_.player.getRecipeBook();

      for (int i = 0; i < this.buttons.size(); i++) {
         this.buttons.get(i).setPosition(p_100430_ + 11 + 25 * (i % 5), p_100431_ + 31 + 25 * (i / 5));
      }

      this.forwardButton = new ImageButton(
         p_100430_ + 93, p_100431_ + 137, 12, 17, PAGE_FORWARD_SPRITES, p_459670_ -> this.updateArrowButtons(), NEXT_PAGE_TEXT
      );
      this.forwardButton.setTooltip(Tooltip.create(NEXT_PAGE_TEXT));
      this.backButton = new ImageButton(
         p_100430_ + 38, p_100431_ + 137, 12, 17, PAGE_BACKWARD_SPRITES, p_457357_ -> this.updateArrowButtons(), PREVIOUS_PAGE_TEXT
      );
      this.backButton.setTooltip(Tooltip.create(PREVIOUS_PAGE_TEXT));
   }

   public void updateCollections(List<RecipeCollection> p_100437_, boolean p_100438_, boolean p_363187_) {
      this.recipeCollections = p_100437_;
      this.isFiltering = p_363187_;
      this.totalPages = (int)Math.ceil(p_100437_.size() / 20.0);
      if (this.totalPages <= this.currentPage || p_100438_) {
         this.currentPage = 0;
      }

      this.updateButtonsForPage();
   }

   private void updateButtonsForPage() {
      int i = 20 * this.currentPage;
      ContextMap contextmap = SlotDisplayContext.fromLevel(this.minecraft.level);

      for (int j = 0; j < this.buttons.size(); j++) {
         RecipeButton recipebutton = this.buttons.get(j);
         if (i + j < this.recipeCollections.size()) {
            RecipeCollection recipecollection = this.recipeCollections.get(i + j);
            recipebutton.init(recipecollection, this.isFiltering, this, contextmap);
            recipebutton.visible = true;
         } else {
            recipebutton.visible = false;
         }
      }

      this.updateArrowButtons();
   }

   private void updateArrowButtons() {
      if (this.forwardButton != null) {
         this.forwardButton.visible = this.totalPages > 1 && this.currentPage < this.totalPages - 1;
      }

      if (this.backButton != null) {
         this.backButton.visible = this.totalPages > 1 && this.currentPage > 0;
      }
   }

   public void render(GuiGraphics p_281416_, int p_281888_, int p_281904_, int p_282278_, int p_282424_, float p_281712_) {
      if (this.totalPages > 1) {
         Component component = Component.translatable("gui.recipebook.page", this.currentPage + 1, this.totalPages);
         int i = this.minecraft.font.width(component);
         p_281416_.drawString(this.minecraft.font, component, p_281888_ - i / 2 + 73, p_281904_ + 141, -1);
      }

      this.hoveredButton = null;

      for (RecipeButton recipebutton : this.buttons) {
         recipebutton.render(p_281416_, p_282278_, p_282424_, p_281712_);
         if (recipebutton.visible && recipebutton.isHoveredOrFocused()) {
            this.hoveredButton = recipebutton;
         }
      }

      if (this.forwardButton != null) {
         this.forwardButton.render(p_281416_, p_282278_, p_282424_, p_281712_);
      }

      if (this.backButton != null) {
         this.backButton.render(p_281416_, p_282278_, p_282424_, p_281712_);
      }

      p_281416_.nextStratum();
      this.overlay.render(p_281416_, p_282278_, p_282424_, p_281712_);
   }

   public void renderTooltip(GuiGraphics p_283690_, int p_282626_, int p_282490_) {
      if (this.minecraft.screen != null && this.hoveredButton != null && !this.overlay.isVisible()) {
         ItemStack itemstack = this.hoveredButton.getDisplayStack();
         Identifier identifier = itemstack.get(DataComponents.TOOLTIP_STYLE);
         p_283690_.setComponentTooltipForNextFrame(this.minecraft.font, this.hoveredButton.getTooltipText(itemstack), p_282626_, p_282490_, identifier);
      }
   }

   public @Nullable RecipeDisplayId getLastClickedRecipe() {
      return this.lastClickedRecipe;
   }

   public @Nullable RecipeCollection getLastClickedRecipeCollection() {
      return this.lastClickedRecipeCollection;
   }

   public void setInvisible() {
      this.overlay.setVisible(false);
   }

   public boolean mouseClicked(MouseButtonEvent p_430164_, int p_100412_, int p_100413_, int p_100414_, int p_100415_, boolean p_424902_) {
      this.lastClickedRecipe = null;
      this.lastClickedRecipeCollection = null;
      if (this.overlay.isVisible()) {
         if (this.overlay.mouseClicked(p_430164_, p_424902_)) {
            this.lastClickedRecipe = this.overlay.getLastRecipeClicked();
            this.lastClickedRecipeCollection = this.overlay.getRecipeCollection();
         } else {
            this.overlay.setVisible(false);
         }

         return true;
      } else {
         if (this.forwardButton.mouseClicked(p_430164_, p_424902_)) {
            this.currentPage++;
            this.updateButtonsForPage();
            return true;
         }

         if (this.backButton.mouseClicked(p_430164_, p_424902_)) {
            this.currentPage--;
            this.updateButtonsForPage();
            return true;
         }

         ContextMap contextmap = SlotDisplayContext.fromLevel(this.minecraft.level);

         for (RecipeButton recipebutton : this.buttons) {
            if (recipebutton.mouseClicked(p_430164_, p_424902_)) {
               if (p_430164_.button() == 0) {
                  this.lastClickedRecipe = recipebutton.getCurrentRecipe();
                  this.lastClickedRecipeCollection = recipebutton.getCollection();
               } else if (p_430164_.button() == 1 && !this.overlay.isVisible() && !recipebutton.isOnlyOption()) {
                  this.overlay
                     .init(
                        recipebutton.getCollection(),
                        contextmap,
                        this.isFiltering,
                        recipebutton.getX(),
                        recipebutton.getY(),
                        p_100412_ + p_100414_ / 2,
                        p_100413_ + 13 + p_100415_ / 2,
                        recipebutton.getWidth()
                     );
               }

               return true;
            }
         }

         return false;
      }
   }

   public void recipeShown(RecipeDisplayId p_363823_) {
      this.parent.recipeShown(p_363823_);
   }

   public ClientRecipeBook getRecipeBook() {
      return this.recipeBook;
   }

   protected void listButtons(Consumer<AbstractWidget> p_170054_) {
      this.buttons.forEach(p_170054_);
   }
}
