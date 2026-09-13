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
   final DataSlot selectedRecipeIndex = DataSlot.standalone();
   private final Level level;
   private SelectableRecipe.SingleInputSet<StonecutterRecipe> recipesForInput = SelectableRecipe.SingleInputSet.empty();
   private ItemStack input = ItemStack.EMPTY;
   long lastSoundTime;
   final Slot inputSlot;
   final Slot resultSlot;
   Runnable slotUpdateListener = () -> {};
   public final Container container = new SimpleContainer(1) {
      @Override
      public void setChanged() {
         super.setChanged();
         StonecutterMenu.this.slotsChanged(this);
         StonecutterMenu.this.slotUpdateListener.run();
      }
   };
   final ResultContainer resultContainer = new ResultContainer();

   public StonecutterMenu(int p_40294_, Inventory p_40295_) {
      this(p_40294_, p_40295_, ContainerLevelAccess.NULL);
   }

   public StonecutterMenu(int p_40297_, Inventory p_40298_, final ContainerLevelAccess p_40299_) {
      super(MenuType.STONECUTTER, p_40297_);
      this.access = p_40299_;
      this.level = p_40298_.player.level();
      this.inputSlot = this.addSlot(new Slot(this.container, 0, 20, 33));
      this.resultSlot = this.addSlot(new Slot(this.resultContainer, 1, 143, 33) {
         @Override
         public boolean mayPlace(ItemStack p_40362_) {
            return false;
         }

         @Override
         public void onTake(Player p_150672_, ItemStack p_150673_) {
            p_150673_.onCraftedBy(p_150672_, p_150673_.getCount());
            StonecutterMenu.this.resultContainer.awardUsedRecipes(p_150672_, this.getRelevantItems());
            ItemStack itemstack = StonecutterMenu.this.inputSlot.remove(1);
            if (!itemstack.isEmpty()) {
               StonecutterMenu.this.setupResultSlot(StonecutterMenu.this.selectedRecipeIndex.get());
            }

            p_40299_.execute((p_449772_, p_449773_) -> {
               long i = p_449772_.getGameTime();
               if (StonecutterMenu.this.lastSoundTime != i) {
                  p_449772_.playSound(null, p_449773_, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                  StonecutterMenu.this.lastSoundTime = i;
               }
            });
            super.onTake(p_150672_, p_150673_);
         }

         private List<ItemStack> getRelevantItems() {
            return List.of(StonecutterMenu.this.inputSlot.getItem());
         }
      });
      this.addStandardInventorySlots(p_40298_, 8, 84);
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
   public boolean stillValid(Player p_40307_) {
      return stillValid(this.access, p_40307_, Blocks.STONECUTTER);
   }

   @Override
   public boolean clickMenuButton(Player p_40309_, int p_40310_) {
      if (this.selectedRecipeIndex.get() == p_40310_) {
         return false;
      }

      if (this.isValidRecipeIndex(p_40310_)) {
         this.selectedRecipeIndex.set(p_40310_);
         this.setupResultSlot(p_40310_);
      }

      return true;
   }

   private boolean isValidRecipeIndex(int p_40335_) {
      return p_40335_ >= 0 && p_40335_ < this.recipesForInput.size();
   }

   @Override
   public void slotsChanged(Container p_40302_) {
      ItemStack itemstack = this.inputSlot.getItem();
      if (!itemstack.is(this.input.getItem())) {
         this.input = itemstack.copy();
         this.setupRecipeList(itemstack);
      }
   }

   private void setupRecipeList(ItemStack p_40305_) {
      this.selectedRecipeIndex.set(-1);
      this.resultSlot.set(ItemStack.EMPTY);
      if (!p_40305_.isEmpty()) {
         this.recipesForInput = this.level.recipeAccess().stonecutterRecipes().selectByInput(p_40305_);
      } else {
         this.recipesForInput = SelectableRecipe.SingleInputSet.empty();
      }
   }

   void setupResultSlot(int p_366661_) {
      Optional<RecipeHolder<StonecutterRecipe>> optional;
      if (!this.recipesForInput.isEmpty() && this.isValidRecipeIndex(p_366661_)) {
         SelectableRecipe.SingleInputEntry<StonecutterRecipe> singleinputentry = this.recipesForInput.entries().get(p_366661_);
         optional = singleinputentry.recipe().recipe();
      } else {
         optional = Optional.empty();
      }

      optional.ifPresentOrElse(p_359376_ -> {
         this.resultContainer.setRecipeUsed((RecipeHolder<?>)p_359376_);
         this.resultSlot.set(p_359376_.value().assemble(new SingleRecipeInput(this.container.getItem(0)), this.level.registryAccess()));
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

   public void registerUpdateListener(Runnable p_40324_) {
      this.slotUpdateListener = p_40324_;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack p_40321_, Slot p_40322_) {
      return p_40322_.container != this.resultContainer && super.canTakeItemForPickAll(p_40321_, p_40322_);
   }

   @Override
   public ItemStack quickMoveStack(Player p_40328_, int p_40329_) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = this.slots.get(p_40329_);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         Item item = itemstack1.getItem();
         itemstack = itemstack1.copy();
         if (p_40329_ == 1) {
            item.onCraftedBy(itemstack1, p_40328_);
            if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(itemstack1, itemstack);
         } else if (p_40329_ == 0) {
            if (!this.moveItemStackTo(itemstack1, 2, 38, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.level.recipeAccess().stonecutterRecipes().acceptsInput(itemstack1)) {
            if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (p_40329_ >= 2 && p_40329_ < 29) {
            if (!this.moveItemStackTo(itemstack1, 29, 38, false)) {
               return ItemStack.EMPTY;
            }
         } else if (p_40329_ >= 29 && p_40329_ < 38 && !this.moveItemStackTo(itemstack1, 2, 29, false)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         }

         slot.setChanged();
         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(p_40328_, itemstack1);
         if (p_40329_ == 1) {
            p_40328_.drop(itemstack1, false);
         }

         this.broadcastChanges();
      }

      return itemstack;
   }

   @Override
   public void removed(Player p_40326_) {
      super.removed(p_40326_);
      this.resultContainer.removeItemNoUpdate(1);
      this.access.execute((p_40313_, p_40314_) -> this.clearContainer(p_40326_, this.container));
   }
}
