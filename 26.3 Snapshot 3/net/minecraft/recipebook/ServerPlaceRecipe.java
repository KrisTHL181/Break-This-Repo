package net.minecraft.recipebook;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;

public class ServerPlaceRecipe<R extends Recipe<?>> {
   private static final int ITEM_NOT_FOUND = -1;
   private final Inventory inventory;
   private final ServerPlaceRecipe.CraftingMenuAccess<R> menu;
   private final boolean useMaxItems;
   private final int gridWidth;
   private final int gridHeight;
   private final List<Slot> inputGridSlots;
   private final List<Slot> slotsToClear;

   public static <I extends RecipeInput, R extends Recipe<I>> RecipeBookMenu.PostPlaceAction placeRecipe(
      final ServerPlaceRecipe.CraftingMenuAccess<R> menu,
      final int gridWidth,
      final int gridHeight,
      final List<Slot> inputGridSlots,
      final List<Slot> slotsToClear,
      final Inventory inventory,
      final RecipeHolder<R> recipe,
      final boolean useMaxItems,
      final boolean allowDroppingItemsToClear
   ) {
      ServerPlaceRecipe<R> placer = new ServerPlaceRecipe<>(menu, inventory, useMaxItems, gridWidth, gridHeight, inputGridSlots, slotsToClear);
      if (!allowDroppingItemsToClear && !placer.testClearGrid()) {
         return RecipeBookMenu.PostPlaceAction.NOTHING;
      }

      StackedItemContents availableItems = new StackedItemContents();
      inventory.fillStackedContents(availableItems);
      menu.fillCraftSlotsStackedContents(availableItems);
      return placer.tryPlaceRecipe(recipe, availableItems);
   }

   private ServerPlaceRecipe(
      final ServerPlaceRecipe.CraftingMenuAccess<R> menu,
      final Inventory inventory,
      final boolean useMaxItems,
      final int gridWidth,
      final int gridHeight,
      final List<Slot> inputGridSlots,
      final List<Slot> slotsToClear
   ) {
      this.menu = menu;
      this.inventory = inventory;
      this.useMaxItems = useMaxItems;
      this.gridWidth = gridWidth;
      this.gridHeight = gridHeight;
      this.inputGridSlots = inputGridSlots;
      this.slotsToClear = slotsToClear;
   }

   private RecipeBookMenu.PostPlaceAction tryPlaceRecipe(final RecipeHolder<R> recipe, final StackedItemContents availableItems) {
      if (availableItems.canCraft(recipe.value(), null)) {
         this.placeRecipe(recipe, availableItems);
         this.inventory.setChanged();
         return RecipeBookMenu.PostPlaceAction.NOTHING;
      } else {
         this.clearGrid();
         this.inventory.setChanged();
         return RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE;
      }
   }

   private void clearGrid() {
      for (Slot slot : this.slotsToClear) {
         ItemStack itemStackCopy = slot.getItem().copy();
         this.inventory.placeItemBackInInventory(itemStackCopy, false);
         slot.set(itemStackCopy);
      }

      this.menu.clearCraftingContent();
   }

   private void placeRecipe(final RecipeHolder<R> recipe, final StackedItemContents availableItems) {
      boolean recipeMatchesPlaced = this.menu.recipeMatches(recipe);
      int biggestCraftableStack = availableItems.getBiggestCraftableStack(recipe.value(), null);
      if (recipeMatchesPlaced) {
         for (Slot inputSlot : this.inputGridSlots) {
            ItemStack itemStack = inputSlot.getItem();
            if (!itemStack.isEmpty() && Math.min(biggestCraftableStack, itemStack.getMaxStackSize()) < itemStack.getCount() + 1) {
               return;
            }
         }
      }

      int amountToCraft = this.calculateAmountToCraft(biggestCraftableStack, recipeMatchesPlaced);
      List<Holder<Item>> itemsUsedPerIngredient = new ArrayList<>();
      if (availableItems.canCraft(recipe.value(), amountToCraft, itemsUsedPerIngredient::add)) {
         int adjustedAmountToCraft = clampToMaxStackSize(amountToCraft, itemsUsedPerIngredient);
         if (adjustedAmountToCraft != amountToCraft) {
            itemsUsedPerIngredient.clear();
            if (!availableItems.canCraft(recipe.value(), adjustedAmountToCraft, itemsUsedPerIngredient::add)) {
               return;
            }
         }

         this.clearGrid();
         PlaceRecipeHelper.placeRecipe(
            this.gridWidth,
            this.gridHeight,
            recipe.value(),
            recipe.value().placementInfo().slotsToIngredientIndex(),
            (ingredientIndex, gridIndex, gridXPos, gridYPos) -> {
               if (ingredientIndex != -1) {
                  Slot targetGridSlot = this.inputGridSlots.get(gridIndex);
                  Holder<Item> itemUsed = itemsUsedPerIngredient.get(ingredientIndex);
                  int remainingCount = adjustedAmountToCraft;

                  while (remainingCount > 0) {
                     remainingCount = this.moveItemToGrid(targetGridSlot, itemUsed, remainingCount);
                     if (remainingCount == -1) {
                        return;
                     }
                  }
               }
            }
         );
      }
   }

