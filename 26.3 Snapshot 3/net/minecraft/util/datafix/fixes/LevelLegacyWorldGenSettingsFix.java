package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;

public class LevelLegacyWorldGenSettingsFix extends DataFix {
   private static final String WORLD_GEN_SETTINGS = "WorldGenSettings";
   private static final List<String> OLD_SETTINGS_KEYS = List.of(
      "RandomSeed", "generatorName", "generatorOptions", "generatorVersion", "legacy_custom_options", "MapFeatures", "BonusChest"
   );

   public LevelLegacyWorldGenSettingsFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "LevelLegacyWorldGenSettingsFix", this.getInputSchema().getType(References.LEVEL), input -> input.update(DSL.remainderFinder(), dataTag -> {
            Dynamic<?> worldGenSettings = dataTag.get("WorldGenSettings").orElseEmptyMap();

            for (String key : OLD_SETTINGS_KEYS) {
               Optional<? extends Dynamic<?>> oldSetting = dataTag.get(key).result();
               if (oldSetting.isPresent()) {
                  dataTag = dataTag.remove(key);
                  worldGenSettings = worldGenSettings.set(key, oldSetting.get());
               }
            }

            return dataTag.set("WorldGenSettings", worldGenSettings);
         })
      );
   }
}
