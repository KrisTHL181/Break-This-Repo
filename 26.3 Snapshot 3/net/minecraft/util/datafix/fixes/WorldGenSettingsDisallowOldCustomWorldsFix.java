package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.nbt.NbtFormatException;

public class WorldGenSettingsDisallowOldCustomWorldsFix extends DataFix {
   public WorldGenSettingsDisallowOldCustomWorldsFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> worldGenSettingsType = this.getInputSchema().getType(References.WORLD_GEN_SETTINGS);
      OpticFinder<?> dimensionsFinder = worldGenSettingsType.findField("dimensions");
      return this.fixTypeEverywhereTyped(
         "WorldGenSettingsDisallowOldCustomWorldsFix_" + this.getOutputSchema().getVersionKey(),
         worldGenSettingsType,
         input -> input.updateTyped(dimensionsFinder, dimensions -> {
            dimensions.write().map(tag -> tag.getMapValues().map(map -> {
               map.forEach((key, value) -> {
                  if (value.get("type").asString().result().isEmpty()) {
                     throw new NbtFormatException("Unable load old custom worlds.");
                  }
               });
               return map;
            }));
            return dimensions;
         })
      );
   }
}
