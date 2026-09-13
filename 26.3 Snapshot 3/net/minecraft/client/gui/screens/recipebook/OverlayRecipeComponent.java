package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jspecify.annotations.Nullable;

public class OverlayRecipeComponent implements GuiEventListener, Renderable {
   private static final Identifier OVERLAY_RECIPE_SPRITE = Identifier.withDefaultNamespace("recipe_book/overlay_recipe");
   private static final int MAX_ROW = 4;
   private static final int MAX_ROW_LARGE = 5;
   private static final float ITEM_RENDER_SCALE = 0.375F;
   public static final int BUTTON_SIZE = 25;
   private final List<OverlayRecipeComponent.OverlayRecipeButton> recipeButtons = Lists.newArrayList();
   private boolean isVisible;
   private int x;
   private int y;
   private RecipeCollection collection = RecipeCollection.EMPTY;
   private @Nullable RecipeDisplayId lastRecipeClicked;
   private final SlotSelectTime slotSelectTime;
   private final boolean isFurnaceMenu;

   public OverlayRecipeComponent(final SlotSelectTime slotSelectTime, final boolean isFurnaceMenu) {
      this.slotSelectTime = slotSelectTime;
      this.isFurnaceMenu = isFurnaceMenu;
   }

   public void init(
      final RecipeCollection collection,
      final ContextMap context,
      final boolean isFiltering,
      final int buttonX,
      final int buttonY,
      final int centerX,
      final int centerY,
      final float buttonWidth
   ) {
      this.collection = collection;
      List<RecipeDisplayEntry> craftable = collection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE);
      List<RecipeDisplayEntry> unCraftable = isFiltering
         ? Collections.emptyList()
         : collection.getSelectedRecipes(RecipeCollection.CraftableStatus.NOT_CRAFTABLE);
      int craftables = craftable.size();
      int total = craftables + unCraftable.size();
      int maxRow = total <= 16 ? 4 : 5;
      int rows = (int)Math.ceil((float)total / maxRow);
      this.x = buttonX;
      this.y = buttonY;
      float rightPos = this.x + Math.min(total, maxRow) * 25;
      float maxLeftPos = centerX + 50;
      if (rightPos > maxLeftPos) {
         this.x = (int)(this.x - buttonWidth * (int)((rightPos - maxLeftPos) / buttonWidth));
      }

      float bottomPos = this.y + rows * 25;
      float maxBottomPos = centerY + 50;
      if (bottomPos > maxBottomPos) {
         this.y = (int)(this.y - buttonWidth * Mth.ceil((bottomPos - maxBottomPos) / buttonWidth));
      }

      float topPos = this.y;
      float maxTopPos = centerY - 100;
      if (topPos < maxTopPos) {
         this.y = (int)(this.y - buttonWidth * Mth.ceil((topPos - maxTopPos) / buttonWidth));
      }

      this.isVisible = true;
      this.recipeButtons.clear();

      for (int i = 0; i < total; i++) {
         boolean canCraft = i < craftables;
         RecipeDisplayEntry recipe = canCraft ? craftable.get(i) : unCraftable.get(i - craftables);
         int x = this.x + 4 + 25 * (i % maxRow);
         int y = this.y + 5 + 25 * (i / maxRow);
         if (this.isFurnaceMenu) {
            this.recipeButtons.add(new OverlayRecipeComponent.OverlaySmeltingRecipeButton(x, y, recipe.id(), recipe.display(), context, canCraft));
         } else {
            this.recipeButtons.add(new OverlayRecipeComponent.OverlayCraftingRecipeButton(x, y, recipe.id(), recipe.display(), context, canCraft));
         }
      }

