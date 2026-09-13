package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ItemPredicate;

public class FilteredSlotSource extends TransformedSlotSource {
   public static final MapCodec<FilteredSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_457600_ -> commonFields(p_457600_)
         .and(ItemPredicate.CODEC.fieldOf("item_filter").forGetter(p_458686_ -> p_458686_.filter))
         .apply(p_457600_, FilteredSlotSource::new)
   );
   private final ItemPredicate filter;

   private FilteredSlotSource(SlotSource p_459049_, ItemPredicate p_458580_) {
      super(p_459049_);
      this.filter = p_458580_;
   }

   @Override
   public MapCodec<FilteredSlotSource> codec() {
      return MAP_CODEC;
   }

   @Override
   protected SlotCollection transform(SlotCollection p_459642_) {
      return p_459642_.filter(this.filter);
   }
}
