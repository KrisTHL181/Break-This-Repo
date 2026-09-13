package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.Mth;

public class GameRuleRegistryFix extends DataFix {
   public GameRuleRegistryFix(Schema p_454727_) {
      super(p_454727_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "GameRuleRegistryFix",
         this.getInputSchema().getType(References.LEVEL),
         p_457782_ -> p_457782_.update(
            DSL.remainderFinder(),
            p_460277_ -> p_460277_.renameAndFixField(
               "GameRules",
               "game_rules",
               p_460017_ -> {
                  boolean flag = Boolean.parseBoolean(p_460017_.get("doFireTick").asString("true"));
                  boolean flag1 = Boolean.parseBoolean(p_460017_.get("allowFireTicksAwayFromPlayer").asString("false"));
                  int i;
                  if (!flag) {
                     i = 0;
                  } else if (!flag1) {
                     i = 128;
                  } else {
                     i = -1;
                  }

                  if (i != 128) {
                     p_460017_ = p_460017_.set("minecraft:fire_spread_radius_around_player", p_460017_.createInt(i));
                  }

                  return p_460017_.remove("spawnChunkRadius")
                     .remove("entitiesWithPassengersCanUsePortals")
                     .remove("doFireTick")
                     .remove("allowFireTicksAwayFromPlayer")
                     .renameAndFixField("allowEnteringNetherUsingPortals", "minecraft:allow_entering_nether_using_portals", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("announceAdvancements", "minecraft:show_advancement_messages", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("blockExplosionDropDecay", "minecraft:block_explosion_drop_decay", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("commandBlockOutput", "minecraft:command_block_output", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("enableCommandBlocks", "minecraft:command_blocks_work", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("commandBlocksEnabled", "minecraft:command_blocks_work", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("commandModificationBlockLimit", "minecraft:max_block_modifications", p_456908_ -> convertInteger(p_456908_, 1))
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
                     .renameAndFixField("maxCommandChainLength", "minecraft:max_command_sequence_length", p_454038_ -> convertInteger(p_454038_, 0))
                     .renameAndFixField("maxCommandForkCount", "minecraft:max_command_forks", p_456966_ -> convertInteger(p_456966_, 0))
                     .renameAndFixField("maxEntityCramming", "minecraft:max_entity_cramming", p_454850_ -> convertInteger(p_454850_, 0))
                     .renameAndFixField("minecartMaxSpeed", "minecraft:max_minecart_speed", GameRuleRegistryFix::convertInteger)
                     .renameAndFixField("mobExplosionDropDecay", "minecraft:mob_explosion_drop_decay", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("mobGriefing", "minecraft:mob_griefing", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("naturalRegeneration", "minecraft:natural_health_regeneration", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField(
                        "playersNetherPortalCreativeDelay", "minecraft:players_nether_portal_creative_delay", p_454890_ -> convertInteger(p_454890_, 0)
                     )
                     .renameAndFixField(
                        "playersNetherPortalDefaultDelay", "minecraft:players_nether_portal_default_delay", p_460518_ -> convertInteger(p_460518_, 0)
                     )
                     .renameAndFixField("playersSleepingPercentage", "minecraft:players_sleeping_percentage", p_455552_ -> convertInteger(p_455552_, 0))
                     .renameAndFixField("projectilesCanBreakBlocks", "minecraft:projectiles_can_break_blocks", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("pvp", "minecraft:pvp", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("randomTickSpeed", "minecraft:random_tick_speed", p_450295_ -> convertInteger(p_450295_, 0))
                     .renameAndFixField("reducedDebugInfo", "minecraft:reduced_debug_info", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("sendCommandFeedback", "minecraft:send_command_feedback", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("showDeathMessages", "minecraft:show_death_messages", GameRuleRegistryFix::convertBoolean)
                     .renameAndFixField("snowAccumulationHeight", "minecraft:max_snow_accumulation_height", p_456555_ -> convertInteger(p_456555_, 0, 8))
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

   private static Dynamic<?> convertInteger(Dynamic<?> p_456914_) {
      return convertInteger(p_456914_, Integer.MIN_VALUE, Integer.MAX_VALUE);
   }

   private static Dynamic<?> convertInteger(Dynamic<?> p_456216_, int p_450847_) {
      return convertInteger(p_456216_, p_450847_, Integer.MAX_VALUE);
   }

   private static Dynamic<?> convertInteger(Dynamic<?> p_451298_, int p_458259_, int p_453543_) {
      String s = p_451298_.asString("");

      try {
         int i = Integer.parseInt(s);
         return p_451298_.createInt(Mth.clamp(i, p_458259_, p_453543_));
      } catch (NumberFormatException numberformatexception) {
         return p_451298_;
      }
   }

   private static Dynamic<?> convertBoolean(Dynamic<?> p_459968_) {
      return p_459968_.createBoolean(Boolean.parseBoolean(p_459968_.asString("")));
   }

   private static Dynamic<?> convertBooleanInverted(Dynamic<?> p_457107_) {
      return p_457107_.createBoolean(!Boolean.parseBoolean(p_457107_.asString("")));
   }
}
