package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public interface EntityTypeTags {
   TagKey<EntityType<?>> SKELETONS = create("skeletons");
   TagKey<EntityType<?>> ZOMBIES = create("zombies");
   TagKey<EntityType<?>> RAIDERS = create("raiders");
   TagKey<EntityType<?>> UNDEAD = create("undead");
   TagKey<EntityType<?>> BURN_IN_DAYLIGHT = create("burn_in_daylight");
   TagKey<EntityType<?>> BEEHIVE_INHABITORS = create("beehive_inhabitors");
   TagKey<EntityType<?>> ARROWS = create("arrows");
   TagKey<EntityType<?>> IMPACT_PROJECTILES = create("impact_projectiles");
   TagKey<EntityType<?>> POWDER_SNOW_WALKABLE_MOBS = create("powder_snow_walkable_mobs");
   TagKey<EntityType<?>> AXOLOTL_ALWAYS_HOSTILES = create("axolotl_always_hostiles");
   TagKey<EntityType<?>> AXOLOTL_HUNT_TARGETS = create("axolotl_hunt_targets");
   TagKey<EntityType<?>> FREEZE_IMMUNE_ENTITY_TYPES = create("freeze_immune_entity_types");
   TagKey<EntityType<?>> FREEZE_HURTS_EXTRA_TYPES = create("freeze_hurts_extra_types");
   TagKey<EntityType<?>> CAN_BREATHE_UNDER_WATER = create("can_breathe_under_water");
   TagKey<EntityType<?>> FROG_FOOD = create("frog_food");
   TagKey<EntityType<?>> FALL_DAMAGE_IMMUNE = create("fall_damage_immune");
   TagKey<EntityType<?>> DISMOUNTS_UNDERWATER = create("dismounts_underwater");
   TagKey<EntityType<?>> NON_CONTROLLING_RIDER = create("non_controlling_rider");
   TagKey<EntityType<?>> DEFLECTS_PROJECTILES = create("deflects_projectiles");
   TagKey<EntityType<?>> CAN_TURN_IN_BOATS = create("can_turn_in_boats");
   TagKey<EntityType<?>> ILLAGER = create("illager");
   TagKey<EntityType<?>> AQUATIC = create("aquatic");
   TagKey<EntityType<?>> ARTHROPOD = create("arthropod");
   TagKey<EntityType<?>> IGNORES_POISON_AND_REGEN = create("ignores_poison_and_regen");
   TagKey<EntityType<?>> INVERTED_HEALING_AND_HARM = create("inverted_healing_and_harm");
   TagKey<EntityType<?>> WITHER_FRIENDS = create("wither_friends");
   TagKey<EntityType<?>> ILLAGER_FRIENDS = create("illager_friends");
   TagKey<EntityType<?>> NOT_SCARY_FOR_PUFFERFISH = create("not_scary_for_pufferfish");
   TagKey<EntityType<?>> SENSITIVE_TO_IMPALING = create("sensitive_to_impaling");
   TagKey<EntityType<?>> SENSITIVE_TO_BANE_OF_ARTHROPODS = create("sensitive_to_bane_of_arthropods");
   TagKey<EntityType<?>> SENSITIVE_TO_SMITE = create("sensitive_to_smite");
   TagKey<EntityType<?>> NO_ANGER_FROM_WIND_CHARGE = create("no_anger_from_wind_charge");
   TagKey<EntityType<?>> IMMUNE_TO_OOZING = create("immune_to_oozing");
   TagKey<EntityType<?>> IMMUNE_TO_INFESTED = create("immune_to_infested");
   TagKey<EntityType<?>> REDIRECTABLE_PROJECTILE = create("redirectable_projectile");
   TagKey<EntityType<?>> BOAT = create("boat");
   TagKey<EntityType<?>> CAN_EQUIP_SADDLE = create("can_equip_saddle");
   TagKey<EntityType<?>> CAN_EQUIP_HARNESS = create("can_equip_harness");
   TagKey<EntityType<?>> CAN_WEAR_HORSE_ARMOR = create("can_wear_horse_armor");
   TagKey<EntityType<?>> CAN_WEAR_NAUTILUS_ARMOR = create("can_wear_nautilus_armor");
   TagKey<EntityType<?>> FOLLOWABLE_FRIENDLY_MOBS = create("followable_friendly_mobs");
   TagKey<EntityType<?>> CANNOT_BE_PUSHED_ONTO_BOATS = create("cannot_be_pushed_onto_boats");
   TagKey<EntityType<?>> ACCEPTS_IRON_GOLEM_GIFT = create("accepts_iron_golem_gift");
   TagKey<EntityType<?>> CANDIDATE_FOR_IRON_GOLEM_GIFT = create("candidate_for_iron_golem_gift");
   TagKey<EntityType<?>> NAUTILUS_HOSTILES = create("nautilus_hostiles");
   TagKey<EntityType<?>> CAN_FLOAT_WHILE_RIDDEN = create("can_float_while_ridden");

   private static TagKey<EntityType<?>> create(String p_203849_) {
      return TagKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace(p_203849_));
   }
}
