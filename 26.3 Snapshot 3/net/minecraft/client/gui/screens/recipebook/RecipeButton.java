package net.minecraft.client.gui.screens.recipebook;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

public class RecipeButton extends AbstractWidget {
   private static final Identifier SLOT_MANY_CRAFTABLE_SPRITE = Identifier.withDefaultNamespace("recipe_book/slot_many_craftable");
   private static final Identifier SLOT_CRAFTABLE_SPRITE = Identifier.withDefaultNamespace("recipe_book/slot_craftable");
   private static final Identifier SLOT_MANY_UNCRAFTABLE_SPRITE = Identifier.withDefaultNamespace("recipe_book/slot_many_uncraftable");
   private static final Identifier SLOT_UNCRAFTABLE_SPRITE = Identifier.withDefaultNamespace("recipe_book/slot_uncraftable");
   private static final float ANIMATION_TIME = 15.0F;
   private static final int BACKGROUND_SIZE = 25;
   private static final Component MORE_RECIPES_TOOLTIP = Component.translatable("gui.recipebook.moreRecipes");
   private RecipeCollection collection = RecipeCollection.EMPTY;
   private List<RecipeButton.ResolvedEntry> selectedEntries = List.of();
   private boolean allRecipesHaveSameResultDisplay;
   private final SlotSelectTime slotSelectTime;
   private float animationTime;

   public RecipeButton(final SlotSelectTime slotSelectTime) {
      super(0, 0, 25, 25, CommonComponents.EMPTY);
      this.slotSelectTime = slotSelectTime;
   }

   public void init(final RecipeCollection collection, final boolean isFiltering, final RecipeBookPage page, final ContextMap resolutionContext) {
      this.collection = collection;
      List<RecipeDisplayEntry> fittingRecipes = collection.getSelectedRecipes(
         isFiltering ? RecipeCollection.CraftableStatus.CRAFTABLE : RecipeCollection.CraftableStatus.ANY
      );
      this.selectedEntries = fittingRecipes.stream().map(entry -> new RecipeButton.ResolvedEntry(entry.id(), entry.resultItems(resolutionContext))).toList();
      this.allRecipesHaveSameResultDisplay = allRecipesHaveSameResultDisplay(this.selectedEntries);
      List<RecipeDisplayId> newlyShownRecipes = fittingRecipes.stream().map(RecipeDisplayEntry::id).filter(page.getRecipeBook()::willHighlight).toList();
      if (!newlyShownRecipes.isEmpty()) {
         newlyShownRecipes.forEach(page::recipeShown);
         this.animationTime = 15.0F;
      }
   }

   private static boolean allRecipesHaveSameResultDisplay(final List<RecipeButton.ResolvedEntry> entries) {
      Iterator<ItemStack> itemsIterator = entries.stream().flatMap(e -> e.displayItems().stream()).iterator();
      if (!itemsIterator.hasNext()) {
         return true;
      }

      ItemStack firstItem = itemsIterator.next();

      while (itemsIterator.hasNext()) {
         ItemStack nextItem = itemsIterator.next();
         if (!ItemStack.isSameItemSameComponents(firstItem, nextItem)) {
            return false;
         }
      }

      return true;
   }

   public RecipeCollection getCollection() {
      return this.collection;
   }

   @Override
   public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      Identifier sprite;
      if (this.collection.hasCraftable()) {
         if (this.hasMultipleRecipes()) {
            sprite = SLOT_MANY_CRAFTABLE_SPRITE;
         } else {
            sprite = SLOT_CRAFTABLE_SPRITE;
         }
      } else if (this.hasMultipleRecipes()) {
         sprite = SLOT_MANY_UNCRAFTABLE_SPRITE;
      } else {
         sprite = SLOT_UNCRAFTABLE_SPRITE;
      }

      boolean shouldAnimate = this.animationTime > 0.0F;
      if (shouldAnimate) {
         float squeeze = 1.0F + 0.1F * (float)Math.sin(this.animationTime / 15.0F * (float) Math.PI);
         graphics.pose().pushMatrix();
         graphics.pose().translate(this.getX() + 8, this.getY() + 12);
         graphics.pose().scale(squeeze, squeeze);
         graphics.pose().translate(-(this.getX() + 8), -(this.getY() + 12));
         this.animationTime -= a;
      }

      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);
      ItemStack currentItemStack = this.getDisplayStack();
      int offset = 4;
      if (this.hasMultipleRecipes() && this.allRecipesHaveSameResultDisplay) {
         graphics.item(currentItemStack, this.getX() + offset + 1, this.getY() + offset + 1, 0);
         offset--;
      }

      graphics.fakeItem(currentItemStack, this.getX() + offset, this.getY() + offset);
      if (shouldAnimate) {
         graphics.pose().popMatrix();
      }
   }

   private boolean hasMultipleRecipes() {
      return this.selectedEntries.size() > 1;
   }

   public boolean isOnlyOption() {
      return this.selectedEntries.size() == 1;
   }

   public RecipeDisplayId getCurrentRecipe() {
      int index = this.slotSelectTime.currentIndex() % this.selectedEntries.size();
      return this.selectedEntries.get(index).id;
   }

   public ItemStack getDisplayStack() {
      int currentIndex = this.slotSelectTime.currentIndex();
      int entryCount = this.selectedEntries.size();
      int offsetIndex = currentIndex / entryCount;
      int entryIndex = currentIndex - entryCount * offsetIndex;
      return this.selectedEntries.get(entryIndex).selectItem(offsetIndex);
   }

   public List<Component> getTooltipText(final ItemStack displayStack) {
      List<Component> texts = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), displayStack));
      if (this.hasMultipleRecipes()) {
         texts.add(MORE_RECIPES_TOOLTIP);
      }

      return texts;
   }

   @Override
   public void updateWidgetNarration(final NarrationElementOutput output) {
      output.add(NarratedElementType.TITLE, Component.translatable("narration.recipe", this.getDisplayStack().getHoverName()));
      if (this.hasMultipleRecipes()) {
         output.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"), Component.translatable("narration.recipe.usage.more"));
      } else {
         output.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
      }
   }

   @Override
   public int getWidth() {
      return 25;
   }

   @Override
   protected boolean isValidClickButton(final MouseButtonInfo buttonInfo) {
      return buttonInfo.button() == 0 || buttonInfo.button() == 1;
   }

   private record ResolvedEntry(RecipeDisplayId id, List<ItemStack> displayItems) {
      public ItemStack selectItem(final int index) {
         if (this.displayItems.isEmpty()) {
            return ItemStack.EMPTY;
         }

         int offset = index % this.displayItems.size();
         return this.displayItems.get(offset);
      }
   }
}
