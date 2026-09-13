package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public class LimitSlotSource extends TransformedSlotSource {
   public static final MapCodec<LimitSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_450637_ -> commonFields(p_450637_)
         .and(ExtraCodecs.POSITIVE_INT.fieldOf("limit").forGetter(p_458707_ -> p_458707_.limit))
         .apply(p_450637_, LimitSlotSource::new)
   );
   private final int limit;

   private LimitSlotSource(SlotSource p_455284_, int p_451744_) {
      super(p_455284_);
      this.limit = p_451744_;
   }

   @Override
   public MapCodec<LimitSlotSource> codec() {
      return MAP_CODEC;
   }

   @Override
   protected SlotCollection transform(SlotCollection p_460537_) {
      return p_460537_.limit(this.limit);
   }
}
