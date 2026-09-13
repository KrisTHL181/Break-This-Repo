package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.function.Function;

public class LevelDatToSavedDataPreparationFix extends DataFix {
   public LevelDatToSavedDataPreparationFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> levelType = this.getInputSchema().getType(References.LEVEL);
      return this.writeFixAndRead("LevelSplitSavedDataPreparationFix", levelType, this.getOutputSchema().getType(References.LEVEL), levelData -> {
         levelData = fixDragonFight(levelData);
         levelData = fixWanderingTrader(levelData);
         levelData = fixWeatherData(levelData);
         levelData = fixScheduledEvents(levelData);
         return fixWorldGenSettings(levelData);
      });
   }

   private static Dynamic<?> fixDragonFight(final Dynamic<?> levelData) {
      return levelData.renameAndFixField(
         "DragonFight",
         "dragon_fight",
         dragonFight -> {
            Dynamic<?> newFight = dragonFight.renameField("NeedsStateScanning", "needs_state_scanning")
               .renameField("DragonKilled", "dragon_killed")
               .renameField("PreviouslyKilled", "previously_killed")
               .renameField("Dragon", "dragon_uuid")
               .renameField("ExitPortalLocation", "exit_portal_location")
               .renameField("Gateways", "gateways");
            boolean isRespawning = dragonFight.get("IsRespawning").asBoolean(false);
            if (isRespawning) {
               newFight = newFight.set("respawn_stage", levelData.createString("start")).set("respawn_time", levelData.createInt(0));
            }

            return newFight.remove("IsRespawning");
         }
      );
   }

   private static Dynamic<?> fixWanderingTrader(final Dynamic<?> levelData) {
      Dynamic<?> wanderingTraderData = levelData.emptyMap()
         .set("spawn_delay", levelData.createInt(levelData.get("WanderingTraderSpawnDelay").asInt(24000)))
         .set("spawn_chance", levelData.createInt(levelData.get("WanderingTraderSpawnChance").asInt(25)));
      return levelData.set("wandering_trader_migration_data", wanderingTraderData)
         .remove("WanderingTraderSpawnDelay")
         .remove("WanderingTraderSpawnChance")
         .remove("WanderingTraderId");
   }

   private static Dynamic<?> fixWeatherData(final Dynamic<?> levelData) {
      Dynamic<?> weatherData = levelData.emptyMap()
         .set("clear_weather_time", levelData.createInt(levelData.get("clearWeatherTime").asInt(0)))
         .set("rain_time", levelData.createInt(levelData.get("rainTime").asInt(0)))
         .set("thunder_time", levelData.createInt(levelData.get("thunderTime").asInt(0)))
         .set("raining", levelData.createBoolean(levelData.get("raining").asBoolean(false)))
         .set("thundering", levelData.createBoolean(levelData.get("thundering").asBoolean(false)));
      return levelData.remove("clearWeatherTime")
         .remove("rainTime")
         .remove("thunderTime")
         .remove("raining")
         .remove("thundering")
         .set("weather_data", weatherData);
   }

   private static Dynamic<?> fixScheduledEvents(final Dynamic<?> levelData) {
      List<? extends Dynamic<?>> scheduledEvents = levelData.get("ScheduledEvents").asList(Function.identity());
      Dynamic<?> eventList = levelData.createList(
         scheduledEvents.stream()
            .map(
               event -> event.renameField("Name", "id")
                  .renameField("TriggerTime", "trigger_time")
                  .renameAndFixField("Callback", "callback", callback -> callback.renameField("Type", "type").renameField("Name", "id"))
            )
      );
      Dynamic<?> newEvents = levelData.emptyMap();
      newEvents = newEvents.set("events", eventList);
      return levelData.remove("ScheduledEvents").set("scheduled_events", newEvents);
   }

   private static Dynamic<?> fixWorldGenSettings(final Dynamic<?> levelData) {
      return levelData.renameAndFixField("WorldGenSettings", "world_gen_settings", tag -> tag.renameField("generate_features", "generate_structures"));
   }
}
