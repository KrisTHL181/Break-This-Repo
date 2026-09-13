package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class ScoreboardDisplaySlotFix extends DataFix {
   private static final Map<String, String> SLOT_RENAMES = ImmutableMap.builder()
      .put("slot_0", "list")
      .put("slot_1", "sidebar")
      .put("slot_2", "below_name")
      .put("slot_3", "sidebar.team.black")
      .put("slot_4", "sidebar.team.dark_blue")
      .put("slot_5", "sidebar.team.dark_green")
      .put("slot_6", "sidebar.team.dark_aqua")
      .put("slot_7", "sidebar.team.dark_red")
      .put("slot_8", "sidebar.team.dark_purple")
      .put("slot_9", "sidebar.team.gold")
      .put("slot_10", "sidebar.team.gray")
      .put("slot_11", "sidebar.team.dark_gray")
      .put("slot_12", "sidebar.team.blue")
      .put("slot_13", "sidebar.team.green")
      .put("slot_14", "sidebar.team.aqua")
      .put("slot_15", "sidebar.team.red")
      .put("slot_16", "sidebar.team.light_purple")
      .put("slot_17", "sidebar.team.yellow")
      .put("slot_18", "sidebar.team.white")
      .build();

   public ScoreboardDisplaySlotFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   private static @Nullable String rename(final String key) {
      return SLOT_RENAMES.get(key);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> scoreboardType = this.getInputSchema().getType(References.SAVED_DATA_SCOREBOARD);
      OpticFinder<?> rootTagFinder = scoreboardType.findField("data");
      return this.fixTypeEverywhereTyped(
         "Scoreboard DisplaySlot rename",
         scoreboardType,
         input -> input.updateTyped(
            rootTagFinder,
            scoreboardInfo -> scoreboardInfo.update(
               DSL.remainderFinder(),
               tag -> tag.update(
                  "DisplaySlots",
                  slots -> slots.updateMapValues(
                     pair -> pair.mapFirst(
                        key -> (Dynamic)DataFixUtils.orElse(key.asString().result().map(ScoreboardDisplaySlotFix::rename).map(key::createString), key)
                     )
                  )
               )
            )
         )
      );
   }
}