   private static int clampToMaxStackSize(int value, final List<Holder<Item>> items) {
      for (Holder<Item> item : items) {
         value = Math.min(value, item.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 1));
      }

      return value;
   }

   private int calculateAmountToCraft(final int biggestCraftableStack, final boolean recipeMatchesPlaced) {
      if (this.useMaxItems) {
         return biggestCraftableStack;
      }

      if (recipeMatchesPlaced) {
         int smallestStackSize = Integer.MAX_VALUE;

         for (Slot inputSlot : this.inputGridSlots) {
            ItemStack itemStack = inputSlot.getItem();
            if (!itemStack.isEmpty() && smallestStackSize > itemStack.getCount()) {
               smallestStackSize = itemStack.getCount();
            }
         }

         if (smallestStackSize != Integer.MAX_VALUE) {
            smallestStackSize++;
         }

         return smallestStackSize;
      } else {
         return 1;
      }
   }

   private int moveItemToGrid(final Slot targetSlot, final Holder<Item> itemInInventory, final int count) {
      ItemStack itemInTargetSlot = targetSlot.getItem();
      int inventorySlotId = this.inventory.findSlotMatchingCraftingIngredient(itemInInventory, itemInTargetSlot);
      if (inventorySlotId == -1) {
         return -1;
      }

      ItemStack inventoryItem = this.inventory.getItem(inventorySlotId);
      ItemStack takenStack;
      if (count < inventoryItem.getCount()) {
         takenStack = this.inventory.removeItem(inventorySlotId, count);
      } else {
         takenStack = this.inventory.removeItemNoUpdate(inventorySlotId);
      }

      int takenCount = takenStack.getCount();
      if (itemInTargetSlot.isEmpty()) {
         targetSlot.set(takenStack);
      } else {
         itemInTargetSlot.grow(takenCount);
      }

      return count - takenCount;
   }

   private boolean testClearGrid() {
      List<ItemStack> freeSlots = Lists.newArrayList();
      int freeSlotsInInventory = this.getAmountOfFreeSlotsInInventory();

      for (Slot inputSlot : this.inputGridSlots) {
         ItemStack itemStack = inputSlot.getItem().copy();
         if (!itemStack.isEmpty()) {
            int slotId = this.inventory.getSlotWithRemainingSpace(itemStack);
            if (slotId == -1 && freeSlots.size() <= freeSlotsInInventory) {
               for (ItemStack itemStackInList : freeSlots) {
                  if (ItemStack.isSameItem(itemStackInList, itemStack)
                     && itemStackInList.getCount() != itemStackInList.getMaxStackSize()
                     && itemStackInList.getCount() + itemStack.getCount() <= itemStackInList.getMaxStackSize()) {
                     itemStackInList.grow(itemStack.getCount());
                     itemStack.setCount(0);
                     break;
                  }
               }

               if (!itemStack.isEmpty()) {
                  if (freeSlots.size() >= freeSlotsInInventory) {
                     return false;
                  }

                  freeSlots.add(itemStack);
               }
            } else if (slotId == -1) {
               return false;
            }
         }
      }

      return true;
   }

   private int getAmountOfFreeSlotsInInventory() {
      int freeSlots = 0;

      for (ItemStack item : this.inventory.getNonEquipmentItems()) {
         if (item.isEmpty()) {
            freeSlots++;
         }
      }

      return freeSlots;
   }

   public interface CraftingMenuAccess<T extends Recipe<?>> {
      void fillCraftSlotsStackedContents(StackedItemContents stackedContents);

      void clearCraftingContent();

      boolean recipeMatches(RecipeHolder<T> recipe);
   }
}
