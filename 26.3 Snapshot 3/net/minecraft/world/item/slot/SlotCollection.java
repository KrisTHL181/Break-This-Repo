package net.minecraft.world.item.slot;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemProvider;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableInt;

public interface SlotCollection {
   SlotCollection EMPTY = new SlotCollection() {
      @Override
      public Stream<ItemStack> itemCopies() {
         return Stream.empty();
      }

      @Override
      public int size() {
         return 0;
      }

      @Override
      public int replaceSlotItems(final ItemProvider items, final SlotSelector slotSelector) {
         return 0;
      }

      @Override
      public void modifySlots(final Consumer<? super SlotAccess> consumer, final SlotSelector slotSelector) {
      }
   };

   Stream<ItemStack> itemCopies();

   int size();

   int replaceSlotItems(ItemProvider items, SlotSelector slotSelector);

   default int replaceSlotItems(final ItemProvider items) {
      return this.replaceSlotItems(items, SlotSelector.ANY_SLOT);
   }

   void modifySlots(Consumer<? super SlotAccess> consumer, SlotSelector slotSelector);

   default void modifySlots(final Consumer<? super SlotAccess> consumer) {
      this.modifySlots(consumer, SlotSelector.ANY_SLOT);
   }

   default SlotCollection filter(final Predicate<? super ItemStack> predicate) {
      return new SlotCollection.Filtered(this, predicate);
   }

   default SlotCollection flatMap(final Function<ItemStack, ? extends SlotCollection> mapper) {
      return new SlotCollection.FlatMapped(this, mapper);
   }

   default SlotCollection limit(final int limit) {
      return new SlotCollection.Limited(this, limit);
   }

   static SlotCollection of(final SlotAccess slotAccess) {
      return new SlotCollection() {
         @Override
         public Stream<ItemStack> itemCopies() {
            return Stream.of(slotAccess.get().copy());
         }

         @Override
         public int size() {
            return 1;
         }

         @Override
         public int replaceSlotItems(final ItemProvider items, final SlotSelector slotSelector) {
            if (items.hasNext() && slotSelector.trySelectSlot(slotAccess.get())) {
               return slotAccess.set(items.next()) ? 1 : 0;
            } else {
               return 0;
            }
         }

         @Override
         public void modifySlots(final Consumer<? super SlotAccess> consumer, final SlotSelector slotSelector) {
            if (slotSelector.trySelectSlot(slotAccess.get())) {
               consumer.accept(slotAccess);
            }
         }
      };
   }

   static SlotCollection of(final Collection<? extends SlotAccess> slots) {
      return switch (slots.size()) {
         case 0 -> EMPTY;
         case 1 -> of(slots.iterator().next());
         default -> new SlotCollection() {
            @Override
            public Stream<ItemStack> itemCopies() {
               return slots.stream().map(SlotAccess::get).map(ItemStack::copy);
            }

            @Override
            public int size() {
               return slots.size();
            }

            @Override
            public int replaceSlotItems(final ItemProvider items, final SlotSelector slotSelector) {
               Iterator<? extends SlotAccess> iterator = slots.iterator();
               int successCount = 0;

               while (iterator.hasNext() && items.hasNext()) {
                  SlotAccess slot = iterator.next();
                  if (slotSelector.trySelectSlot(slot.get()) && slot.set(items.next())) {
                     successCount++;
                  }
               }

               return successCount;
            }

            @Override
            public void modifySlots(final Consumer<? super SlotAccess> consumer, final SlotSelector slotSelector) {
               for (SlotAccess slot : slots) {
                  if (slotSelector.trySelectSlot(slot.get())) {
                     consumer.accept(slot);
                  }
               }
            }
         };
      };
   }

   static SlotCollection concat(final SlotCollection first, final SlotCollection second) {
      return new SlotCollection() {
         @Override
         public Stream<ItemStack> itemCopies() {
            return Stream.concat(first.itemCopies(), second.itemCopies());
         }

         @Override
         public int size() {
            return first.size() + second.size();
         }

         @Override
         public int replaceSlotItems(final ItemProvider items, final SlotSelector slotSelector) {
            int firstCount = first.replaceSlotItems(items, slotSelector);
            int secondCount = second.replaceSlotItems(items, slotSelector);
            return firstCount + secondCount;
         }

         @Override
         public void modifySlots(final Consumer<? super SlotAccess> consumer, final SlotSelector slotSelector) {
            first.modifySlots(consumer, slotSelector);
            second.modifySlots(consumer, slotSelector);
         }
      };
   }

