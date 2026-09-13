package net.minecraft.world.item.component;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.slot.SlotSelector;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.apache.commons.lang3.math.Fraction;
import org.jspecify.annotations.Nullable;

public final class BundleContents implements ContainerComponent<BundleContents>, TooltipComponent {
   public static final BundleContents EMPTY = new BundleContents(List.of());
   public static final Codec<BundleContents> CODEC = ItemStackTemplate.CODEC.listOf().xmap(BundleContents::new, contents -> contents.items);
   public static final StreamCodec<RegistryFriendlyByteBuf, BundleContents> STREAM_CODEC = ItemStackTemplate.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(BundleContents::new, contents -> contents.items);
   private static final Fraction BUNDLE_IN_BUNDLE_WEIGHT = Fraction.getFraction(1, 16);
   private static final int NO_STACK_INDEX = -1;
   public static final int NO_SELECTED_ITEM_INDEX = -1;
   public static final DataResult<Fraction> BEEHIVE_WEIGHT = DataResult.success(Fraction.ONE);
   private final List<ItemStackTemplate> items;
   private final int selectedItem;
   private final Supplier<DataResult<Fraction>> weight;

   private BundleContents(final List<ItemStackTemplate> items, final int selectedItem) {
      this.items = items;
      this.selectedItem = selectedItem;
      this.weight = Suppliers.memoize(() -> computeContentWeight(this.items));
   }

   public BundleContents(final List<ItemStackTemplate> items) {
      this(items, -1);
   }

   private static DataResult<Fraction> computeContentWeight(final List<? extends ItemInstance> items) {
      try {
         Fraction weight = Fraction.ZERO;

         for (ItemInstance stack : items) {
            DataResult<Fraction> itemWeight = getWeight(stack);
            if (itemWeight.isError()) {
               return itemWeight;
            }

            weight = weight.add(((Fraction)itemWeight.getOrThrow()).multiplyBy(Fraction.getFraction(stack.count(), 1)));
         }

         return DataResult.success(weight);
      } catch (ArithmeticException exception) {
         return DataResult.error(() -> "Excessive total bundle weight");
      }
   }

   private static DataResult<Fraction> getWeight(final ItemInstance item) {
      BundleContents bundle = item.get(DataComponents.BUNDLE_CONTENTS);
      if (bundle != null) {
         return bundle.weight().map(nestedWeight -> nestedWeight.add(BUNDLE_IN_BUNDLE_WEIGHT));
      }

      List<BeehiveBlockEntity.Occupant> bees = item.getOrDefault(DataComponents.BEES, Bees.EMPTY).bees();
      return !bees.isEmpty() ? BEEHIVE_WEIGHT : DataResult.success(Fraction.getFraction(1, item.getMaxStackSize()));
   }

   public static boolean canItemBeInBundle(final ItemStack itemToAdd) {
      return !itemToAdd.isEmpty() && itemToAdd.getItem().canFitInsideContainerItems();
   }

   public int getNumberOfItemsToShow() {
      int numberOfItemStacks = this.size();
      int availableItemsToShow = numberOfItemStacks > 12 ? 11 : 12;
      int itemsOnNonFullRow = numberOfItemStacks % 4;
      int emptySpaceOnNonFullRow = itemsOnNonFullRow == 0 ? 0 : 4 - itemsOnNonFullRow;
      return Math.min(numberOfItemStacks, availableItemsToShow - emptySpaceOnNonFullRow);
   }

   @Override
   public Stream<ItemStack> itemCopies() {
      return this.items.stream().map(ItemStackTemplate::create);
   }

   public List<ItemStackTemplate> items() {
      return this.items;
   }

   @Override
   public int size() {
      return this.items.size();
   }

