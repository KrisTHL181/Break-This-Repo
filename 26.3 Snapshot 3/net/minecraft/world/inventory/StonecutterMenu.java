package net.minecraft.world.inventory;

import java.util.List;
import java.util.Optional;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class StonecutterMenu extends AbstractContainerMenu {
   public static final int INPUT_SLOT = 0;
   public static final int RESULT_SLOT = 1;
   private static final int INV_SLOT_START = 2;
   private static final int INV_SLOT_END = 29;
   private static final int USE_ROW_SLOT_START = 29;
   private static final int USE_ROW_SLOT_END = 38;
   private final ContainerLevelAccess access;
   private final DataSlot selectedRecipeIndex = DataSlot.standalone();
   private final Level level;
   private SelectableRecipe.SingleInputSet<StonecutterRecipe> recipesForInput = SelectableRecipe.SingleInputSet.empty();
   private ItemStack input = ItemStack.EMPTY;
   private long lastSoundTime;
   private final Slot inputSlot;
   private final Slot resultSlot;
   private Runnable slotUpdateListener = () -> {};
   public final Container container = new SimpleContainer(1) {
      @Override
      public void setChanged() {
         super.setChanged();
         StonecutterMenu.this.slotsChanged(this);
         StonecutterMenu.this.slotUpdateListener.run();
      }
   };
   private final ResultContainer resultContainer = new ResultContainer();

   public StonecutterMenu(final int containerId, final Inventory inventory) {
      this(containerId, inventory, ContainerLevelAccess.NULL);
   }

   public StonecutterMenu(final int containerId, final Inventory inventory, final ContainerLevelAccess access) {
      super(MenuType.STONECUTTER, containerId);
      this.access = access;
      this.level = inventory.player.level();
      this.inputSlot = this.addSlot(new Slot(this.container, 0, 20, 33));
      this.resultSlot = this.addSlot(new Slot(this.resultContainer, 1, 143, 33) {
         @Override
         public boolean mayPlace(final ItemStack itemStack) {
            return false;
         }

         @Override
         public void onTake(final Player player, final ItemStack carried) {
            carried.onCraftedBy(player, carried.getCount());
            StonecutterMenu.this.resultContainer.awardUsedRecipes(player, this.getRelevantItems());
            ItemStack remaining = StonecutterMenu.this.inputSlot.remove(1);
            if (!remaining.isEmpty()) {
               StonecutterMenu.this.setupResultSlot(StonecutterMenu.this.selectedRecipeIndex.get());
            }

            access.execute((level, pos) -> {
               long gameTime = level.getGameTime();
               if (StonecutterMenu.this.lastSoundTime != gameTime) {
                  level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                  StonecutterMenu.this.lastSoundTime = gameTime;
               }
            });
            super.onTake(player, carried);
         }

         private List<ItemStack> getRelevantItems() {
            return List.of(StonecutterMenu.this.inputSlot.getItem());
         }
      });
      this.addStandardInventorySlots(inventory, 8, 84);
      this.addDataSlot(this.selectedRecipeIndex);
   }

   public int getSelectedRecipeIndex() {
      return this.selectedRecipeIndex.get();
   }

   public SelectableRecipe.SingleInputSet<StonecutterRecipe> getVisibleRecipes() {
      return this.recipesForInput;
   }

   public int getNumberOfVisibleRecipes() {
      return this.recipesForInput.size();
   }

   public boolean hasInputItem() {
      return this.inputSlot.hasItem() && !this.recipesForInput.isEmpty();
   }

   @Override
   public boolean stillValid(final Player player) {
      return stillValid(this.access, player, Blocks.STONECUTTER);
   }

   @Override
   public boolean clickMenuButton(final Player player, final int buttonId) {
      if (this.selectedRecipeIndex.get() == buttonId) {
         return false;
      }

      if (this.isValidRecipeIndex(buttonId)) {
         this.selectedRecipeIndex.set(buttonId);
         this.setupResultSlot(buttonId);
      }

      return true;
   }

   private boolean isValidRecipeIndex(final int buttonId) {
      return buttonId >= 0 && buttonId < this.recipesForInput.size();
   }

   @Override
   public void slotsChanged(final Container container) {
      ItemStack input = this.inputSlot.getItem();
      if (!input.is(this.input.getItem())) {
         this.input = input.copy();
         this.setupRecipeList(input);
      }
   }

   private void setupRecipeList(final ItemStack item) {
      this.selectedRecipeIndex.set(-1);
      this.resultSlot.set(ItemStack.EMPTY);
      if (!item.isEmpty()) {
         this.recipesForInput = this.level.recipeAccess().stonecutterRecipes().selectByInput(item);
      } else {
         this.recipesForInput = SelectableRecipe.SingleInputSet.empty();
      }
   }

   private void setupResultSlot(final int index) {
      Optional<RecipeHolder<StonecutterRecipe>> usedRecipe;
      if (!this.recipesForInput.isEmpty() && this.isValidRecipeIndex(index)) {
         SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry = this.recipesForInput.entries().get(index);
         usedRecipe = entry.recipe().recipe();
      } else {
         usedRecipe = Optional.empty();
      }

      usedRecipe.ifPresentOrElse(recipe -> {
         this.resultContainer.setRecipeUsed((RecipeHolder<?>)recipe);
         this.resultSlot.set(recipe.value().assemble(new SingleRecipeInput(this.container.getItem(0))));
      }, () -> {
         this.resultSlot.set(ItemStack.EMPTY);
         this.resultContainer.setRecipeUsed(null);
      });
      this.broadcastChanges();
   }

   @Override
   public MenuType<?> getType() {
      return MenuType.STONECUTTER;
   }

   public void registerUpdateListener(final Runnable slotUpdateListener) {
      this.slotUpdateListener = slotUpdateListener;
   }

   @Override
   public boolean canTakeItemForPickAll(final ItemStack carried, final Slot target) {
      return target.container != this.resultContainer && super.canTakeItemForPickAll(carried, target);
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      ItemStack clicked = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         Item item = stack.getItem();
         clicked = stack.copy();
         if (slotIndex == 1) {
            item.onCraftedBy(stack, player);
            if (!this.moveItemStackTo(stack, 2, 38, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(stack, clicked);
         } else if (slotIndex == 0) {
            if (!this.moveItemStackTo(stack, 2, 38, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.level.recipeAccess().stonecutterRecipes().acceptsInput(stack)) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (slotIndex >= 2 && slotIndex < 29) {
            if (!this.moveItemStackTo(stack, 29, 38, false)) {
               return ItemStack.EMPTY;
            }
         } else if (slotIndex >= 29 && slotIndex < 38 && !this.moveItemStackTo(stack, 2, 29, false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         }

         slot.setChanged();
         if (stack.getCount() == clicked.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(player, stack);
         if (slotIndex == 1) {
            player.drop(stack, false);
         }

         this.broadcastChanges();
      }

      return clicked;
   }

   @Override
   public void removed(final Player player) {
      super.removed(player);
      this.resultContainer.removeItemNoUpdate(1);
      this.access.execute((level, pos) -> this.clearContainer(player, this.container));
   }
}
