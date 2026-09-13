package net.minecraft.world;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ContainerHelper {
   public static final String TAG_ITEMS = "Items";

   public static ItemStack removeItem(List<ItemStack> p_18970_, int p_18971_, int p_18972_) {
      return p_18971_ >= 0 && p_18971_ < p_18970_.size() && !p_18970_.get(p_18971_).isEmpty() && p_18972_ > 0
         ? p_18970_.get(p_18971_).split(p_18972_)
         : ItemStack.EMPTY;
   }

   public static ItemStack takeItem(List<ItemStack> p_18967_, int p_18968_) {
      return p_18968_ >= 0 && p_18968_ < p_18967_.size() ? p_18967_.set(p_18968_, ItemStack.EMPTY) : ItemStack.EMPTY;
   }

   public static void saveAllItems(ValueOutput p_408970_, NonNullList<ItemStack> p_18978_) {
      saveAllItems(p_408970_, p_18978_, true);
   }

   public static void saveAllItems(ValueOutput p_409332_, NonNullList<ItemStack> p_18975_, boolean p_336339_) {
      ValueOutput.TypedOutputList<ItemStackWithSlot> typedoutputlist = p_409332_.list("Items", ItemStackWithSlot.CODEC);

      for (int i = 0; i < p_18975_.size(); i++) {
         ItemStack itemstack = p_18975_.get(i);
         if (!itemstack.isEmpty()) {
            typedoutputlist.add(new ItemStackWithSlot(i, itemstack));
         }
      }

      if (typedoutputlist.isEmpty() && !p_336339_) {
         p_409332_.discard("Items");
      }
   }

   public static void loadAllItems(ValueInput p_408837_, NonNullList<ItemStack> p_18982_) {
      for (ItemStackWithSlot itemstackwithslot : p_408837_.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
         if (itemstackwithslot.isValidInContainer(p_18982_.size())) {
            p_18982_.set(itemstackwithslot.slot(), itemstackwithslot.stack());
         }
      }
   }

   public static int clearOrCountMatchingItems(Container p_18957_, Predicate<ItemStack> p_18958_, int p_18959_, boolean p_18960_) {
      int i = 0;

      for (int j = 0; j < p_18957_.getContainerSize(); j++) {
         ItemStack itemstack = p_18957_.getItem(j);
         int k = clearOrCountMatchingItems(itemstack, p_18958_, p_18959_ - i, p_18960_);
         if (k > 0 && !p_18960_ && itemstack.isEmpty()) {
            p_18957_.setItem(j, ItemStack.EMPTY);
         }

         i += k;
      }

      return i;
   }

   public static int clearOrCountMatchingItems(ItemStack p_18962_, Predicate<ItemStack> p_18963_, int p_18964_, boolean p_18965_) {
      if (p_18962_.isEmpty() || !p_18963_.test(p_18962_)) {
         return 0;
      }

      if (p_18965_) {
         return p_18962_.getCount();
      }

      int i = p_18964_ < 0 ? p_18962_.getCount() : Math.min(p_18964_, p_18962_.getCount());
      p_18962_.shrink(i);
      return i;
   }
}
