package net.minecraft.world.item.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextArg;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class RangeSlotSource implements SlotSource {
   private static final LootContextArg<Object> CONTAINER_ARG = LootContextArg.of(LootContextParams.CONTAINER);
   private static final Codec<LootContextArg<Object>> SOURCE_CODEC = LootContextArg.createArgCodec(
      builder -> builder.anyOf(LootContext.EntityTarget.values()).anyOf(LootContext.BlockEntityTarget.values()).or("container", CONTAINER_ARG)
   );
   public static final MapCodec<RangeSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            SOURCE_CODEC.optionalFieldOf("source", CONTAINER_ARG).forGetter(t -> t.source), SlotRanges.CODEC.fieldOf("slots").forGetter(t -> t.slotRange)
         )
         .apply(i, RangeSlotSource::new)
   );
   private final LootContextArg<Object> source;
   private final SlotRange slotRange;

   private RangeSlotSource(final LootContextArg<Object> source, final SlotRange slotRange) {
      this.source = source;
      this.slotRange = slotRange;
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
   public final SlotCollection provide(final LootContext context) {
      return this.source.get(context) instanceof SlotProvider slotProvider ? slotProvider.getSlotsFromRange(this.slotRange.slots()) : SlotCollection.EMPTY;
   }

   public static RangeSlotSource slotRange(final SlotRange slotRange) {
      return new RangeSlotSource(CONTAINER_ARG, slotRange);
   }
}
