package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record ConditionalValue(LootItemCondition condition, NumberProvider onTrue, NumberProvider onFalse) implements NumberProvider {
   public static final MapCodec<ConditionalValue> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            LootItemCondition.DIRECT_CODEC.fieldOf("condition").forGetter(c -> c.condition),
            NumberProviders.DIRECT_CODEC.fieldOf("on_true").forGetter(c -> c.onTrue),
            NumberProviders.DIRECT_CODEC.optionalFieldOf("on_false", ConstantValue.exactly(0.0F)).forGetter(c -> c.onFalse)
         )
         .apply(i, ConditionalValue::new)
   );
   public static final Codec<ConditionalValue> CODEC = MAP_CODEC.codec();

   @Override
   public int getInt(final LootContext context) {
      return this.selectValue(context).getInt(context);
   }

   @Override
   public float getFloat(final LootContext context) {
      return this.selectValue(context).getFloat(context);
   }

   private NumberProvider selectValue(final LootContext context) {
      return !this.condition.test(context) ? this.onFalse : this.onTrue;
   }

   @Override
   public MapCodec<ConditionalValue> codec() {
      return MAP_CODEC;
   }

   @Override
   public void validate(final ValidationContext context) {
      NumberProvider.super.validate(context);
      Validatable.validate(context, "condition", this.condition);
      Validatable.validate(context, "on_true", this.onTrue);
      Validatable.validate(context, "on_false", this.onFalse);
   }
}
