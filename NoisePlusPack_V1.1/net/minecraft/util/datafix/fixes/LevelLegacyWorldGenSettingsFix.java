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

   public LevelLegacyWorldGenSettingsFix(Schema p_311836_) {
      super(p_311836_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "LevelLegacyWorldGenSettingsFix", this.getInputSchema().getType(References.LEVEL), p_311579_ -> p_311579_.update(DSL.remainderFinder(), p_309406_ -> {
            Dynamic<?> dynamic = p_309406_.get("WorldGenSettings").orElseEmptyMap();

            for (String s : OLD_SETTINGS_KEYS) {
               Optional<? extends Dynamic<?>> optional = p_309406_.get(s).result();
               if (optional.isPresent()) {
                  p_309406_ = p_309406_.remove(s);
                  dynamic = dynamic.set(s, optional.get());
               }
            }

            return p_309406_.set("WorldGenSettings", dynamic);
         })
      );
   }
}
