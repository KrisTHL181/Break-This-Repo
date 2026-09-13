package net.minecraft.world.item.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.storage.loot.LootContext;

public interface SlotSources {
   Codec<SlotSource> DIRECT_CODEC = Codec.lazyInitialized(
      () -> Codec.withAlternative(BuiltInRegistries.SLOT_SOURCE_TYPE.byNameCodec().dispatch(SlotSource::codec, c -> c), GroupSlotSource.INLINE_CODEC)
   );
   Codec<Holder<SlotSource>> CODEC = RegistryFileCodec.create(Registries.SLOT_SOURCE, DIRECT_CODEC);

   static MapCodec<? extends SlotSource> bootstrap(final Registry<MapCodec<? extends SlotSource>> registry) {
      Registry.register(registry, "group", GroupSlotSource.MAP_CODEC);
      Registry.register(registry, "filtered", FilteredSlotSource.MAP_CODEC);
      Registry.register(registry, "limit_slots", LimitSlotSource.MAP_CODEC);
      Registry.register(registry, "slot_range", RangeSlotSource.MAP_CODEC);
      Registry.register(registry, "contents", ContentsSlotSource.MAP_CODEC);
      Registry.register(registry, "reference", SlotSourceReference.MAP_CODEC);
      return Registry.register(registry, "empty", EmptySlotSource.MAP_CODEC);
   }

   static Function<LootContext, SlotCollection> group(final Collection<? extends SlotSource> list) {
      List<SlotSource> terms = List.copyOf(list);

      return switch (terms.size()) {
         case 0 -> context -> SlotCollection.EMPTY;
         case 1 -> terms.getFirst()::provide;
         case 2 -> {
            SlotSource first = terms.get(0);
            SlotSource second = terms.get(1);
            yield context -> SlotCollection.concat(first.provide(context), second.provide(context));
         }
         default -> context -> {
            List<SlotCollection> collections = new ArrayList<>();

            for (SlotSource term : terms) {
               collections.add(term.provide(context));
            }

            return SlotCollection.concat(collections);
         };
      };
   }
}
