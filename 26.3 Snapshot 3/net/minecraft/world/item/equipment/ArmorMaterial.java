package net.minecraft.world.item.equipment;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public record ArmorMaterial(
   int durability,
   Map<ArmorType, Integer> defense,
   int enchantmentValue,
   Holder<SoundEvent> equipSound,
   float toughness,
   float knockbackResistance,
   TagKey<Item> repairIngredient,
   ResourceKey<EquipmentAsset> assetId
) {
   public ItemAttributeModifiers createAttributes(final ArmorType type) {
      int defense = this.defense.getOrDefault(type, 0);
      ItemAttributeModifiers.Builder modifiers = ItemAttributeModifiers.builder();
      EquipmentSlotGroup slotGroup = EquipmentSlotGroup.bySlot(type.getSlot());
      Identifier modifierId = Identifier.withDefaultNamespace("armor." + type.getName());
      modifiers.add(Attributes.ARMOR, new AttributeModifier(modifierId, defense, AttributeModifier.Operation.ADD_VALUE), slotGroup);
      modifiers.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(modifierId, this.toughness, AttributeModifier.Operation.ADD_VALUE), slotGroup);
      if (this.knockbackResistance > 0.0F) {
         modifiers.add(
            Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(modifierId, this.knockbackResistance, AttributeModifier.Operation.ADD_VALUE), slotGroup
         );
      }

      return modifiers.build();
   }
}
