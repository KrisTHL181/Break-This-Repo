package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;

public class MapIdFix extends DataFix {
   public MapIdFix(final Schema schema) {
      super(schema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "Map id fix",
         this.getInputSchema().getType(References.SAVED_DATA_MAP_INDEX),
         input -> input.update(DSL.remainderFinder(), tag -> tag.createMap(Map.of(tag.createString("data"), tag)))
      );
   }
}
