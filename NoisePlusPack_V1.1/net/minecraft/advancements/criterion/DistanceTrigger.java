package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class DistanceTrigger extends SimpleCriterionTrigger<DistanceTrigger.TriggerInstance> {
   @Override
   public Codec<DistanceTrigger.TriggerInstance> codec() {
      return DistanceTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_453647_, Vec3 p_455952_) {
      Vec3 vec3 = p_453647_.position();
      this.trigger(p_453647_, p_459897_ -> p_459897_.matches(p_453647_.level(), p_455952_, vec3));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<LocationPredicate> startPosition, Optional<DistancePredicate> distance)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<DistanceTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_458751_ -> p_458751_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(DistanceTrigger.TriggerInstance::player),
               LocationPredicate.CODEC.optionalFieldOf("start_position").forGetter(DistanceTrigger.TriggerInstance::startPosition),
               DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(DistanceTrigger.TriggerInstance::distance)
            )
            .apply(p_458751_, DistanceTrigger.TriggerInstance::new)
      );

      public static Criterion<DistanceTrigger.TriggerInstance> fallFromHeight(
         EntityPredicate.Builder p_460046_, DistancePredicate p_455179_, LocationPredicate.Builder p_456316_
      ) {
         return CriteriaTriggers.FALL_FROM_HEIGHT
            .createCriterion(
               new DistanceTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_460046_)), Optional.of(p_456316_.build()), Optional.of(p_455179_))
            );
      }

      public static Criterion<DistanceTrigger.TriggerInstance> rideEntityInLava(EntityPredicate.Builder p_450504_, DistancePredicate p_459716_) {
         return CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER
            .createCriterion(new DistanceTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_450504_)), Optional.empty(), Optional.of(p_459716_)));
      }

      public static Criterion<DistanceTrigger.TriggerInstance> travelledThroughNether(DistancePredicate p_450667_) {
         return CriteriaTriggers.NETHER_TRAVEL.createCriterion(new DistanceTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(p_450667_)));
      }

      public boolean matches(ServerLevel p_459624_, Vec3 p_456261_, Vec3 p_455171_) {
         return this.startPosition.isPresent() && !this.startPosition.get().matches(p_459624_, p_456261_.x, p_456261_.y, p_456261_.z)
            ? false
            : !this.distance.isPresent() || this.distance.get().matches(p_456261_.x, p_456261_.y, p_456261_.z, p_455171_.x, p_455171_.y, p_455171_.z);
      }
   }
}
