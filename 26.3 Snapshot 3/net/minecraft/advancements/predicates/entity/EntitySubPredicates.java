package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;

public class EntitySubPredicates {
   public static Codec<? extends EntitySubPredicate> bootstrap(final Registry<Codec<? extends EntitySubPredicate>> registry) {
      Registry.register(registry, "entity_type", EntityTypePredicate.CODEC);
      Registry.register(registry, "location", EntityLocationPredicate.CODEC);
      Registry.register(registry, "stepping_on", SteppingOnPredicate.CODEC);
      Registry.register(registry, "movement_affected_by", MovementAffectedByPredicate.CODEC);
      Registry.register(registry, "distance", DistanceToPlayerPredicate.CODEC);
      Registry.register(registry, "movement", MovementPredicate.CODEC);
      Registry.register(registry, "effects", EntityEffectsPredicate.CODEC);
      Registry.register(registry, "nbt", EntityNbtPredicate.CODEC);
      Registry.register(registry, "flags", EntityFlagsPredicate.CODEC);
      Registry.register(registry, "equipment", EntityEquipmentPredicate.CODEC);
      Registry.register(registry, "periodic_tick", PeriodicEntityTickPredicate.CODEC);
      Registry.register(registry, "vehicle", VehiclePredicate.CODEC);
      Registry.register(registry, "passenger", PassengerPredicate.CODEC);
      Registry.register(registry, "targeted_entity", TargetedEntityPredicate.CODEC);
      Registry.register(registry, "team", TeamPredicate.CODEC);
      Registry.register(registry, "slots", EntitySlotsPredicate.CODEC);
      Registry.register(registry, "components", EntityExactDataComponentsPredicate.CODEC);
      Registry.register(registry, "predicates", EntityPartialComponentsPredicate.CODEC);
      Registry.register(registry, "entity_tags", EntityTagPredicate.CODEC);
      Registry.register(registry, "type_specific/lightning", LightningBoltPredicate.CODEC);
      Registry.register(registry, "type_specific/fishing_hook", FishingHookPredicate.CODEC);
      Registry.register(registry, "type_specific/player", PlayerPredicate.CODEC);
      Registry.register(registry, "type_specific/cube_mob", CubeMobPredicate.CODEC);
      Registry.register(registry, "type_specific/raider", RaiderPredicate.CODEC);
      return Registry.register(registry, "type_specific/sheep", SheepPredicate.CODEC);
   }
}
