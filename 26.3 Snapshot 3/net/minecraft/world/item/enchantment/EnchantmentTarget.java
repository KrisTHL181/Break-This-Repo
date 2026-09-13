package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum EnchantmentTarget implements StringRepresentable {
   ATTACKER("attacker"),
   DAMAGING_ENTITY("damaging_entity"),
   VICTIM("victim");

   public static final Codec<EnchantmentTarget> CODEC = StringRepresentable.fromEnum(EnchantmentTarget::values);
   public static final Codec<EnchantmentTarget> NON_DAMAGE_CODEC = StringRepresentable.fromEnum(() -> new EnchantmentTarget[]{ATTACKER, VICTIM});
   private final String id;

   EnchantmentTarget(final String id) {
      this.id = id;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }
}
