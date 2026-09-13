package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetAttributesFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetAttributesFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_341999_ -> commonFields(p_341999_)
         .and(
            p_341999_.group(
               SetAttributesFunction.Modifier.CODEC.listOf().fieldOf("modifiers").forGetter(p_297111_ -> p_297111_.modifiers),
               Codec.BOOL.optionalFieldOf("replace", true).forGetter(p_327579_ -> p_327579_.replace)
            )
         )
         .apply(p_341999_, SetAttributesFunction::new)
   );
   private final List<SetAttributesFunction.Modifier> modifiers;
   private final boolean replace;

   SetAttributesFunction(List<LootItemCondition> p_80834_, List<SetAttributesFunction.Modifier> p_298826_, boolean p_336168_) {
      super(p_80834_);
      this.modifiers = List.copyOf(p_298826_);
      this.replace = p_336168_;
   }

   @Override
   public LootItemFunctionType<SetAttributesFunction> getType() {
      return LootItemFunctions.SET_ATTRIBUTES;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return this.modifiers.stream().flatMap(p_450096_ -> p_450096_.amount.getReferencedContextParams().stream()).collect(ImmutableSet.toImmutableSet());
   }

   @Override
   public ItemStack run(ItemStack p_80840_, LootContext p_80841_) {
      if (this.replace) {
         p_80840_.set(DataComponents.ATTRIBUTE_MODIFIERS, this.updateModifiers(p_80841_, ItemAttributeModifiers.EMPTY));
      } else {
         p_80840_.update(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY, p_360673_ -> this.updateModifiers(p_80841_, p_360673_));
      }

      return p_80840_;
   }

   private ItemAttributeModifiers updateModifiers(LootContext p_331587_, ItemAttributeModifiers p_333868_) {
      RandomSource randomsource = p_331587_.getRandom();

      for (SetAttributesFunction.Modifier setattributesfunction$modifier : this.modifiers) {
         EquipmentSlotGroup equipmentslotgroup = Util.getRandom(setattributesfunction$modifier.slots, randomsource);
         p_333868_ = p_333868_.withModifierAdded(
            setattributesfunction$modifier.attribute,
            new AttributeModifier(
               setattributesfunction$modifier.id, setattributesfunction$modifier.amount.getFloat(p_331587_), setattributesfunction$modifier.operation
            ),
            equipmentslotgroup
         );
      }

      return p_333868_;
   }

   public static SetAttributesFunction.ModifierBuilder modifier(
      Identifier p_456809_, Holder<Attribute> p_300622_, AttributeModifier.Operation p_165238_, NumberProvider p_165239_
   ) {
      return new SetAttributesFunction.ModifierBuilder(p_456809_, p_300622_, p_165238_, p_165239_);
   }

   public static SetAttributesFunction.Builder setAttributes() {
      return new SetAttributesFunction.Builder();
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetAttributesFunction.Builder> {
      private final boolean replace;
      private final List<SetAttributesFunction.Modifier> modifiers = Lists.newArrayList();

      public Builder(boolean p_330119_) {
         this.replace = p_330119_;
      }

      public Builder() {
         this(false);
      }

      protected SetAttributesFunction.Builder getThis() {
         return this;
      }

      public SetAttributesFunction.Builder withModifier(SetAttributesFunction.ModifierBuilder p_165246_) {
         this.modifiers.add(p_165246_.build());
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetAttributesFunction(this.getConditions(), this.modifiers, this.replace);
      }
   }

   record Modifier(Identifier id, Holder<Attribute> attribute, AttributeModifier.Operation operation, NumberProvider amount, List<EquipmentSlotGroup> slots) {
      private static final Codec<List<EquipmentSlotGroup>> SLOTS_CODEC = ExtraCodecs.nonEmptyList(ExtraCodecs.compactListCodec(EquipmentSlotGroup.CODEC));
      public static final Codec<SetAttributesFunction.Modifier> CODEC = RecordCodecBuilder.create(
         p_450097_ -> p_450097_.group(
               Identifier.CODEC.fieldOf("id").forGetter(SetAttributesFunction.Modifier::id),
               Attribute.CODEC.fieldOf("attribute").forGetter(SetAttributesFunction.Modifier::attribute),
               AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(SetAttributesFunction.Modifier::operation),
               NumberProviders.CODEC.fieldOf("amount").forGetter(SetAttributesFunction.Modifier::amount),
               SLOTS_CODEC.fieldOf("slot").forGetter(SetAttributesFunction.Modifier::slots)
            )
            .apply(p_450097_, SetAttributesFunction.Modifier::new)
      );
   }

   public static class ModifierBuilder {
      private final Identifier id;
      private final Holder<Attribute> attribute;
      private final AttributeModifier.Operation operation;
      private final NumberProvider amount;
      private final Set<EquipmentSlotGroup> slots = EnumSet.noneOf(EquipmentSlotGroup.class);

      public ModifierBuilder(Identifier p_451982_, Holder<Attribute> p_299576_, AttributeModifier.Operation p_165265_, NumberProvider p_165266_) {
         this.id = p_451982_;
         this.attribute = p_299576_;
         this.operation = p_165265_;
         this.amount = p_165266_;
      }

      public SetAttributesFunction.ModifierBuilder forSlot(EquipmentSlotGroup p_333921_) {
         this.slots.add(p_333921_);
         return this;
      }

      public SetAttributesFunction.Modifier build() {
         return new SetAttributesFunction.Modifier(this.id, this.attribute, this.operation, this.amount, List.copyOf(this.slots));
      }
   }
}
