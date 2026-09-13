package net.minecraft.world.item;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public interface ItemProvider {
   boolean hasNext();

   void restart();

   ItemStack next() throws NoSuchElementException;

   ItemStack peek() throws NoSuchElementException;

   boolean findNext(Predicate<ItemStack> predicate);

   default boolean findNextNonEmpty() {
      return this.findNext(s -> !s.isEmpty());
   }

   default ItemProvider orElseProvide(final ItemStack fallbackItem) {
      return new ItemProvider.FallbackItemProvider(this, fallbackItem);
   }

   static ItemProvider of(final ItemStack... items) {
      return new ArrayItemProvider(items);
   }

   static ItemProvider of(final Collection<ItemStack> items) {
      return of(items.toArray(new ItemStack[0]));
   }

   static ItemProvider cycle(final ItemStack... items) {
      return new ArrayItemProvider.Cycled(items);
   }

   static ItemProvider cycle(final Collection<ItemStack> items) {
      return cycle(items.toArray(new ItemStack[0]));
   }

   class FallbackItemProvider implements ItemProvider {
      private final ItemProvider itemProvider;
      private final ItemStack fallbackItem;
      private boolean isUsingFallback;

      public FallbackItemProvider(final ItemProvider itemProvider, final ItemStack fallbackItem) {
         this.itemProvider = itemProvider;
         this.fallbackItem = fallbackItem;
      }

      @Override
      public boolean hasNext() {
         return true;
      }

      @Override
      public void restart() {
         this.itemProvider.restart();
         this.isUsingFallback = false;
      }

      @Override
      public ItemProvider orElseProvide(final ItemStack fallbackItem) {
         return this;
      }

      @Override
      public ItemStack peek() {
         if (!this.isUsingFallback) {
            if (this.itemProvider.hasNext()) {
               return this.itemProvider.peek();
            }

            this.isUsingFallback = true;
         }

         return this.fallbackItem.copy();
      }

      @Override
      public ItemStack next() {
         if (!this.isUsingFallback) {
            if (this.itemProvider.hasNext()) {
               return this.itemProvider.next();
            }

            this.isUsingFallback = true;
         }

         return this.fallbackItem.copy();
      }

      @Override
      public boolean findNext(final Predicate<ItemStack> predicate) {
         if (this.isUsingFallback) {
            return predicate.test(this.fallbackItem);
         } else if (this.itemProvider.findNext(predicate)) {
            return true;
         } else if (predicate.test(this.fallbackItem)) {
            this.isUsingFallback = true;
            return true;
         } else {
            return false;
         }
      }
   }
}
