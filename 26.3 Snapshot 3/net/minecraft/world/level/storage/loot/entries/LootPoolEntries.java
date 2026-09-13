package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class LootPoolEntries {
   public static final Codec<LootPoolEntryContainer> CODEC = BuiltInRegistries.LOOT_POOL_ENTRY_TYPE
      .byNameCodec()
      .dispatch(LootPoolEntryContainer::codec, c -> c);

   public static MapCodec<? extends LootPoolEntryContainer> bootstrap(final Registry<MapCodec<? extends LootPoolEntryContainer>> registry) {
      Registry.register(registry, "empty", EmptyLootItem.MAP_CODEC);
      Registry.register(registry, "item", LootItem.MAP_CODEC);
      Registry.register(registry, "loot_table", NestedLootTable.MAP_CODEC);
      Registry.register(registry, "dynamic", DynamicLoot.MAP_CODEC);
      Registry.register(registry, "tag", TagEntry.MAP_CODEC);
      Registry.register(registry, "slots", SlotLoot.MAP_CODEC);
      Registry.register(registry, "alternatives", AlternativesEntry.MAP_CODEC);
      Registry.register(registry, "sequence", SequentialEntry.MAP_CODEC);
      return Registry.register(registry, "group", EntryGroup.MAP_CODEC);
   }
}
