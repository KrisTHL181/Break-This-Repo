package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ItemPredicate;

public class FilteredSlotSource extends TransformedSlotSource {
   public static final MapCodec<FilteredSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i).and(ItemPredicate.CODEC.fieldOf("item_filter").forGetter(t -> t.filter)).apply(i, FilteredSlotSource::new)
   );
   private final ItemPredicate filter;

   private FilteredSlotSource(final SlotSource slotSource, final ItemPredicate filter) {
      super(slotSource);
      this.filter = filter;
   }

   @Override
   public MapCodec<FilteredSlotSource> codec() {
      return MAP_CODEC;
   }

   @Override
   protected SlotCollection transform(final SlotCollection slots) {
      return slots.filter(this.filter);
   }
}
