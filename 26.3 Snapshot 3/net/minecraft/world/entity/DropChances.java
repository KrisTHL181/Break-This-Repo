package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;

public record DropChances(Map<EquipmentSlot, Float> byEquipment) {
   public static final float DEFAULT_EQUIPMENT_DROP_CHANCE = 0.085F;
   public static final float PRESERVE_ITEM_DROP_CHANCE_THRESHOLD = 1.0F;
   public static final int PRESERVE_ITEM_DROP_CHANCE = 2;
   public static final DropChances DEFAULT = new DropChances(Util.makeEnumMap(EquipmentSlot.class, slot -> 0.085F));
   public static final Codec<DropChances> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ExtraCodecs.NON_NEGATIVE_FLOAT)
      .xmap(DropChances::toEnumMap, DropChances::filterDefaultValues)
      .xmap(DropChances::new, DropChances::byEquipment);

   private static Map<EquipmentSlot, Float> filterDefaultValues(final Map<EquipmentSlot, Float> map) {
      Map<EquipmentSlot, Float> filteredMap = new HashMap<>(map);
      filteredMap.values().removeIf(chance -> chance == 0.085F);
      return filteredMap;
   }

   private static Map<EquipmentSlot, Float> toEnumMap(final Map<EquipmentSlot, Float> map) {
      return Util.makeEnumMap(EquipmentSlot.class, slot -> map.getOrDefault(slot, 0.085F));
   }

   public DropChances withGuaranteedDrop(final EquipmentSlot slot) {
      return this.withEquipmentChance(slot, 2.0F);
   }

   public DropChances withEquipmentChance(final EquipmentSlot slot, final float chance) {
      if (chance < 0.0F) {
         throw new IllegalArgumentException("Tried to set invalid equipment chance " + chance + " for " + slot);
      } else {
         return this.byEquipment(slot) == chance
            ? this
            : new DropChances(Util.makeEnumMap(EquipmentSlot.class, newSlot -> newSlot == slot ? chance : this.byEquipment(newSlot)));
      }
   }

   public float byEquipment(final EquipmentSlot slot) {
      return this.byEquipment.getOrDefault(slot, 0.085F);
   }

   public boolean isPreserved(final EquipmentSlot slot) {
      return this.byEquipment(slot) > 1.0F;
   }
}
