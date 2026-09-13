package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class LootPoolEntryContainer implements ComposableEntryContainer {
   protected final List<LootItemCondition> conditions;
   private final Predicate<LootContext> compositeCondition;

   protected LootPoolEntryContainer(List<LootItemCondition> p_298327_) {
      this.conditions = p_298327_;
      this.compositeCondition = Util.allOf(p_298327_);
   }

   protected static <T extends LootPoolEntryContainer> P1<Mu<T>, List<LootItemCondition>> commonFields(Instance<T> p_297717_) {
      return p_297717_.group(LootItemCondition.DIRECT_CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(p_297410_ -> p_297410_.conditions));
   }

   public void validate(ValidationContext p_79641_) {
      for (int i = 0; i < this.conditions.size(); i++) {
         this.conditions.get(i).validate(p_79641_.forChild(new ProblemReporter.IndexedFieldPathElement("conditions", i)));
      }
   }

   protected final boolean canRun(LootContext p_79640_) {
      return this.compositeCondition.test(p_79640_);
   }

   public abstract LootPoolEntryType getType();

   public abstract static class Builder<T extends LootPoolEntryContainer.Builder<T>> implements ConditionUserBuilder<T> {
      private final com.google.common.collect.ImmutableList.Builder<LootItemCondition> conditions = ImmutableList.builder();

      protected abstract T getThis();

      public T when(LootItemCondition.Builder p_79646_) {
         this.conditions.add(p_79646_.build());
         return this.getThis();
      }

      public final T unwrap() {
         return this.getThis();
      }

      protected List<LootItemCondition> getConditions() {
         return this.conditions.build();
      }

      public AlternativesEntry.Builder otherwise(LootPoolEntryContainer.Builder<?> p_79644_) {
         return new AlternativesEntry.Builder(this, p_79644_);
      }

      public EntryGroup.Builder append(LootPoolEntryContainer.Builder<?> p_165148_) {
         return new EntryGroup.Builder(this, p_165148_);
      }

      public SequentialEntry.Builder then(LootPoolEntryContainer.Builder<?> p_165149_) {
         return new SequentialEntry.Builder(this, p_165149_);
      }

      public abstract LootPoolEntryContainer build();
   }
}
