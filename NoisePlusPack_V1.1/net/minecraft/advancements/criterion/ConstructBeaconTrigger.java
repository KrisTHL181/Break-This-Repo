package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;

public class ConstructBeaconTrigger extends SimpleCriterionTrigger<ConstructBeaconTrigger.TriggerInstance> {
   @Override
   public Codec<ConstructBeaconTrigger.TriggerInstance> codec() {
      return ConstructBeaconTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_456362_, int p_452117_) {
      this.trigger(p_456362_, p_456328_ -> p_456328_.matches(p_452117_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints level) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ConstructBeaconTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_459225_ -> p_459225_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ConstructBeaconTrigger.TriggerInstance::player),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("level", MinMaxBounds.Ints.ANY).forGetter(ConstructBeaconTrigger.TriggerInstance::level)
            )
            .apply(p_459225_, ConstructBeaconTrigger.TriggerInstance::new)
      );

      public static Criterion<ConstructBeaconTrigger.TriggerInstance> constructedBeacon() {
         return CriteriaTriggers.CONSTRUCT_BEACON.createCriterion(new ConstructBeaconTrigger.TriggerInstance(Optional.empty(), MinMaxBounds.Ints.ANY));
      }

      public static Criterion<ConstructBeaconTrigger.TriggerInstance> constructedBeacon(MinMaxBounds.Ints p_457935_) {
         return CriteriaTriggers.CONSTRUCT_BEACON.createCriterion(new ConstructBeaconTrigger.TriggerInstance(Optional.empty(), p_457935_));
      }

      public boolean matches(int p_451880_) {
         return this.level.matches(p_451880_);
      }
   }
}
