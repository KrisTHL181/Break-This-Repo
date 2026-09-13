package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class ChangeDimensionTrigger extends SimpleCriterionTrigger<ChangeDimensionTrigger.TriggerInstance> {
   @Override
   public Codec<ChangeDimensionTrigger.TriggerInstance> codec() {
      return ChangeDimensionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_452864_, ResourceKey<Level> p_458837_, ResourceKey<Level> p_456762_) {
      this.trigger(p_452864_, p_457150_ -> p_457150_.matches(p_458837_, p_456762_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ResourceKey<Level>> from, Optional<ResourceKey<Level>> to)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ChangeDimensionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_454135_ -> p_454135_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ChangeDimensionTrigger.TriggerInstance::player),
               ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("from").forGetter(ChangeDimensionTrigger.TriggerInstance::from),
               ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("to").forGetter(ChangeDimensionTrigger.TriggerInstance::to)
            )
            .apply(p_454135_, ChangeDimensionTrigger.TriggerInstance::new)
      );

      public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimension() {
         return CriteriaTriggers.CHANGED_DIMENSION
            .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimension(ResourceKey<Level> p_454493_, ResourceKey<Level> p_454067_) {
         return CriteriaTriggers.CHANGED_DIMENSION
            .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.of(p_454493_), Optional.of(p_454067_)));
      }

      public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimensionTo(ResourceKey<Level> p_455742_) {
         return CriteriaTriggers.CHANGED_DIMENSION
            .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(p_455742_)));
      }

      public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimensionFrom(ResourceKey<Level> p_458066_) {
         return CriteriaTriggers.CHANGED_DIMENSION
            .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.of(p_458066_), Optional.empty()));
      }

      public boolean matches(ResourceKey<Level> p_455530_, ResourceKey<Level> p_452383_) {
         return this.from.isPresent() && this.from.get() != p_455530_ ? false : !this.to.isPresent() || this.to.get() == p_452383_;
      }
   }
}
