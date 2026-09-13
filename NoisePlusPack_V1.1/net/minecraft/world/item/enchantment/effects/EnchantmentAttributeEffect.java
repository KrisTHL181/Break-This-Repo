package net.minecraft.world.item.enchantment.effects;

import com.google.common.collect.HashMultimap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record EnchantmentAttributeEffect(Identifier id, Holder<Attribute> attribute, LevelBasedValue amount, AttributeModifier.Operation operation)
   implements EnchantmentLocationBasedEffect {
   public static final MapCodec<EnchantmentAttributeEffect> CODEC = RecordCodecBuilder.mapCodec(
      p_449868_ -> p_449868_.group(
            Identifier.CODEC.fieldOf("id").forGetter(EnchantmentAttributeEffect::id),
            Attribute.CODEC.fieldOf("attribute").forGetter(EnchantmentAttributeEffect::attribute),
            LevelBasedValue.CODEC.fieldOf("amount").forGetter(EnchantmentAttributeEffect::amount),
            AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(EnchantmentAttributeEffect::operation)
         )
         .apply(p_449868_, EnchantmentAttributeEffect::new)
   );

   private Identifier idForSlot(StringRepresentable p_345417_) {
      return this.id.withSuffix("/" + p_345417_.getSerializedName());
   }

   public AttributeModifier getModifier(int p_342709_, StringRepresentable p_342150_) {
      return new AttributeModifier(this.idForSlot(p_342150_), this.amount().calculate(p_342709_), this.operation());
   }

   @Override
   public void onChangedBlock(ServerLevel p_342233_, int p_343426_, EnchantedItemInUse p_344251_, Entity p_342367_, Vec3 p_343372_, boolean p_342530_) {
      if (p_342530_ && p_342367_ instanceof LivingEntity livingentity) {
         livingentity.getAttributes().addTransientAttributeModifiers(this.makeAttributeMap(p_343426_, p_344251_.inSlot()));
      }
   }

   @Override
   public void onDeactivated(EnchantedItemInUse p_343672_, Entity p_343519_, Vec3 p_342547_, int p_343187_) {
      if (p_343519_ instanceof LivingEntity livingentity) {
         livingentity.getAttributes().removeAttributeModifiers(this.makeAttributeMap(p_343187_, p_343672_.inSlot()));
      }
   }

   private HashMultimap<Holder<Attribute>, AttributeModifier> makeAttributeMap(int p_342373_, EquipmentSlot p_343561_) {
      HashMultimap<Holder<Attribute>, AttributeModifier> hashmultimap = HashMultimap.create();
      hashmultimap.put(this.attribute, this.getModifier(p_342373_, p_343561_));
      return hashmultimap;
   }

   @Override
   public MapCodec<EnchantmentAttributeEffect> codec() {
      return CODEC;
   }
}
