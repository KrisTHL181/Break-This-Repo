package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import org.slf4j.Logger;

public class LevelUUIDFix extends AbstractUUIDFix {
   private static final Logger LOGGER = LogUtils.getLogger();

   public LevelUUIDFix(final Schema outputSchema) {
      super(outputSchema, References.LEVEL);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(this.typeReference);
      OpticFinder<?> customBossEventsF = type.findField("CustomBossEvents");
      OpticFinder<?> customBossEventF = DSL.typeFinder(
         DSL.and(DSL.optional(DSL.field("Name", this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT))), DSL.remainderType())
      );
      return this.fixTypeEverywhereTyped(
         "LevelUUIDFix",
         type,
         input -> input.update(DSL.remainderFinder(), tag -> {
               tag = this.updateDragonFight(tag);
               return this.updateWanderingTrader(tag);
            })
            .updateTyped(
               customBossEventsF,
               customBossEvents -> customBossEvents.updateTyped(customBossEventF, event -> event.update(DSL.remainderFinder(), this::updateCustomBossEvent))
            )
      );
   }

   private Dynamic<?> updateWanderingTrader(final Dynamic<?> tag) {
      return replaceUUIDString(tag, "WanderingTraderId", "WanderingTraderId").orElse(tag);
   }

   private Dynamic<?> updateDragonFight(final Dynamic<?> tag) {
      return tag.update(
         "DimensionData",
         dimensionDataMap -> dimensionDataMap.updateMapValues(
            dimensionDataPair -> dimensionDataPair.mapSecond(
               dimensionData -> dimensionData.update(
                  "DragonFight", dragonfight -> replaceUUIDLeastMost(dragonfight, "DragonUUID", "Dragon").orElse(dragonfight)
               )
            )
         )
      );
   }

   private Dynamic<?> updateCustomBossEvent(final Dynamic<?> tag) {
      return tag.update("Players", players -> tag.createList(players.asStream().map(player -> (Dynamic)createUUIDFromML((Dynamic<?>)player).orElseGet(() -> {
         LOGGER.warn("CustomBossEvents contains invalid UUIDs.");
         return (Dynamic<?>)player;
      }))));
   }
}
