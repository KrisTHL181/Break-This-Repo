package net.minecraft.world.level.levelgen.structure;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public interface BuiltinStructures {
   ResourceKey<Structure> PILLAGER_OUTPOST = createKey("pillager_outpost");
   ResourceKey<Structure> MINESHAFT = createKey("mineshaft");
   ResourceKey<Structure> MINESHAFT_MESA = createKey("mineshaft_mesa");
   ResourceKey<Structure> WOODLAND_MANSION = createKey("mansion");
   ResourceKey<Structure> JUNGLE_TEMPLE = createKey("jungle_pyramid");
   ResourceKey<Structure> DESERT_PYRAMID = createKey("desert_pyramid");
   ResourceKey<Structure> IGLOO = createKey("igloo");
   ResourceKey<Structure> SHIPWRECK = createKey("shipwreck");
   ResourceKey<Structure> SHIPWRECK_BEACHED = createKey("shipwreck_beached");
   ResourceKey<Structure> SWAMP_HUT = createKey("swamp_hut");
   ResourceKey<Structure> STRONGHOLD = createKey("stronghold");
   ResourceKey<Structure> OCEAN_MONUMENT = createKey("monument");
   ResourceKey<Structure> OCEAN_RUIN_COLD = createKey("ocean_ruin_cold");
   ResourceKey<Structure> OCEAN_RUIN_WARM = createKey("ocean_ruin_warm");
   ResourceKey<Structure> FORTRESS = createKey("fortress");
   ResourceKey<Structure> NETHER_FOSSIL = createKey("nether_fossil");
   ResourceKey<Structure> END_CITY = createKey("end_city");
   ResourceKey<Structure> BURIED_TREASURE = createKey("buried_treasure");
   ResourceKey<Structure> BASTION_REMNANT = createKey("bastion_remnant");
   ResourceKey<Structure> VILLAGE_PLAINS = createKey("village_plains");
   ResourceKey<Structure> VILLAGE_DESERT = createKey("village_desert");
   ResourceKey<Structure> VILLAGE_SAVANNA = createKey("village_savanna");
   ResourceKey<Structure> VILLAGE_SNOWY = createKey("village_snowy");
   ResourceKey<Structure> VILLAGE_TAIGA = createKey("village_taiga");
   ResourceKey<Structure> RUINED_PORTAL_STANDARD = createKey("ruined_portal");
   ResourceKey<Structure> RUINED_PORTAL_DESERT = createKey("ruined_portal_desert");
   ResourceKey<Structure> RUINED_PORTAL_JUNGLE = createKey("ruined_portal_jungle");
   ResourceKey<Structure> RUINED_PORTAL_SWAMP = createKey("ruined_portal_swamp");
   ResourceKey<Structure> RUINED_PORTAL_MOUNTAIN = createKey("ruined_portal_mountain");
   ResourceKey<Structure> RUINED_PORTAL_OCEAN = createKey("ruined_portal_ocean");
   ResourceKey<Structure> RUINED_PORTAL_NETHER = createKey("ruined_portal_nether");
   ResourceKey<Structure> ANCIENT_CITY = createKey("ancient_city");
   ResourceKey<Structure> TRAIL_RUINS = createKey("trail_ruins");
   ResourceKey<Structure> TRIAL_CHAMBERS = createKey("trial_chambers");
   ResourceKey<Structure> ABANDONED_CAMP_BAMBOO_JUNGLE = createKey("abandoned_camp_bamboo_jungle");
   ResourceKey<Structure> ABANDONED_CAMP_BIRCH_FOREST = createKey("abandoned_camp_birch_forest");
   ResourceKey<Structure> ABANDONED_CAMP_CHERRY_GROVE = createKey("abandoned_camp_cherry_grove");
   ResourceKey<Structure> ABANDONED_CAMP_DAPPLED_FOREST = createKey("abandoned_camp_dappled_forest");
   ResourceKey<Structure> ABANDONED_CAMP_FLOWER_FOREST = createKey("abandoned_camp_flower_forest");
   ResourceKey<Structure> ABANDONED_CAMP_FOREST = createKey("abandoned_camp_forest");
   ResourceKey<Structure> ABANDONED_CAMP_MEADOW = createKey("abandoned_camp_meadow");
   ResourceKey<Structure> ABANDONED_CAMP_OLD_GROWTH_BIRCH_FOREST = createKey("abandoned_camp_old_growth_birch_forest");
   ResourceKey<Structure> ABANDONED_CAMP_OLD_GROWTH_PINE_TAIGA = createKey("abandoned_camp_old_growth_pine_taiga");
   ResourceKey<Structure> ABANDONED_CAMP_OLD_GROWTH_SPRUCE_TAIGA = createKey("abandoned_camp_old_growth_spruce_taiga");
   ResourceKey<Structure> ABANDONED_CAMP_PALE_GARDEN = createKey("abandoned_camp_pale_garden");
   ResourceKey<Structure> ABANDONED_CAMP_SAVANNA = createKey("abandoned_camp_savanna");
   ResourceKey<Structure> ABANDONED_CAMP_SNOWY_TAIGA = createKey("abandoned_camp_snowy_taiga");
   ResourceKey<Structure> ABANDONED_CAMP_SPARSE_JUNGLE = createKey("abandoned_camp_sparse_jungle");
   ResourceKey<Structure> ABANDONED_CAMP_SWAMP = createKey("abandoned_camp_swamp");
   ResourceKey<Structure> ABANDONED_CAMP_TAIGA = createKey("abandoned_camp_taiga");
   ResourceKey<Structure> ABANDONED_CAMP_WINDSWEPT_FOREST = createKey("abandoned_camp_windswept_forest");
   ResourceKey<Structure> ABANDONED_CAMP_WOODED_BADLANDS = createKey("abandoned_camp_wooded_badlands");

   private static ResourceKey<Structure> createKey(final String name) {
      return ResourceKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace(name));
   }
}
