package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.Mth;

public class GameRuleRegistryFix extends DataFix {
   public GameRuleRegistryFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "GameRuleRegistryFix",
         this.getInputSchema().getType(References.LEVEL),
         input -> input.update(
            DSL.remainderFinder(),
            tag -> tag.renameAndFixField(
               "GameRules",
               "game_rules",
               gameRules -> {
                  boolean doFireTick = Boolean.parseBoolean(gameRules.get("doFireTick").asString("true"));
                  boolean allowFireTicksAwayFromPlayer = Boolean.parseBoolean(gameRules.get("allowFireTicksAwayFromPlayer").asString("false"));
                  int fireSpreadRadius;
                  if (!doFireTick) {
                     fireSpreadRadius = 0;
                  } else if (!allowFireTicksAwayFromPlayer) {
                     fireSpreadRadius = 128;
                  } else {
                     fireSpreadRadius = -1;
                  }

                  if (fireSpreadRadius != 128) {
                     gameRules = gameRules.set("minecraft:fire_spread_radius_around_player", gameRules.createInt(fireSpreadRadius));
                  }

                  return gameRules.remove("spawnChunkRadius")
                     .remove("entitiesWithPassengersCanUsePortals")
                     .remove("gameLoopFunction")
                     .remove("doFireTick")
                     .remove("allowFireTicksAwayFromPlayer")
                     .renameAndFixField("allowEnteringNetherUsingPortals", "minecraft:allow_entering_nether_using_portals", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("announceAdvancements", "minecraft:show_advancement_messages", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("blockExplosionDropDecay", "minecraft:block_explosion_drop_decay", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("commandBlockOutput", "minecraft:command_block_output", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("enableCommandBlocks", "minecraft:command_blocks_work", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("commandBlocksEnabled", "minecraft:command_blocks_work", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("commandModificationBlockLimit", "minecraft:max_block_modifications", oldValue -> convertInteger(oldValue, 1))
                     .renameAndFixField("disableElytraMovementCheck", "minecraft:elytra_movement_check", GameRuleRegistryFix::convertBooleanInverted)
                     .renameAndFixField("disablePlayerMovementCheck", "minecraft:player_movement_check", GameRuleRegistryFix::convertBooleanInverted)
                     .renameAndFixField("disableRaids", "minecraft:raids", GameRuleRegistryFix::convertBooleanInverted)
                     .renameAndFixField("doDaylightCycle", "minecraft:advance_time", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("doEntityDrops", "minecraft:entity_drops", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("doImmediateRespawn", "minecraft:immediate_respawn", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("doInsomnia", "minecraft:spawn_phantoms", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("doLimitedCrafting", "minecraft:limited_crafting", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("doMobLoot", "minecraft:mob_drops", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("doMobSpawning", "minecraft:spawn_mobs", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("doPatrolSpawning", "minecraft:spawn_patrols", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("doTileDrops", "minecraft:block_drops", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("doTraderSpawning", "minecraft:spawn_wandering_traders", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("doVinesSpread", "minecraft:spread_vines", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("doWardenSpawning", "minecraft:spawn_wardens", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("doWeatherCycle", "minecraft:advance_weather", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("drowningDamage", "minecraft:drowning_damage", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("enderPearlsVanishOnDeath", "minecraft:ender_pearls_vanish_on_death", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("fallDamage", "minecraft:fall_damage", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("fireDamage", "minecraft:fire_damage", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("forgiveDeadPlayers", "minecraft:forgive_dead_players", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("freezeDamage", "minecraft:freeze_damage", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("globalSoundEvents", "minecraft:global_sound_events", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("keepInventory", "minecraft:keep_inventory", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("lavaSourceConversion", "minecraft:lava_source_conversion", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("locatorBar", "minecraft:locator_bar", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("logAdminCommands", "minecraft:log_admin_commands", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("maxCommandChainLength", "minecraft:max_command_sequence_length", oldValue -> convertInteger(oldValue, 0))
                     .renameAndFixField("maxCommandForkCount", "minecraft:max_command_forks", oldValue -> convertInteger(oldValue, 0))
                     .renameAndFixField("maxEntityCramming", "minecraft:max_entity_cramming", oldValue -> convertInteger(oldValue, 0))
                     .renameAndFixField("minecartMaxSpeed", "minecraft:max_minecart_speed", GameRuleRegistryFix::convertInteger)
                     .renameAndFixField("mobExplosionDropDecay", "minecraft:mob_explosion_drop_decay", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("mobGriefing", "minecraft:mob_griefing", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("naturalRegeneration", "minecraft:natural_health_regeneration", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField(
                        "playersNetherPortalCreativeDelay", "minecraft:players_nether_portal_creative_delay", oldValue -> convertInteger(oldValue, 0)
                     )
                     .renameAndFixField(
                        "playersNetherPortalDefaultDelay", "minecraft:players_nether_portal_default_delay", oldValue -> convertInteger(oldValue, 0)
                     )
                     .renameAndFixField("playersSleepingPercentage", "minecraft:players_sleeping_percentage", oldValue -> convertInteger(oldValue, 0))
                     .renameAndFixField("projectilesCanBreakBlocks", "minecraft:projectiles_can_break_blocks", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("pvp", "minecraft:pvp", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("randomTickSpeed", "minecraft:random_tick_speed", oldValue -> convertInteger(oldValue, 0))
                     .renameAndFixField("reducedDebugInfo", "minecraft:reduced_debug_info", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("sendCommandFeedback", "minecraft:send_command_feedback", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("showDeathMessages", "minecraft:show_death_messages", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("snowAccumulationHeight", "minecraft:max_snow_accumulation_height", oldValue -> convertInteger(oldValue, 0, 8))
                     .renameAndFixField("spawnMonsters", "minecraft:spawn_monsters", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("spawnRadius", "minecraft:respawn_radius", GameRuleRegistryFix::convertInteger)
                     .renameAndFixField("spawnerBlocksEnabled", "minecraft:spawner_blocks_work", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("spectatorsGenerateChunks", "minecraft:spectators_generate_chunks", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("tntExplodes", "minecraft:tnt_explodes", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("tntExplosionDropDecay", "minecraft:tnt_explosion_drop_decay", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("universalAnger", "minecraft:universal_anger", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("waterSourceConversion", "minecraft:water_source_conversion", GameRuleRegistryFix::convertBoolean);
               }
            )
         )
      );
   }

   private static Dynamic<?> convertInteger(final Dynamic<?> oldValue) {
      return convertInteger(oldValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
   }

   private static Dynamic<?> convertInteger(final Dynamic<?> oldValue, final int min) {
      return convertInteger(oldValue, min, Integer.MAX_VALUE);
   }

   private static Dynamic<?> convertInteger(final Dynamic<?> oldValue, final int min, final int max) {
      String stringValue = oldValue.asString("");

      try {
         int parsedValue = Integer.parseInt(stringValue);
         return oldValue.createInt(Mth.clamp(parsedValue, min, max));
      } catch (NumberFormatException ignored) {
         return oldValue;
      }
   }

   private static Dynamic<?> convertBoolean(final Dynamic<?> oldValue) {
      return oldValue.createBoolean(Boolean.parseBoolean(oldValue.asString("")));
   }

   private static Dynamic<?> convertBooleanInverted(final Dynamic<?> oldValue) {
      return oldValue.createBoolean(!Boolean.parseBoolean(oldValue.asString("")));
   }
}
