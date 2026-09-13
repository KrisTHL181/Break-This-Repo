package net.minecraft.world.entity.player;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectIterable;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.jspecify.annotations.Nullable;

public class StackedContents<T> {
   public final Reference2IntOpenHashMap<T> amounts = new Reference2IntOpenHashMap();

   private boolean hasAtLeast(final T item, final int count) {
      return this.amounts.getInt(item) >= count;
   }

   private void take(final T item, final int amount) {
      int previous = this.amounts.addTo(item, -amount);
      if (previous < amount) {
         throw new IllegalStateException("Took " + amount + " items, but only had " + previous);
      }
   }

   private void put(final T item, final int count) {
      this.amounts.addTo(item, count);
   }

   public boolean tryPick(
      final List<? extends StackedContents.IngredientInfo<T>> ingredients, final int amount, final StackedContents.@Nullable Output<T> output
   ) {
      return new StackedContents.RecipePicker(ingredients).tryPick(amount, output);
   }

   public int tryPickAll(
      final List<? extends StackedContents.IngredientInfo<T>> ingredients, final int maxSize, final StackedContents.@Nullable Output<T> output
   ) {
      return new StackedContents.RecipePicker(ingredients).tryPickAll(maxSize, output);
   }

   public void clear() {
      this.amounts.clear();
   }

   public void account(final T item, final int count) {
      this.put(item, count);
   }

   private List<T> getUniqueAvailableIngredientItems(final Iterable<? extends StackedContents.IngredientInfo<T>> ingredients) {
      List<T> result = new ArrayList<>();
      ObjectIterator var3 = Reference2IntMaps.fastIterable(this.amounts).iterator();

      while (var3.hasNext()) {
         Entry<T> availableItem = (Entry<T>)var3.next();
         if (availableItem.getIntValue() > 0 && anyIngredientMatches(ingredients, (T)availableItem.getKey())) {
            result.add((T)availableItem.getKey());
         }
      }

      return result;
   }

   private static <T> boolean anyIngredientMatches(final Iterable<? extends StackedContents.IngredientInfo<T>> ingredients, final T item) {
      for (StackedContents.IngredientInfo<T> ingredient : ingredients) {
         if (ingredient.acceptsItem(item)) {
            return true;
         }
      }

      return false;
   }

   @VisibleForTesting
   public int getResultUpperBound(final List<? extends StackedContents.IngredientInfo<T>> ingredients) {
      int min = Integer.MAX_VALUE;
      ObjectIterable<Entry<T>> availableItems = Reference2IntMaps.fastIterable(this.amounts);

      label31:
      for (StackedContents.IngredientInfo<T> ingredient : ingredients) {
         int max = 0;
         ObjectIterator var7 = availableItems.iterator();

         while (var7.hasNext()) {
            Entry<T> entry = (Entry<T>)var7.next();
            int itemCount = entry.getIntValue();
            if (itemCount > max) {
               if (ingredient.acceptsItem((T)entry.getKey())) {
                  max = itemCount;
               }

               if (max >= min) {
                  continue label31;
               }
            }
         }

         min = max;
         if (min == 0) {
            break;
         }
      }

      return min;
   }

   @FunctionalInterface
   public interface IngredientInfo<T> {
      boolean acceptsItem(T item);
   }

   @FunctionalInterface
   public interface Output<T> {
      void accept(T item);
   }

   private class RecipePicker {
      private final List<? extends StackedContents.IngredientInfo<T>> ingredients;
      private final int ingredientCount;
      private final List<T> items;
      private final int itemCount;
      private final BitSet data;
      private final IntList path = new IntArrayList();

      public RecipePicker(final List<? extends StackedContents.IngredientInfo<T>> ingredients) {
         this.ingredients = ingredients;
         this.ingredientCount = ingredients.size();
         this.items = StackedContents.this.getUniqueAvailableIngredientItems(ingredients);
         this.itemCount = this.items.size();
         this.data = new BitSet(this.visitedIngredientCount() + this.visitedItemCount() + this.satisfiedCount() + this.connectionCount() + this.residualCount());
         this.setInitialConnections();
      }

