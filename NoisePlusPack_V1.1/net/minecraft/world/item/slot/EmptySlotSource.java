package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;

public record EmptySlotSource() implements SlotSource {
   public static final MapCodec<EmptySlotSource> MAP_CODEC = MapCodec.unit(new EmptySlotSource());

   @Override
   public MapCodec<EmptySlotSource> codec() {
      return MAP_CODEC;
   }

   @Override
   public SlotCollection provide(LootContext p_453405_) {
      return SlotCollection.EMPTY;
   }
}
