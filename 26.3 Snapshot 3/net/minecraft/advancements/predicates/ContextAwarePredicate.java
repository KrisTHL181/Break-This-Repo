package net.minecraft.advancements.predicates;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ContextAwarePredicate implements Validatable {
   public static final Codec<ContextAwarePredicate> CODEC = LootItemCondition.DIRECT_CODEC
      .listOf()
      .xmap(ContextAwarePredicate::new, predicate -> predicate.conditions);
   private final List<LootItemCondition> conditions;
   private final Predicate<LootContext> compositePredicates;

   public ContextAwarePredicate(final List<LootItemCondition> conditions) {
      this.conditions = conditions;
      this.compositePredicates = Util.allOf(conditions);
   }

   public static ContextAwarePredicate create(final LootItemCondition... conditions) {
      return new ContextAwarePredicate(List.of(conditions));
   }

   public boolean matches(final LootContext context) {
      return this.compositePredicates.test(context);
   }

   @Override
   public void validate(final ValidationContext context) {
      Validatable.validate(context, this.conditions);
   }
}
