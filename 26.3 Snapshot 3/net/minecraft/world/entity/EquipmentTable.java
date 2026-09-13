package net.minecraft.world.entity;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public record EquipmentTable(ResourceKey<LootTable> lootTable, Map<EquipmentSlot, Float> slotDropChances) {
   public static final Codec<Map<EquipmentSlot, Float>> DROP_CHANCES_CODEC = Codec.either(Codec.FLOAT, Codec.unboundedMap(EquipmentSlot.CODEC, Codec.FLOAT))
      .xmap(either -> (Map)either.map(EquipmentTable::createForAllSlots, Function.identity()), provider -> {
         boolean dropChancesTheSame = provider.values().stream().distinct().count() == 1L;
         boolean allSlotsArePresent = provider.keySet().containsAll(EquipmentSlot.VALUES);
         return dropChancesTheSame && allSlotsArePresent ? Either.left(provider.values().stream().findFirst().orElse(0.0F)) : Either.right(provider);
      });
   public static final Codec<EquipmentTable> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            LootTable.KEY_CODEC.fieldOf("loot_table").forGetter(EquipmentTable::lootTable),
            DROP_CHANCES_CODEC.optionalFieldOf("slot_drop_chances", Map.of()).forGetter(EquipmentTable::slotDropChances)
         )
         .apply(i, EquipmentTable::new)
   );

   public EquipmentTable(final ResourceKey<LootTable> lootTable, final float dropChance) {
      this(lootTable, createForAllSlots(dropChance));
   }

   private static Map<EquipmentSlot, Float> createForAllSlots(final float dropChance) {
      return createForAllSlots(List.of(EquipmentSlot.values()), dropChance);
   }

   private static Map<EquipmentSlot, Float> createForAllSlots(final List<EquipmentSlot> slots, final float dropChance) {
      Map<EquipmentSlot, Float> values = Maps.newHashMap();

      for (EquipmentSlot slot : slots) {
         values.put(slot, dropChance);
      }

      return values;
   }
}
