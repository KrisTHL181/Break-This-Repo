package net.minecraft.world.item.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.LootContext;

public interface SlotSources {
   Codec<SlotSource> TYPED_CODEC = BuiltInRegistries.SLOT_SOURCE_TYPE.byNameCodec().dispatch(SlotSource::codec, p_454987_ -> p_454987_);
   Codec<SlotSource> CODEC = Codec.lazyInitialized(() -> Codec.withAlternative(TYPED_CODEC, GroupSlotSource.INLINE_CODEC));

   static MapCodec<? extends SlotSource> bootstrap(Registry<MapCodec<? extends SlotSource>> p_451547_) {
      Registry.register(p_451547_, "group", GroupSlotSource.MAP_CODEC);
      Registry.register(p_451547_, "filtered", FilteredSlotSource.MAP_CODEC);
      Registry.register(p_451547_, "limit_slots", LimitSlotSource.MAP_CODEC);
      Registry.register(p_451547_, "slot_range", RangeSlotSource.MAP_CODEC);
      Registry.register(p_451547_, "contents", ContentsSlotSource.MAP_CODEC);
      return Registry.register(p_451547_, "empty", EmptySlotSource.MAP_CODEC);
   }

   static Function<LootContext, SlotCollection> group(Collection<? extends SlotSource> p_451898_) {
      List<SlotSource> list = List.copyOf(p_451898_);

      return switch (list.size()) {
         case 0 -> p_453745_ -> SlotCollection.EMPTY;
         case 1 -> list.getFirst()::provide;
         case 2 -> {
            SlotSource slotsource = list.get(0);
            SlotSource slotsource1 = list.get(1);
            yield p_455664_ -> SlotCollection.concat(slotsource.provide(p_455664_), slotsource1.provide(p_455664_));
         }
         default -> p_453043_ -> {
            List<SlotCollection> list1 = new ArrayList<>();

            for (SlotSource slotsource2 : list) {
               list1.add(slotsource2.provide(p_453043_));
            }

            return SlotCollection.concat(list1);
         };
      };
   }
}
