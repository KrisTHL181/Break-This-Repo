package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.DistancePredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class DistanceTrigger extends SimpleCriterionTrigger<DistanceTrigger.TriggerInstance> {
   @Override
   public Codec<DistanceTrigger.TriggerInstance> codec() {
      return DistanceTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final Vec3 startPosition) {
      Vec3 playerPosition = player.position();
      this.trigger(player, t -> t.matches(player.level(), startPosition, playerPosition));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<LocationPredicate> startPosition, Optional<DistancePredicate> distance)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<DistanceTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(DistanceTrigger.TriggerInstance::player),
               LocationPredicate.CODEC.optionalFieldOf("start_position").forGetter(DistanceTrigger.TriggerInstance::startPosition),
               DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(DistanceTrigger.TriggerInstance::distance)
            )
            .apply(i, DistanceTrigger.TriggerInstance::new)
      );

      public static Criterion<DistanceTrigger.TriggerInstance> fallFromHeight(
         final EntityPredicate.Builder player, final DistancePredicate distance, final LocationPredicate.Builder startPosition
      ) {
         return CriteriaTriggers.FALL_FROM_HEIGHT
            .createCriterion(
               new DistanceTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(player)), Optional.of(startPosition.build()), Optional.of(distance))
            );
      }

      public static Criterion<DistanceTrigger.TriggerInstance> rideEntityInLava(final EntityPredicate.Builder player, final DistancePredicate distance) {
         return CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER
            .createCriterion(new DistanceTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(player)), Optional.empty(), Optional.of(distance)));
      }

      public static Criterion<DistanceTrigger.TriggerInstance> travelledThroughNether(final DistancePredicate distance) {
         return CriteriaTriggers.NETHER_TRAVEL.createCriterion(new DistanceTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(distance)));
      }

      public boolean matches(final ServerLevel level, final Vec3 enteredPosition, final Vec3 playerPosition) {
         return this.startPosition.isPresent() && !this.startPosition.get().matches(level, enteredPosition.x, enteredPosition.y, enteredPosition.z)
            ? false
            : !this.distance.isPresent()
               || this.distance.get().matches(enteredPosition.x, enteredPosition.y, enteredPosition.z, playerPosition.x, playerPosition.y, playerPosition.z);
      }
   }
}
