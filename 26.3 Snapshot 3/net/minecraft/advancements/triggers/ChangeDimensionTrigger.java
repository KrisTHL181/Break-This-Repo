package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class ChangeDimensionTrigger extends SimpleCriterionTrigger<ChangeDimensionTrigger.TriggerInstance> {
   @Override
   public Codec<ChangeDimensionTrigger.TriggerInstance> codec() {
      return ChangeDimensionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final ResourceKey<Level> from, final ResourceKey<Level> to) {
      this.trigger(player, t -> t.matches(from, to));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ResourceKey<Level>> from, Optional<ResourceKey<Level>> to)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ChangeDimensionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ChangeDimensionTrigger.TriggerInstance::player),
               ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("from").forGetter(ChangeDimensionTrigger.TriggerInstance::from),
               ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("to").forGetter(ChangeDimensionTrigger.TriggerInstance::to)
            )
            .apply(i, ChangeDimensionTrigger.TriggerInstance::new)
      );

      public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimension() {
         return CriteriaTriggers.CHANGED_DIMENSION
            .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimension(final ResourceKey<Level> from, final ResourceKey<Level> to) {
         return CriteriaTriggers.CHANGED_DIMENSION
            .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.of(from), Optional.of(to)));
      }

      public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimensionTo(final ResourceKey<Level> to) {
         return CriteriaTriggers.CHANGED_DIMENSION
            .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(to)));
      }

      public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimensionFrom(final ResourceKey<Level> from) {
         return CriteriaTriggers.CHANGED_DIMENSION
            .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.of(from), Optional.empty()));
      }

      public boolean matches(final ResourceKey<Level> from, final ResourceKey<Level> to) {
         return this.from.isPresent() && this.from.get() != from ? false : !this.to.isPresent() || this.to.get() == to;
      }
   }
}
