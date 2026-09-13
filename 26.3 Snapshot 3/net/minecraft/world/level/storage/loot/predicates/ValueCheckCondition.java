package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public record ValueCheckCondition(NumberProvider value, IntRange range) implements LootItemCondition {
   public static final MapCodec<ValueCheckCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            NumberProviders.DIRECT_CODEC.fieldOf("value").forGetter(ValueCheckCondition::value),
            IntRange.CODEC.fieldOf("range").forGetter(ValueCheckCondition::range)
         )
         .apply(i, ValueCheckCondition::new)
   );

   @Override
   public MapCodec<ValueCheckCondition> codec() {
      return MAP_CODEC;
   }

   @Override
   public void validate(final ValidationContext context) {
      LootItemCondition.super.validate(context);
      Validatable.validate(context, "value", this.value);
      Validatable.validate(context, "range", this.range);
   }

   public boolean test(final LootContext context) {
      return this.range.test(context, this.value.getInt(context));
   }

   public static LootItemCondition.Builder hasValue(final NumberProvider value, final IntRange range) {
      return () -> new ValueCheckCondition(value, range);
   }
}