      this.lastRecipeClicked = null;
   }

   public RecipeCollection getRecipeCollection() {
      return this.collection;
   }

   public @Nullable RecipeDisplayId getLastRecipeClicked() {
      return this.lastRecipeClicked;
   }

   @Override
   public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
      if (event.button() != 0) {
         return false;
      }

      for (OverlayRecipeComponent.OverlayRecipeButton recipeButton : this.recipeButtons) {
         if (recipeButton.mouseClicked(event, doubleClick)) {
            this.lastRecipeClicked = recipeButton.recipe;
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean isMouseOver(final double mouseX, final double mouseY) {
      return false;
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      if (this.isVisible) {
         int maxRow = this.recipeButtons.size() <= 16 ? 4 : 5;
         int width = Math.min(this.recipeButtons.size(), maxRow);
         int height = Mth.ceil((float)this.recipeButtons.size() / maxRow);
         int border = 4;
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, OVERLAY_RECIPE_SPRITE, this.x, this.y, width * 25 + 8, height * 25 + 8);

         for (OverlayRecipeComponent.OverlayRecipeButton component : this.recipeButtons) {
            component.extractRenderState(graphics, mouseX, mouseY, a);
         }
      }
   }

   public void setVisible(final boolean visible) {
      this.isVisible = visible;
   }

   public boolean isVisible() {
      return this.isVisible;
   }

   @Override
   public void setFocused(final boolean focused) {
   }

   @Override
   public boolean isFocused() {
      return false;
   }

   private class OverlayCraftingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
      private static final Identifier ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay");
      private static final Identifier HIGHLIGHTED_ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay_highlighted");
      private static final Identifier DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay_disabled");
      private static final Identifier HIGHLIGHTED_DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay_disabled_highlighted");
      private static final int GRID_WIDTH = 3;
      private static final int GRID_HEIGHT = 3;

      public OverlayCraftingRecipeButton(
         final int x, final int y, final RecipeDisplayId id, final RecipeDisplay recipe, final ContextMap context, final boolean isCraftable
      ) {
         super(x, y, id, isCraftable, calculateIngredientsPositions(recipe, context));
      }

      private static List<OverlayRecipeComponent.OverlayRecipeButton.Pos> calculateIngredientsPositions(final RecipeDisplay recipe, final ContextMap context) {
         List<OverlayRecipeComponent.OverlayRecipeButton.Pos> result = new ArrayList<>();
         switch (recipe) {
            case ShapedCraftingRecipeDisplay shaped:
               PlaceRecipeHelper.placeRecipe(3, 3, shaped.width(), shaped.height(), shaped.ingredients(), (ingredient, gridIndex, gridXPos, gridYPos) -> {
                  List<ItemStack> itemsx = ingredient.resolveForStacks(context);
                  if (!itemsx.isEmpty()) {
                     result.add(createGridPos(gridXPos, gridYPos, itemsx));
                  }
               });
               break;
            case ShapelessCraftingRecipeDisplay shapeless:
               label19: {
                  List<SlotDisplay> ingredients = shapeless.ingredients();

                  for (int i = 0; i < ingredients.size(); i++) {
                     List<ItemStack> items = ingredients.get(i).resolveForStacks(context);
                     if (!items.isEmpty()) {
                        result.add(createGridPos(i % 3, i / 3, items));
                     }
                  }
                  break label19;
               }
            default:
         }

         return result;
      }

      @Override
      protected Identifier getSprite(final boolean isCraftable) {
         if (isCraftable) {
            return this.isHoveredOrFocused() ? HIGHLIGHTED_ENABLED_SPRITE : ENABLED_SPRITE;
         } else {
            return this.isHoveredOrFocused() ? HIGHLIGHTED_DISABLED_SPRITE : DISABLED_SPRITE;
         }
      }
   }

   private abstract class OverlayRecipeButton extends AbstractWidget {
      private final RecipeDisplayId recipe;
      private final boolean isCraftable;
      private final List<OverlayRecipeComponent.OverlayRecipeButton.Pos> slots;

      public OverlayRecipeButton(
         final int x, final int y, final RecipeDisplayId recipe, final boolean isCraftable, final List<OverlayRecipeComponent.OverlayRecipeButton.Pos> slots
      ) {
         super(x, y, 24, 24, CommonComponents.EMPTY);
         this.slots = slots;
         this.recipe = recipe;
         this.isCraftable = isCraftable;
      }

      protected static OverlayRecipeComponent.OverlayRecipeButton.Pos createGridPos(final int gridXPos, final int gridYPos, final List<ItemStack> itemStacks) {
         return new OverlayRecipeComponent.OverlayRecipeButton.Pos(3 + gridXPos * 7, 3 + gridYPos * 7, itemStacks);
      }

      protected abstract Identifier getSprite(boolean isCraftable);

      @Override
      public void updateWidgetNarration(final NarrationElementOutput output) {
         this.defaultButtonNarrationText(output);
      }

      @Override
      public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSprite(this.isCraftable), this.getX(), this.getY(), this.width, this.height);
         float gridPosX = this.getX() + 2;
         float gridPosY = this.getY() + 2;

         for (OverlayRecipeComponent.OverlayRecipeButton.Pos pos : this.slots) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(gridPosX + pos.x, gridPosY + pos.y);
            graphics.pose().scale(0.375F, 0.375F);
            graphics.pose().translate(-8.0F, -8.0F);
            graphics.item(pos.selectIngredient(OverlayRecipeComponent.this.slotSelectTime.currentIndex()), 0, 0);
            graphics.pose().popMatrix();
         }
      }

      protected record Pos(int x, int y, List<ItemStack> ingredients) {
         public Pos {
            if (ingredients.isEmpty()) {
               throw new IllegalArgumentException("Ingredient list must be non-empty");
            }
         }

         public ItemStack selectIngredient(final int currentIndex) {
            return this.ingredients.get(currentIndex % this.ingredients.size());
         }
      }
   }

   private class OverlaySmeltingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
      private static final Identifier ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay");
      private static final Identifier HIGHLIGHTED_ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay_highlighted");
      private static final Identifier DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay_disabled");
      private static final Identifier HIGHLIGHTED_DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay_disabled_highlighted");

      public OverlaySmeltingRecipeButton(
         final int x, final int y, final RecipeDisplayId id, final RecipeDisplay recipe, final ContextMap context, final boolean isCraftable
      ) {
         super(x, y, id, isCraftable, calculateIngredientsPositions(recipe, context));
      }

      private static List<OverlayRecipeComponent.OverlayRecipeButton.Pos> calculateIngredientsPositions(final RecipeDisplay recipe, final ContextMap context) {
         if (recipe instanceof FurnaceRecipeDisplay furnaceRecipe) {
            List<ItemStack> items = furnaceRecipe.ingredient().resolveForStacks(context);
            if (!items.isEmpty()) {
               return List.of(createGridPos(1, 1, items));
            }
         }

         return List.of();
      }

      @Override
      protected Identifier getSprite(final boolean isCraftable) {
         if (isCraftable) {
            return this.isHoveredOrFocused() ? HIGHLIGHTED_ENABLED_SPRITE : ENABLED_SPRITE;
         } else {
            return this.isHoveredOrFocused() ? HIGHLIGHTED_DISABLED_SPRITE : DISABLED_SPRITE;
         }
      }
   }
}