      private void setInitialConnections() {
         for (int ingredient = 0; ingredient < this.ingredientCount; ingredient++) {
            StackedContents.IngredientInfo<T> ingredientInfo = (StackedContents.IngredientInfo<T>)this.ingredients.get(ingredient);

            for (int item = 0; item < this.itemCount; item++) {
               if (ingredientInfo.acceptsItem(this.items.get(item))) {
                  this.setConnection(item, ingredient);
               }
            }
         }
      }

      public boolean tryPick(final int capacity, final StackedContents.@Nullable Output<T> output) {
         if (capacity <= 0) {
            return true;
         }

         int satisfiedIngredientCount = 0;

         while (true) {
            IntList path = this.tryAssigningNewItem(capacity);
            if (path == null) {
               boolean isValidAssignment = satisfiedIngredientCount == this.ingredientCount;
               boolean hasOutput = isValidAssignment && output != null;
               this.clearAllVisited();
               this.clearSatisfied();

               for (int ingredient = 0; ingredient < this.ingredientCount; ingredient++) {
                  for (int item = 0; item < this.itemCount; item++) {
                     if (this.isAssigned(item, ingredient)) {
                        this.unassign(item, ingredient);
                        StackedContents.this.put(this.items.get(item), capacity);
                        if (hasOutput) {
                           output.accept(this.items.get(item));
                        }
                        break;
                     }
                  }
               }

               assert this.data.get(this.residualOffset(), this.residualOffset() + this.residualCount()).isEmpty();
               return isValidAssignment;
            }

            int assignedItem = path.getInt(0);
            StackedContents.this.take(this.items.get(assignedItem), capacity);
            int satisfiedIngredient = path.size() - 1;
            this.setSatisfied(path.getInt(satisfiedIngredient));
            satisfiedIngredientCount++;

            for (int i = 0; i < path.size() - 1; i++) {
               if (isPathIndexItem(i)) {
                  int item = path.getInt(i);
                  int ingredient = path.getInt(i + 1);
                  this.assign(item, ingredient);
               } else {
                  int item = path.getInt(i + 1);
                  int ingredient = path.getInt(i);
                  this.unassign(item, ingredient);
               }
            }
         }
      }

      private static boolean isPathIndexItem(final int index) {
         return (index & 1) == 0;
      }

      private @Nullable IntList tryAssigningNewItem(final int capacity) {
         this.clearAllVisited();

         for (int item = 0; item < this.itemCount; item++) {
            if (StackedContents.this.hasAtLeast(this.items.get(item), capacity)) {
               IntList path = this.findNewItemAssignmentPath(item);
               if (path != null) {
                  return path;
               }
            }
         }

         return null;
      }

      private @Nullable IntList findNewItemAssignmentPath(final int startingItem) {
         this.path.clear();
         this.visitItem(startingItem);
         this.path.add(startingItem);

         while (!this.path.isEmpty()) {
            int pathLength = this.path.size();
            if (isPathIndexItem(pathLength - 1)) {
               int itemToAssign = this.path.getInt(pathLength - 1);

               for (int ingredient = 0; ingredient < this.ingredientCount; ingredient++) {
                  if (!this.hasVisitedIngredient(ingredient) && this.hasConnection(itemToAssign, ingredient) && !this.isAssigned(itemToAssign, ingredient)) {
                     this.visitIngredient(ingredient);
                     this.path.add(ingredient);
                     break;
                  }
               }
            } else {
               int lastAssignedIngredient = this.path.getInt(pathLength - 1);
               if (!this.isSatisfied(lastAssignedIngredient)) {
                  return this.path;
               }

               for (int item = 0; item < this.itemCount; item++) {
                  if (!this.hasVisitedItem(item) && this.isAssigned(item, lastAssignedIngredient)) {
                     assert this.hasConnection(item, lastAssignedIngredient);
                     this.visitItem(item);
                     this.path.add(item);
                     break;
                  }
               }
            }

            int newLength = this.path.size();
            if (newLength == pathLength) {
               this.path.removeInt(newLength - 1);
            }
         }

         return null;
      }

      private int visitedIngredientOffset() {
         return 0;
      }

      private int visitedIngredientCount() {
         return this.ingredientCount;
      }

      private int visitedItemOffset() {
         return this.visitedIngredientOffset() + this.visitedIngredientCount();
      }

