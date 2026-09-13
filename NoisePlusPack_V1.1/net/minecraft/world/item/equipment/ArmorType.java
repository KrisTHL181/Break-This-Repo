package net.minecraft.world.item.equipment;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;

public enum ArmorType implements StringRepresentable {
   HELMET(EquipmentSlot.HEAD, 11, "helmet"),
   CHESTPLATE(EquipmentSlot.CHEST, 16, "chestplate"),
   LEGGINGS(EquipmentSlot.LEGS, 15, "leggings"),
   BOOTS(EquipmentSlot.FEET, 13, "boots"),
   BODY(EquipmentSlot.BODY, 16, "body");

   public static final Codec<ArmorType> CODEC = StringRepresentable.fromValues(ArmorType::values);
   private final EquipmentSlot slot;
   private final String name;
   private final int unitDurability;

   ArmorType(final EquipmentSlot p_368148_, final int p_367541_, final String p_367745_) {
      this.slot = p_368148_;
      this.name = p_367745_;
      this.unitDurability = p_367541_;
   }

   public int getDurability(int p_369315_) {
      return this.unitDurability * p_369315_;
   }

   public EquipmentSlot getSlot() {
      return this.slot;
   }

   public String getName() {
      return this.name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
