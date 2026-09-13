package net.minecraft.world.entity.player;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

public class StackedItemContents {
   private final StackedContents<Holder<Item>> raw = new StackedContents<>();

   public void accountSimpleStack(ItemStack p_364422_) {
      if (Inventory.isUsableForCrafting(p_364422_)) {
         this.accountStack(p_364422_);
      }
   }

   public void accountStack(ItemStack p_367488_) {
      this.accountStack(p_367488_, p_367488_.getMaxStackSize());
   }

   public void accountStack(ItemStack p_363064_, int p_362894_) {
      if (!p_363064_.isEmpty()) {
         int i = Math.min(p_362894_, p_363064_.getCount());
         this.raw.account(p_363064_.getItemHolder(), i);
      }
   }

   public boolean canCraft(Recipe<?> p_366312_, StackedContents.@Nullable Output<Holder<Item>> p_369339_) {
      return this.canCraft(p_366312_, 1, p_369339_);
   }

   public boolean canCraft(Recipe<?> p_365279_, int p_366048_, StackedContents.@Nullable Output<Holder<Item>> p_369851_) {
      PlacementInfo placementinfo = p_365279_.placementInfo();
      return placementinfo.isImpossibleToPlace() ? false : this.canCraft(placementinfo.ingredients(), p_366048_, p_369851_);
   }

   public boolean canCraft(List<? extends StackedContents.IngredientInfo<Holder<Item>>> p_366143_, StackedContents.@Nullable Output<Holder<Item>> p_370013_) {
      return this.canCraft(p_366143_, 1, p_370013_);
   }

   private boolean canCraft(
      List<? extends StackedContents.IngredientInfo<Holder<Item>>> p_366198_, int p_367643_, StackedContents.@Nullable Output<Holder<Item>> p_366621_
   ) {
      return this.raw.tryPick(p_366198_, p_367643_, p_366621_);
   }

   public int getBiggestCraftableStack(Recipe<?> p_369356_, StackedContents.@Nullable Output<Holder<Item>> p_368498_) {
      return this.getBiggestCraftableStack(p_369356_, Integer.MAX_VALUE, p_368498_);
   }

   public int getBiggestCraftableStack(Recipe<?> p_366627_, int p_361474_, StackedContents.@Nullable Output<Holder<Item>> p_366799_) {
      return this.raw.tryPickAll(p_366627_.placementInfo().ingredients(), p_361474_, p_366799_);
   }

   public void clear() {
      this.raw.clear();
   }
}