      private int visitedItemCount() {
         return this.itemCount;
      }

      private int satisfiedOffset() {
         return this.visitedItemOffset() + this.visitedItemCount();
      }

      private int satisfiedCount() {
         return this.ingredientCount;
      }

      private int connectionOffset() {
         return this.satisfiedOffset() + this.satisfiedCount();
      }

      private int connectionCount() {
         return this.ingredientCount * this.itemCount;
      }

      private int residualOffset() {
         return this.connectionOffset() + this.connectionCount();
      }

      private int residualCount() {
         return this.ingredientCount * this.itemCount;
      }

      private boolean isSatisfied(final int ingredient) {
         return this.data.get(this.getSatisfiedIndex(ingredient));
      }

      private void setSatisfied(final int ingredient) {
         this.data.set(this.getSatisfiedIndex(ingredient));
      }

      private int getSatisfiedIndex(final int ingredient) {
         assert ingredient >= 0 && ingredient < this.ingredientCount;
         return this.satisfiedOffset() + ingredient;
      }

      private void clearSatisfied() {
         this.clearRange(this.satisfiedOffset(), this.satisfiedCount());
      }

      private void setConnection(final int item, final int ingredient) {
         this.data.set(this.getConnectionIndex(item, ingredient));
      }

      private boolean hasConnection(final int item, final int ingredient) {
         return this.data.get(this.getConnectionIndex(item, ingredient));
      }

      private int getConnectionIndex(final int item, final int ingredient) {
         assert item >= 0 && item < this.itemCount;
         assert ingredient >= 0 && ingredient < this.ingredientCount;
         return this.connectionOffset() + item * this.ingredientCount + ingredient;
      }

      private boolean isAssigned(final int item, final int ingredient) {
         return this.data.get(this.getResidualIndex(item, ingredient));
      }

      private void assign(final int item, final int ingredient) {
         int residualIndex = this.getResidualIndex(item, ingredient);
         assert !this.data.get(residualIndex);
         this.data.set(residualIndex);
      }

      private void unassign(final int item, final int ingredient) {
         int residualIndex = this.getResidualIndex(item, ingredient);
         assert this.data.get(residualIndex);
         this.data.clear(residualIndex);
      }

      private int getResidualIndex(final int item, final int ingredient) {
         assert item >= 0 && item < this.itemCount;
         assert ingredient >= 0 && ingredient < this.ingredientCount;
         return this.residualOffset() + item * this.ingredientCount + ingredient;
      }

      private void visitIngredient(final int item) {
         this.data.set(this.getVisitedIngredientIndex(item));
      }

      private boolean hasVisitedIngredient(final int ingredient) {
         return this.data.get(this.getVisitedIngredientIndex(ingredient));
      }

      private int getVisitedIngredientIndex(final int ingredient) {
         assert ingredient >= 0 && ingredient < this.ingredientCount;
         return this.visitedIngredientOffset() + ingredient;
      }

      private void visitItem(final int item) {
         this.data.set(this.getVisitiedItemIndex(item));
      }

      private boolean hasVisitedItem(final int item) {
         return this.data.get(this.getVisitiedItemIndex(item));
      }

      private int getVisitiedItemIndex(final int item) {
         assert item >= 0 && item < this.itemCount;
         return this.visitedItemOffset() + item;
      }

      private void clearAllVisited() {
         this.clearRange(this.visitedIngredientOffset(), this.visitedIngredientCount());
         this.clearRange(this.visitedItemOffset(), this.visitedItemCount());
      }

      private void clearRange(final int offset, final int count) {
         this.data.clear(offset, offset + count);
      }

      public int tryPickAll(final int maxSize, final StackedContents.@Nullable Output<T> output) {
         int min = 0;
         int max = Math.min(maxSize, StackedContents.this.getResultUpperBound(this.ingredients)) + 1;

         while (true) {
            int mid = (min + max) / 2;
            if (this.tryPick(mid, null)) {
               if (max - min <= 1) {
                  if (mid > 0) {
                     this.tryPick(mid, output);
                  }

                  return mid;
               }

               min = mid;
            } else {
               max = mid;
            }
         }
      }
   }
}
