package net.minecraft.world.item.slot;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;

public interface SlotCollection {
    SlotCollection EMPTY = Stream::empty;

    Stream<ItemStack> itemCopies();

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
        return () -> Stream.of(slotAccess.get().copy());
    }

    static SlotCollection of(final Collection<? extends SlotAccess> slots) {
        return switch (slots.size()) {
            case 0 -> EMPTY;
            case 1 -> of(slots.iterator().next());
            default -> () -> slots.stream().map(SlotAccess::get).map(ItemStack::copy);
        };
    }

    static SlotCollection concat(final SlotCollection first, final SlotCollection second) {
        return () -> Stream.concat(first.itemCopies(), second.itemCopies());
    }

    static SlotCollection concat(final List<? extends SlotCollection> terms) {
        return switch (terms.size()) {
            case 0 -> EMPTY;
            case 1 -> (SlotCollection)terms.getFirst();
            case 2 -> concat(terms.get(0), terms.get(1));
            default -> () -> terms.stream().flatMap(SlotCollection::itemCopies);
        };
    }

    record Filtered(SlotCollection slots, Predicate<? super ItemStack> filter) implements SlotCollection {
        @Override
        public Stream<ItemStack> itemCopies() {
            return this.slots.itemCopies().filter(this.filter);
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
    }

    record Limited(SlotCollection slots, int limit) implements SlotCollection {
        @Override
        public Stream<ItemStack> itemCopies() {
            return this.slots.itemCopies().limit(this.limit);
        }

        @Override
        public SlotCollection limit(final int limit) {
            return new SlotCollection.Limited(this.slots, Math.min(this.limit, limit));
        }
    }
}