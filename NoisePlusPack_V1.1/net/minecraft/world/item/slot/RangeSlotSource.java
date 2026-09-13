package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextArg;

public class RangeSlotSource implements SlotSource {
   public static final MapCodec<RangeSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_450408_ -> p_450408_.group(
            LootContextArg.ENTITY_OR_BLOCK.fieldOf("source").forGetter(p_453446_ -> p_453446_.source),
            SlotRanges.CODEC.fieldOf("slots").forGetter(p_453789_ -> p_453789_.slotRange)
         )
         .apply(p_450408_, RangeSlotSource::new)
   );
   private final LootContextArg<Object> source;
   private final SlotRange slotRange;

   private RangeSlotSource(LootContextArg<Object> p_451543_, SlotRange p_452399_) {
      this.source = p_451543_;
      this.slotRange = p_452399_;
   }

   @Override
   public MapCodec<RangeSlotSource> codec() {
      return MAP_CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(this.source.contextParam());
   }

   @Override
   public final SlotCollection provide(LootContext p_453630_) {
      return this.source.get(p_453630_) instanceof SlotProvider slotprovider ? slotprovider.getSlotsFromRange(this.slotRange.slots()) : SlotCollection.EMPTY;
   }
}
