package net.minecraft.world.item.slot;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;

public interface SlotCollection {
   SlotCollection EMPTY = Stream::empty;

   Stream<ItemStack> itemCopies();

   default SlotCollection filter(Predicate<ItemStack> p_451415_) {
      return new SlotCollection.Filtered(this, p_451415_);
   }

   default SlotCollection flatMap(Function<ItemStack, ? extends SlotCollection> p_451352_) {
      return new SlotCollection.FlatMapped(this, p_451352_);
   }

   default SlotCollection limit(int p_460595_) {
      return new SlotCollection.Limited(this, p_460595_);
   }

   static SlotCollection of(SlotAccess p_454366_) {
      return () -> Stream.of(p_454366_.get().copy());
   }

   static SlotCollection of(Collection<? extends SlotAccess> p_459804_) {
      return switch (p_459804_.size()) {
         case 0 -> EMPTY;
         case 1 -> of(p_459804_.iterator().next());
         default -> () -> p_459804_.stream().map(SlotAccess::get).map(ItemStack::copy);
      };
   }

   static SlotCollection concat(SlotCollection p_458556_, SlotCollection p_451355_) {
      return () -> Stream.concat(p_458556_.itemCopies(), p_451355_.itemCopies());
   }

   static SlotCollection concat(List<? extends SlotCollection> p_455140_) {
      return switch (p_455140_.size()) {
         case 0 -> EMPTY;
         case 1 -> (SlotCollection)p_455140_.getFirst();
         case 2 -> concat(p_455140_.get(0), p_455140_.get(1));
         default -> () -> p_455140_.stream().flatMap(SlotCollection::itemCopies);
      };
   }

   record Filtered(SlotCollection slots, Predicate<ItemStack> filter) implements SlotCollection {
      @Override
      public Stream<ItemStack> itemCopies() {
         return this.slots.itemCopies().filter(this.filter);
      }

      @Override
      public SlotCollection filter(Predicate<ItemStack> p_456944_) {
         return new SlotCollection.Filtered(this.slots, this.filter.and(p_456944_));
      }
   }

   record FlatMapped(SlotCollection slots, Function<ItemStack, ? extends SlotCollection> mapper) implements SlotCollection {
      @Override
      public Stream<ItemStack> itemCopies() {
         return this.slots.itemCopies().map(this.mapper).flatMap(SlotCollection::itemCopies);
      }
   }

   record Limited(SlotCollection slots, int limit) implements SlotCollection {
      @Override
      public Stream<ItemStack> itemCopies() {
         return this.slots.itemCopies().limit(this.limit);
      }

      @Override
      public SlotCollection limit(int p_456606_) {
         return new SlotCollection.Limited(this.slots, Math.min(this.limit, p_456606_));
      }
   }
}
