package net.minecraft.world.inventory;

import java.util.List;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public abstract class AbstractFurnaceMenu extends RecipeBookMenu {
   public static final int INGREDIENT_SLOT = 0;
   public static final int FUEL_SLOT = 1;
   public static final int RESULT_SLOT = 2;
   public static final int SLOT_COUNT = 3;
   public static final int DATA_COUNT = 4;
   private static final int INV_SLOT_START = 3;
   private static final int INV_SLOT_END = 30;
   private static final int USE_ROW_SLOT_START = 30;
   private static final int USE_ROW_SLOT_END = 39;
   private final Container container;
   private final ContainerData data;
   protected final Level level;
   private final RecipePropertySet acceptedInputs;
   private final RecipeBookType recipeBookType;

   protected AbstractFurnaceMenu(
      final MenuType<?> menuType,
      final ResourceKey<RecipePropertySet> allowedInputs,
      final RecipeBookType recipeBookType,
      final int containerId,
      final Inventory inventory
   ) {
      this(menuType, allowedInputs, recipeBookType, containerId, inventory, new SimpleContainer(3), new SimpleContainerData(4));
   }

   protected AbstractFurnaceMenu(
      final MenuType<?> menuType,
      final ResourceKey<RecipePropertySet> allowedInputs,
      final RecipeBookType recipeBookType,
      final int containerId,
      final Inventory inventory,
      final Container container,
      final ContainerData data
   ) {
      super(menuType, containerId);
      this.recipeBookType = recipeBookType;
      checkContainerSize(container, 3);
      checkContainerDataCount(data, 4);
      this.container = container;
      this.data = data;
      this.level = inventory.player.level();
      this.acceptedInputs = this.level.recipeAccess().propertySet(allowedInputs);
      this.addSlot(new Slot(container, 0, 56, 17));
      this.addSlot(new FurnaceFuelSlot(this, container, 1, 56, 53));
      this.addSlot(new FurnaceResultSlot(inventory.player, container, 2, 116, 35));
      this.addStandardInventorySlots(inventory, 8, 84);
      this.addDataSlots(data);
   }

   @Override
   public void fillCraftSlotsStackedContents(final StackedItemContents stackedContents) {
      if (this.container instanceof StackedContentsCompatible stackedContentsCompatible) {
         stackedContentsCompatible.fillStackedContents(stackedContents);
      }
   }

   public Slot getResultSlot() {
      return this.slots.get(2);
   }

   @Override
   public boolean stillValid(final Player player) {
      return this.container.stillValid(player);
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      ItemStack clicked = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         clicked = stack.copy();
         if (slotIndex == 2) {
            if (!this.moveItemStackTo(stack, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(stack, clicked);
         } else if (slotIndex != 1 && slotIndex != 0) {
            if (this.canSmelt(stack)) {
               if (!this.moveItemStackTo(stack, 0, 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (this.isFuel(stack)) {
               if (!this.moveItemStackTo(stack, 1, 2, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (slotIndex >= 3 && slotIndex < 30) {
               if (!this.moveItemStackTo(stack, 30, 39, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (slotIndex >= 30 && slotIndex < 39 && !this.moveItemStackTo(stack, 3, 30, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, 3, 39, false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (stack.getCount() == clicked.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(player, stack);
      }

      return clicked;
   }

   protected boolean canSmelt(final ItemStack itemStack) {
      return this.acceptedInputs.test(itemStack);
   }

   protected boolean isFuel(final ItemStack itemStack) {
      return this.level.fuelValues().isFuel(itemStack);
   }

   public float getBurnProgress() {
      int current = this.data.get(2);
      int total = this.data.get(3);
      return total != 0 && current != 0 ? Mth.clamp((float)current / total, 0.0F, 1.0F) : 0.0F;
   }

   public float getLitProgress() {
      int litDuration = this.data.get(1);
      if (litDuration == 0) {
         litDuration = 200;
      }

      return Mth.clamp((float)this.data.get(0) / litDuration, 0.0F, 1.0F);
   }

   public boolean isLit() {
      return this.data.get(0) > 0;
   }

   @Override
   public RecipeBookType getRecipeBookType() {
      return this.recipeBookType;
   }

   @Override
   public RecipeBookMenu.PostPlaceAction handlePlacement(
      final boolean useMaxItems, final boolean allowDroppingItemsToClear, final RecipeHolder<?> recipe, final ServerLevel level, final Inventory inventory
   ) {
      final List<Slot> slotsToClear = List.of(this.getSlot(0), this.getSlot(2));
      RecipeHolder<AbstractCookingRecipe> typedRecipe = (RecipeHolder<AbstractCookingRecipe>)recipe;
      return ServerPlaceRecipe.placeRecipe(new ServerPlaceRecipe.CraftingMenuAccess<AbstractCookingRecipe>() {
         @Override
         public void fillCraftSlotsStackedContents(final StackedItemContents stackedContents) {
            AbstractFurnaceMenu.this.fillCraftSlotsStackedContents(stackedContents);
         }

         @Override
         public void clearCraftingContent() {
            slotsToClear.forEach(s -> s.set(ItemStack.EMPTY));
         }

         @Override
         public boolean recipeMatches(final RecipeHolder<AbstractCookingRecipe> recipe) {
            return recipe.value().matches(new SingleRecipeInput(AbstractFurnaceMenu.this.container.getItem(0)), level);
         }
      }, 1, 1, List.of(this.getSlot(0)), slotsToClear, inventory, typedRecipe, useMaxItems, allowDroppingItemsToClear);
   }
}