   static SlotCollection concat(final List<? extends SlotCollection> terms) {
      return switch (terms.size()) {
         case 0 -> EMPTY;
         case 1 -> (SlotCollection)terms.getFirst();
         case 2 -> concat(terms.get(0), terms.get(1));
         default -> new SlotCollection() {
            @Override
            public Stream<ItemStack> itemCopies() {
               return terms.stream().flatMap(SlotCollection::itemCopies);
            }

            @Override
            public int size() {
               return terms.stream().mapToInt(SlotCollection::size).sum();
            }

            @Override
            public int replaceSlotItems(final ItemProvider items, final SlotSelector slotSelector) {
               int successCount = 0;

               for (SlotCollection slots : terms) {
                  successCount += slots.replaceSlotItems(items, slotSelector);
               }

               return successCount;
            }

            @Override
            public void modifySlots(final Consumer<? super SlotAccess> consumer, final SlotSelector slotSelector) {
               for (SlotCollection slots : terms) {
                  slots.modifySlots(consumer, slotSelector);
               }
            }
         };
      };
   }

   record Filtered(SlotCollection slots, Predicate<? super ItemStack> filter) implements SlotCollection {
      @Override
      public Stream<ItemStack> itemCopies() {
         return this.slots.itemCopies().filter(this.filter);
      }

      @Override
      public int size() {
         return (int)this.itemCopies().count();
      }

      @Override
      public int replaceSlotItems(final ItemProvider items, final SlotSelector slotSelector) {
         return this.slots.replaceSlotItems(items, slotSelector.filter(this.filter));
      }

      @Override
      public void modifySlots(final Consumer<? super SlotAccess> consumer, final SlotSelector slotSelector) {
         this.slots.modifySlots(consumer, slotSelector.filter(this.filter));
      }

      @Override
      public SlotCollection filter(final Predicate<? super ItemStack> predicate) {
         Objects.requireNonNull(predicate);
         return new SlotCollection.Filtered(this.slots, t -> this.filter.test(t) && predicate.test(t));
      }
   }

   record FlatMapped(SlotCollection slots, Function<ItemStack, ? extends SlotCollection> mapper) implements SlotCollection {
      @Override
      public Stream<ItemStack> itemCopies() {
         return this.slots.itemCopies().map(this.mapper).flatMap(SlotCollection::itemCopies);
      }

      @Override
      public int size() {
         return this.slots.itemCopies().map(this.mapper).mapToInt(SlotCollection::size).sum();
      }

      @Override
      public int replaceSlotItems(final ItemProvider items, final SlotSelector slotSelector) {
         MutableInt successCount = new MutableInt();
         this.slots.modifySlots(slot -> {
            SlotCollection mapped = this.mapper.apply(slot.get());
            successCount.add(mapped.replaceSlotItems(items, slotSelector));
         });
         return successCount.intValue();
      }

      @Override
      public void modifySlots(final Consumer<? super SlotAccess> consumer, final SlotSelector slotSelector) {
         this.slots.modifySlots(slot -> {
            SlotCollection mapped = this.mapper.apply(slot.get());
            mapped.modifySlots(consumer, slotSelector);
         });
      }
   }

   record Limited(SlotCollection slots, int limit) implements SlotCollection {
      @Override
      public Stream<ItemStack> itemCopies() {
         return this.slots.itemCopies().limit(this.limit);
      }

      @Override
      public int size() {
         return Math.min(this.slots.size(), this.limit);
      }

      @Override
      public int replaceSlotItems(final ItemProvider items, final SlotSelector slotSelector) {
         return this.slots.replaceSlotItems(items, slotSelector.limit(this.limit));
      }

      @Override
      public void modifySlots(final Consumer<? super SlotAccess> consumer, final SlotSelector slotSelector) {
         this.slots.modifySlots(consumer, slotSelector.limit(this.limit));
      }

      @Override
      public SlotCollection limit(final int limit) {
         return new SlotCollection.Limited(this.slots, Math.min(this.limit, limit));
      }
   }
}
