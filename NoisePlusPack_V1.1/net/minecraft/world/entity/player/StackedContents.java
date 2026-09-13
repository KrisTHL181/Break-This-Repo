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

   boolean hasAtLeast(T p_364618_, int p_365723_) {
      return this.amounts.getInt(p_364618_) >= p_365723_;
   }

   void take(T p_366589_, int p_36457_) {
      int i = this.amounts.addTo(p_366589_, -p_36457_);
      if (i < p_36457_) {
         throw new IllegalStateException("Took " + p_36457_ + " items, but only had " + i);
      }
   }

   void put(T p_360774_, int p_36485_) {
      this.amounts.addTo(p_360774_, p_36485_);
   }

   public boolean tryPick(List<? extends StackedContents.IngredientInfo<T>> p_369350_, int p_363945_, StackedContents.@Nullable Output<T> p_367932_) {
      return new StackedContents.RecipePicker(p_369350_).tryPick(p_363945_, p_367932_);
   }

   public int tryPickAll(List<? extends StackedContents.IngredientInfo<T>> p_364447_, int p_369029_, StackedContents.@Nullable Output<T> p_367758_) {
      return new StackedContents.RecipePicker(p_364447_).tryPickAll(p_369029_, p_367758_);
   }

   public void clear() {
      this.amounts.clear();
   }

   public void account(T p_367526_, int p_363519_) {
      this.put(p_367526_, p_363519_);
   }

   List<T> getUniqueAvailableIngredientItems(Iterable<? extends StackedContents.IngredientInfo<T>> p_375762_) {
      List<T> list = new ArrayList<>();
      ObjectIterator var3 = Reference2IntMaps.fastIterable(this.amounts).iterator();

      while (var3.hasNext()) {
         Entry<T> entry = (Entry<T>)var3.next();
         if (entry.getIntValue() > 0 && anyIngredientMatches(p_375762_, (T)entry.getKey())) {
            list.add((T)entry.getKey());
         }
      }

      return list;
   }

   private static <T> boolean anyIngredientMatches(Iterable<? extends StackedContents.IngredientInfo<T>> p_377979_, T p_378788_) {
      for (StackedContents.IngredientInfo<T> ingredientinfo : p_377979_) {
         if (ingredientinfo.acceptsItem(p_378788_)) {
            return true;
         }
      }

      return false;
   }

   @VisibleForTesting
   public int getResultUpperBound(List<? extends StackedContents.IngredientInfo<T>> p_378094_) {
      int i = Integer.MAX_VALUE;
      ObjectIterable<Entry<T>> objectiterable = Reference2IntMaps.fastIterable(this.amounts);

      label30:
      for (StackedContents.IngredientInfo<T> ingredientinfo : p_378094_) {
         int j = 0;
         ObjectIterator var7 = objectiterable.iterator();

         while (var7.hasNext()) {
            Entry<T> entry = (Entry<T>)var7.next();
            int k = entry.getIntValue();
            if (k > j) {
               if (ingredientinfo.acceptsItem((T)entry.getKey())) {
                  j = k;
               }

               if (j >= i) {
                  continue label30;
               }
            }
         }

         i = j;
         if (j == 0) {
            break;
         }
      }

      return i;
   }

   @FunctionalInterface
   public interface IngredientInfo<T> {
      boolean acceptsItem(T var1);
   }

   @FunctionalInterface
   public interface Output<T> {
      void accept(T var1);
   }

   class RecipePicker {
      private final List<? extends StackedContents.IngredientInfo<T>> ingredients;
      private final int ingredientCount;
      private final List<T> items;
      private final int itemCount;
      private final BitSet data;
      private final IntList path = new IntArrayList();

      public RecipePicker(final List<? extends StackedContents.IngredientInfo<T>> p_370054_) {
         this.ingredients = p_370054_;
         this.ingredientCount = p_370054_.size();
         this.items = StackedContents.this.getUniqueAvailableIngredientItems(p_370054_);
         this.itemCount = this.items.size();
         this.data = new BitSet(this.visitedIngredientCount() + this.visitedItemCount() + this.satisfiedCount() + this.connectionCount() + this.residualCount());
         this.setInitialConnections();
      }

      private void setInitialConnections() {
         for (int i = 0; i < this.ingredientCount; i++) {
            StackedContents.IngredientInfo<T> ingredientinfo = (StackedContents.IngredientInfo<T>)this.ingredients.get(i);

            for (int j = 0; j < this.itemCount; j++) {
               if (ingredientinfo.acceptsItem(this.items.get(j))) {
                  this.setConnection(j, i);
               }
            }
         }
      }

      public boolean tryPick(int p_36513_, StackedContents.@Nullable Output<T> p_362244_) {
         if (p_36513_ <= 0) {
            return true;
         }

         int i = 0;

         while (true) {
            IntList intlist = this.tryAssigningNewItem(p_36513_);
            if (intlist == null) {
               boolean flag = i == this.ingredientCount;
               boolean flag1 = flag && p_362244_ != null;
               this.clearAllVisited();
               this.clearSatisfied();

               for (int k1 = 0; k1 < this.ingredientCount; k1++) {
                  for (int l1 = 0; l1 < this.itemCount; l1++) {
                     if (this.isAssigned(l1, k1)) {
                        this.unassign(l1, k1);
                        StackedContents.this.put(this.items.get(l1), p_36513_);
                        if (flag1) {
                           p_362244_.accept(this.items.get(l1));
                        }
                        break;
                     }
                  }
               }

               assert this.data.get(this.residualOffset(), this.residualOffset() + this.residualCount()).isEmpty();
               return flag;
            }

            int j = intlist.getInt(0);
            StackedContents.this.take(this.items.get(j), p_36513_);
            int k = intlist.size() - 1;
            this.setSatisfied(intlist.getInt(k));
            i++;

            for (int l = 0; l < intlist.size() - 1; l++) {
               if (isPathIndexItem(l)) {
                  int i1 = intlist.getInt(l);
                  int j1 = intlist.getInt(l + 1);
                  this.assign(i1, j1);
               } else {
                  int i2 = intlist.getInt(l + 1);
                  int j2 = intlist.getInt(l);
                  this.unassign(i2, j2);
               }
            }
         }
      }

      private static boolean isPathIndexItem(int p_364021_) {
         return (p_364021_ & 1) == 0;
      }

      private @Nullable IntList tryAssigningNewItem(int p_362499_) {
         this.clearAllVisited();

         for (int i = 0; i < this.itemCount; i++) {
            if (StackedContents.this.hasAtLeast(this.items.get(i), p_362499_)) {
               IntList intlist = this.findNewItemAssignmentPath(i);
               if (intlist != null) {
                  return intlist;
               }
            }
         }

         return null;
      }

      private @Nullable IntList findNewItemAssignmentPath(int p_365332_) {
         this.path.clear();
         this.visitItem(p_365332_);
         this.path.add(p_365332_);

         while (!this.path.isEmpty()) {
            int i = this.path.size();
            if (isPathIndexItem(i - 1)) {
               int l = this.path.getInt(i - 1);

               for (int j1 = 0; j1 < this.ingredientCount; j1++) {
                  if (!this.hasVisitedIngredient(j1) && this.hasConnection(l, j1) && !this.isAssigned(l, j1)) {
                     this.visitIngredient(j1);
                     this.path.add(j1);
                     break;
                  }
               }
            } else {
               int j = this.path.getInt(i - 1);
               if (!this.isSatisfied(j)) {
                  return this.path;
               }

               for (int k = 0; k < this.itemCount; k++) {
                  if (!this.hasVisitedItem(k) && this.isAssigned(k, j)) {
                     assert this.hasConnection(k, j);
                     this.visitItem(k);
                     this.path.add(k);
                     break;
                  }
               }
            }

            int i1 = this.path.size();
            if (i1 == i) {
               this.path.removeInt(i1 - 1);
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

      private boolean isSatisfied(int p_36524_) {
         return this.data.get(this.getSatisfiedIndex(p_36524_));
      }

      private void setSatisfied(int p_36536_) {
         this.data.set(this.getSatisfiedIndex(p_36536_));
      }

      private int getSatisfiedIndex(int p_36545_) {
         assert p_36545_ >= 0 && p_36545_ < this.ingredientCount;
         return this.satisfiedOffset() + p_36545_;
      }

      private void clearSatisfied() {
         this.clearRange(this.satisfiedOffset(), this.satisfiedCount());
      }

      private void setConnection(int p_363439_, int p_361612_) {
         this.data.set(this.getConnectionIndex(p_363439_, p_361612_));
      }

      private boolean hasConnection(int p_36520_, int p_36521_) {
         return this.data.get(this.getConnectionIndex(p_36520_, p_36521_));
      }

      private int getConnectionIndex(int p_368635_, int p_361826_) {
         assert p_368635_ >= 0 && p_368635_ < this.itemCount;
         assert p_361826_ >= 0 && p_361826_ < this.ingredientCount;
         return this.connectionOffset() + p_368635_ * this.ingredientCount + p_361826_;
      }

      private boolean isAssigned(int p_361137_, int p_362327_) {
         return this.data.get(this.getResidualIndex(p_361137_, p_362327_));
      }

      private void assign(int p_369076_, int p_369424_) {
         int i = this.getResidualIndex(p_369076_, p_369424_);
         assert !this.data.get(i);
         this.data.set(i);
      }

      private void unassign(int p_365832_, int p_369102_) {
         int i = this.getResidualIndex(p_365832_, p_369102_);
         assert this.data.get(i);
         this.data.clear(i);
      }

      private int getResidualIndex(int p_362820_, int p_368001_) {
         assert p_362820_ >= 0 && p_362820_ < this.itemCount;
         assert p_368001_ >= 0 && p_368001_ < this.ingredientCount;
         return this.residualOffset() + p_362820_ * this.ingredientCount + p_368001_;
      }

      private void visitIngredient(int p_365987_) {
         this.data.set(this.getVisitedIngredientIndex(p_365987_));
      }

      private boolean hasVisitedIngredient(int p_363422_) {
         return this.data.get(this.getVisitedIngredientIndex(p_363422_));
      }

      private int getVisitedIngredientIndex(int p_367746_) {
         assert p_367746_ >= 0 && p_367746_ < this.ingredientCount;
         return this.visitedIngredientOffset() + p_367746_;
      }

      private void visitItem(int p_368859_) {
         this.data.set(this.getVisitiedItemIndex(p_368859_));
      }

      private boolean hasVisitedItem(int p_361000_) {
         return this.data.get(this.getVisitiedItemIndex(p_361000_));
      }

      private int getVisitiedItemIndex(int p_369625_) {
         assert p_369625_ >= 0 && p_369625_ < this.itemCount;
         return this.visitedItemOffset() + p_369625_;
      }

      private void clearAllVisited() {
         this.clearRange(this.visitedIngredientOffset(), this.visitedIngredientCount());
         this.clearRange(this.visitedItemOffset(), this.visitedItemCount());
      }

      private void clearRange(int p_365189_, int p_366696_) {
         this.data.clear(p_365189_, p_365189_ + p_366696_);
      }

      public int tryPickAll(int p_36526_, StackedContents.@Nullable Output<T> p_367532_) {
         int i = 0;
         int j = Math.min(p_36526_, StackedContents.this.getResultUpperBound(this.ingredients)) + 1;

         while (true) {
            int k = (i + j) / 2;
            if (this.tryPick(k, null)) {
               if (j - i <= 1) {
                  if (k > 0) {
                     this.tryPick(k, p_367532_);
                  }

                  return k;
               }

               i = k;
            } else {
               j = k;
            }
         }
      }
   }
}
