package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;

public record InvertedLootItemCondition(LootItemCondition term) implements LootItemCondition {
   public static final MapCodec<InvertedLootItemCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(LootItemCondition.DIRECT_CODEC.fieldOf("term").forGetter(InvertedLootItemCondition::term)).apply(i, InvertedLootItemCondition::new)
   );

   @Override
   public MapCodec<InvertedLootItemCondition> codec() {
      return MAP_CODEC;
   }

   public boolean test(final LootContext context) {
      return !this.term.test(context);
   }

   @Override
   public void validate(final ValidationContext output) {
      LootItemCondition.super.validate(output);
      Validatable.validate(output, "term", this.term);
   }

   public static LootItemCondition.Builder invert(final LootItemCondition.Builder term) {
      InvertedLootItemCondition result = new InvertedLootItemCondition(term.build());
      return () -> result;
   }
}