   public DataResult<Fraction> weight() {
      return this.weight.get();
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   public int getSelectedItemIndex() {
      return this.selectedItem;
   }

   public @Nullable ItemStackTemplate getSelectedItem() {
      return this.selectedItem == -1 ? null : this.items.get(this.selectedItem);
   }

   public BundleContents copyWithContents(final Stream<ItemStack> newContents) {
      BundleContents.Mutable builder = new BundleContents.Mutable();
      newContents.forEach(builder::tryInsert);
      return builder.toImmutable();
   }

   public BundleContents.Mutable asMutable() {
      DataResult<Fraction> currentWeight = this.weight.get();
      if (currentWeight.isError()) {
         return new BundleContents.Mutable();
      }

      List<ItemStack> itemsList = new ArrayList<>(this.items.size());

      for (ItemStackTemplate item : this.items) {
         itemsList.add(item.create());
      }

      return new BundleContents.Mutable(itemsList, (Fraction)currentWeight.getOrThrow(), this.selectedItem);
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      } else {
         return obj instanceof BundleContents contents ? this.items.equals(contents.items) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.items.hashCode();
   }

   @Override
   public String toString() {
      return "BundleContents" + this.items;
   }

   public static class Mutable extends SimpleMutableContainer<BundleContents> {
      private Fraction weight;
      private int selectedItem;
      private boolean needsFlattening;

      private Mutable(final List<ItemStack> items, final Fraction weight, final int selectedItem) {
         super(items);
         this.weight = weight;
         this.selectedItem = selectedItem;
      }

      public Mutable() {
         this(new ArrayList<>(), Fraction.ZERO, -1);
      }

      public BundleContents.Mutable clearItems() {
         this.items.clear();
         this.weight = Fraction.ZERO;
         this.selectedItem = -1;
         return this;
      }

      private int findStackIndexWithinRange(final ItemStack itemsToAdd, final int minInclusive, final int maxExclusive) {
         if (!itemsToAdd.isStackable()) {
            return -1;
         }

         int startIndex = Math.max(minInclusive, 0);
         int endIndex = Math.min(maxExclusive, this.items.size());

         for (int i = startIndex; i < endIndex; i++) {
            if (ItemStack.isSameItemSameComponents(this.items.get(i), itemsToAdd)) {
               return i;
            }
         }

         return -1;
      }

      private int findStackIndex(final ItemStack itemsToAdd) {
         return this.findStackIndexWithinRange(itemsToAdd, 0, this.items.size());
      }

      private int getMaxAmountToAdd(final Fraction itemWeight) {
         Fraction remainingWeight = Fraction.ONE.subtract(this.weight);
         return Math.max(remainingWeight.divideBy(itemWeight).intValue(), 0);
      }

      public int tryInsert(final ItemStack itemsToAdd) {
         if (!BundleContents.canItemBeInBundle(itemsToAdd)) {
            return 0;
         }

         DataResult<Fraction> maybeItemWeight = BundleContents.getWeight(itemsToAdd);
         if (maybeItemWeight.isError()) {
            return 0;
         }

         Fraction itemWeight = (Fraction)maybeItemWeight.getOrThrow();
         int amountToAdd = Math.min(itemsToAdd.getCount(), this.getMaxAmountToAdd(itemWeight));
         if (amountToAdd == 0) {
            return 0;
         }

         this.weight = this.weight.add(getStackedWeight(itemWeight, amountToAdd));
         int stackIndex = this.findStackIndex(itemsToAdd);
         if (stackIndex != -1) {
            ItemStack removedStack = this.items.remove(stackIndex);
            ItemStack mergedStack = removedStack.copyWithCount(removedStack.getCount() + amountToAdd);
            itemsToAdd.shrink(amountToAdd);
            this.items.addFirst(mergedStack);
         } else {
            this.items.addFirst(itemsToAdd.split(amountToAdd));
         }

         return amountToAdd;
      }

      public int tryTransfer(final Slot slot, final Player player) {
         ItemStack other = slot.getItem();
         DataResult<Fraction> itemWeight = BundleContents.getWeight(other);
         if (itemWeight.isError()) {
            return 0;
         }

         int maxAmount = this.getMaxAmountToAdd((Fraction)itemWeight.getOrThrow());
         return BundleContents.canItemBeInBundle(other) ? this.tryInsert(slot.safeTake(other.getCount(), maxAmount, player)) : 0;
      }

      public void toggleSelectedItem(final int selectedItem) {
         this.selectedItem = this.selectedItem != selectedItem && !this.indexIsOutsideAllowedBounds(selectedItem) ? selectedItem : -1;
      }

      private boolean indexIsOutsideAllowedBounds(final int selectedItem) {
         return selectedItem < 0 || selectedItem >= this.items.size();
      }

      public @Nullable ItemStack removeOne() {
         if (this.items.isEmpty()) {
            return null;
         }

         int removeIndex = this.indexIsOutsideAllowedBounds(this.selectedItem) ? 0 : this.selectedItem;
         ItemStack stack = this.items.remove(removeIndex).copy();
         this.weight = this.weight.subtract(getStackedWeight(stack));
         this.toggleSelectedItem(-1);
         return stack;
      }

      private static Fraction getStackedWeight(final Fraction weight, final int count) {
         return weight.multiplyBy(Fraction.getFraction(count, 1));
      }

      private static Fraction getStackedWeight(final ItemStack stack) {
         return getStackedWeight((Fraction)BundleContents.getWeight(stack).getOrThrow(), stack.getCount());
      }

      public Fraction weight() {
         return this.weight;
      }

      @Override
      public int replaceSlotItems(final ItemProvider newItems, final SlotSelector slotSelector) {
         this.mergeIdenticalStacks();
         return super.replaceSlotItems(newItems, slotSelector);
      }

      @Override
      public void modifySlots(final Consumer<? super SlotAccess> consumer, final SlotSelector slotSelector) {
         this.mergeIdenticalStacks();
         super.modifySlots(consumer, slotSelector);
      }

      @Override
      protected int insertNewSlots(final ItemProvider newItems, final SlotSelector slotSelector) {
         int successCount;
         for (successCount = 0; newItems.hasNext() && this.canInsertNewSlots() && slotSelector.trySelectSlot(ItemStack.EMPTY); successCount++) {
            boolean success = newItems.findNextNonEmpty() && this.addSlotWithItem(newItems.peek());
            if (!success) {
               break;
            }

            newItems.next();
         }

         return successCount;
      }

      @Override
      protected boolean setItem(final int slot, final ItemStack itemStack) {
         ItemStack currentItem = this.items.get(slot);
         Fraction adjustedWeight = currentItem.isEmpty() ? this.weight : this.weight.subtract(getStackedWeight(currentItem));
         Fraction newWeight = itemStack.isEmpty() ? adjustedWeight : getWeightWithAddedItems(adjustedWeight, itemStack);
         if (newWeight != null && super.setItem(slot, itemStack)) {
            this.weight = newWeight;
            this.needsFlattening = true;
            return true;
         } else {
            return false;
         }
      }

      @Override
      protected boolean addSlotWithItem(final ItemStack itemStack) {
         if (itemStack.isEmpty()) {
            return false;
         } else {
            Fraction newWeight = getWeightWithAddedItems(this.weight, itemStack);
            if (newWeight != null && super.addSlotWithItem(itemStack)) {
               this.weight = newWeight;
               this.needsFlattening = true;
               return true;
            } else {
               return false;
            }
         }
      }

      private static @Nullable Fraction getWeightWithAddedItems(final Fraction weight, final ItemStack itemsToAdd) {
         if (BundleContents.canItemBeInBundle(itemsToAdd)) {
            DataResult<Fraction> itemWeight = BundleContents.getWeight(itemsToAdd);
            if (itemWeight.isError()) {
               return null;
            }

            Fraction newWeight = weight.add(getStackedWeight((Fraction)itemWeight.getOrThrow(), itemsToAdd.getCount()));
            if (newWeight.compareTo(Fraction.ONE) <= 0) {
               return newWeight;
            }
         }

         return null;
      }

      @Override
      public boolean canInsertNewSlots() {
         return this.weight.compareTo(Fraction.ONE) < 0;
      }

      private void mergeIdenticalStacks() {
         if (this.needsFlattening) {
            for (int index = 0; index < this.items.size(); index++) {
               ItemStack itemStack = this.items.get(index);
               if (itemStack.isEmpty()) {
                  this.items.remove(index--);
               } else {
                  int stackIndex = this.findStackIndexWithinRange(itemStack, index + 1, this.items.size());
                  if (stackIndex != -1) {
                     ItemStack targetStack = this.items.get(stackIndex);
                     int mergedCount = itemStack.getCount() + targetStack.getCount();
                     this.items.set(stackIndex, itemStack.copyWithCount(mergedCount));
                     this.items.remove(index--);
                  }
               }
            }

            this.needsFlattening = false;
         }
      }

      public BundleContents toImmutable() {
         this.mergeIdenticalStacks();
         Builder<ItemStackTemplate> builder = ImmutableList.builder();

         for (ItemStack item : this.items) {
            builder.add(ItemStackTemplate.fromNonEmptyStack(item));
         }

         return new BundleContents(builder.build(), this.selectedItem);
      }
   }
}
