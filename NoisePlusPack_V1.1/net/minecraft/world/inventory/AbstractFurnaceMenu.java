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
import net.minecraft.world.item.crafting.RecipeType;
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
   final Container container;
   private final ContainerData data;
   protected final Level level;
   private final RecipeType<? extends AbstractCookingRecipe> recipeType;
   private final RecipePropertySet acceptedInputs;
   private final RecipeBookType recipeBookType;

   protected AbstractFurnaceMenu(
      MenuType<?> p_38960_,
      RecipeType<? extends AbstractCookingRecipe> p_38961_,
      ResourceKey<RecipePropertySet> p_360708_,
      RecipeBookType p_38962_,
      int p_38963_,
      Inventory p_38964_
   ) {
      this(p_38960_, p_38961_, p_360708_, p_38962_, p_38963_, p_38964_, new SimpleContainer(3), new SimpleContainerData(4));
   }

   protected AbstractFurnaceMenu(
      MenuType<?> p_38966_,
      RecipeType<? extends AbstractCookingRecipe> p_38967_,
      ResourceKey<RecipePropertySet> p_365963_,
      RecipeBookType p_38968_,
      int p_38969_,
      Inventory p_38970_,
      Container p_38971_,
      ContainerData p_38972_
   ) {
      super(p_38966_, p_38969_);
      this.recipeType = p_38967_;
      this.recipeBookType = p_38968_;
      checkContainerSize(p_38971_, 3);
      checkContainerDataCount(p_38972_, 4);
      this.container = p_38971_;
      this.data = p_38972_;
      this.level = p_38970_.player.level();
      this.acceptedInputs = this.level.recipeAccess().propertySet(p_365963_);
      this.addSlot(new Slot(p_38971_, 0, 56, 17));
      this.addSlot(new FurnaceFuelSlot(this, p_38971_, 1, 56, 53));
      this.addSlot(new FurnaceResultSlot(p_38970_.player, p_38971_, 2, 116, 35));
      this.addStandardInventorySlots(p_38970_, 8, 84);
      this.addDataSlots(p_38972_);
   }

   @Override
   public void fillCraftSlotsStackedContents(StackedItemContents p_364624_) {
      if (this.container instanceof StackedContentsCompatible) {
         ((StackedContentsCompatible)this.container).fillStackedContents(p_364624_);
      }
   }

   public Slot getResultSlot() {
      return this.slots.get(2);
   }

   @Override
   public boolean stillValid(Player p_38974_) {
      return this.container.stillValid(p_38974_);
   }

   @Override
   public ItemStack quickMoveStack(Player p_38986_, int p_38987_) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = this.slots.get(p_38987_);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (p_38987_ == 2) {
            if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(itemstack1, itemstack);
         } else if (p_38987_ != 1 && p_38987_ != 0) {
            if (this.canSmelt(itemstack1)) {
               if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (this.isFuel(itemstack1)) {
               if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (p_38987_ >= 3 && p_38987_ < 30) {
               if (!this.moveItemStackTo(itemstack1, 30, 39, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (p_38987_ >= 30 && p_38987_ < 39 && !this.moveItemStackTo(itemstack1, 3, 30, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(p_38986_, itemstack1);
      }

      return itemstack;
   }

   protected boolean canSmelt(ItemStack p_38978_) {
      return this.acceptedInputs.test(p_38978_);
   }

   protected boolean isFuel(ItemStack p_38989_) {
      return this.level.fuelValues().isFuel(p_38989_);
   }

   public float getBurnProgress() {
      int i = this.data.get(2);
      int j = this.data.get(3);
      return j != 0 && i != 0 ? Mth.clamp((float)i / j, 0.0F, 1.0F) : 0.0F;
   }

   public float getLitProgress() {
      int i = this.data.get(1);
      if (i == 0) {
         i = 200;
      }

      return Mth.clamp((float)this.data.get(0) / i, 0.0F, 1.0F);
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
      boolean p_366505_, boolean p_361487_, RecipeHolder<?> p_366286_, final ServerLevel p_366253_, Inventory p_364103_
   ) {
      final List<Slot> list = List.of(this.getSlot(0), this.getSlot(2));
      return ServerPlaceRecipe.placeRecipe(new ServerPlaceRecipe.CraftingMenuAccess<AbstractCookingRecipe>() {
         @Override
         public void fillCraftSlotsStackedContents(StackedItemContents p_366344_) {
            AbstractFurnaceMenu.this.fillCraftSlotsStackedContents(p_366344_);
         }

         @Override
         public void clearCraftingContent() {
            list.forEach(p_365059_ -> p_365059_.set(ItemStack.EMPTY));
         }

         @Override
         public boolean recipeMatches(RecipeHolder<AbstractCookingRecipe> p_363054_) {
            return p_363054_.value().matches(new SingleRecipeInput(AbstractFurnaceMenu.this.container.getItem(0)), p_366253_);
         }
      }, 1, 1, List.of(this.getSlot(0)), list, p_364103_, (RecipeHolder<AbstractCookingRecipe>)p_366286_, p_366505_, p_361487_);
   }
}
