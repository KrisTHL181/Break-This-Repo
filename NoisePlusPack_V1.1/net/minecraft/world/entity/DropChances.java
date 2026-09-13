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
   public static final DropChances DEFAULT = new DropChances(Util.makeEnumMap(EquipmentSlot.class, p_392495_ -> 0.085F));
   public static final Codec<DropChances> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ExtraCodecs.NON_NEGATIVE_FLOAT)
      .xmap(DropChances::toEnumMap, DropChances::filterDefaultValues)
      .xmap(DropChances::new, DropChances::byEquipment);

   private static Map<EquipmentSlot, Float> filterDefaultValues(Map<EquipmentSlot, Float> p_395286_) {
      Map<EquipmentSlot, Float> map = new HashMap<>(p_395286_);
      map.values().removeIf(p_391217_ -> p_391217_ == 0.085F);
      return map;
   }

   private static Map<EquipmentSlot, Float> toEnumMap(Map<EquipmentSlot, Float> p_393581_) {
      return Util.makeEnumMap(EquipmentSlot.class, p_391496_ -> p_393581_.getOrDefault(p_391496_, 0.085F));
   }

   public DropChances withGuaranteedDrop(EquipmentSlot p_395095_) {
      return this.withEquipmentChance(p_395095_, 2.0F);
   }

   public DropChances withEquipmentChance(EquipmentSlot p_391265_, float p_393080_) {
      if (p_393080_ < 0.0F) {
         throw new IllegalArgumentException("Tried to set invalid equipment chance " + p_393080_ + " for " + p_391265_);
      } else {
         return this.byEquipment(p_391265_) == p_393080_
            ? this
            : new DropChances(Util.makeEnumMap(EquipmentSlot.class, p_392887_ -> p_392887_ == p_391265_ ? p_393080_ : this.byEquipment(p_392887_)));
      }
   }

   public float byEquipment(EquipmentSlot p_397982_) {
      return this.byEquipment.getOrDefault(p_397982_, 0.085F);
   }

   public boolean isPreserved(EquipmentSlot p_394795_) {
      return this.byEquipment(p_394795_) > 1.0F;
   }
}
