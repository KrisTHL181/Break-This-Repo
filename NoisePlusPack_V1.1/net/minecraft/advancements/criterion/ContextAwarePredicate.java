package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ContextAwarePredicate {
   public static final Codec<ContextAwarePredicate> CODEC = LootItemCondition.DIRECT_CODEC
      .listOf()
      .xmap(ContextAwarePredicate::new, p_455730_ -> p_455730_.conditions);
   private final List<LootItemCondition> conditions;
   private final Predicate<LootContext> compositePredicates;

   ContextAwarePredicate(List<LootItemCondition> p_452432_) {
      this.conditions = p_452432_;
      this.compositePredicates = Util.allOf(p_452432_);
   }

   public static ContextAwarePredicate create(LootItemCondition... p_458317_) {
      return new ContextAwarePredicate(List.of(p_458317_));
   }

   public boolean matches(LootContext p_452351_) {
      return this.compositePredicates.test(p_452351_);
   }

   public void validate(ValidationContext p_456169_) {
      for (int i = 0; i < this.conditions.size(); i++) {
         LootItemCondition lootitemcondition = this.conditions.get(i);
         lootitemcondition.validate(p_456169_.forChild(new ProblemReporter.IndexedPathElement(i)));
      }
   }
}
