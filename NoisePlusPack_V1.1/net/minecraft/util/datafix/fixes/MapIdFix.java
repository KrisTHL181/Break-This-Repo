package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;

public class MapIdFix extends DataFix {
   public MapIdFix(Schema p_16396_) {
      super(p_16396_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "Map id fix",
         this.getInputSchema().getType(References.SAVED_DATA_MAP_INDEX),
         p_405251_ -> p_405251_.update(DSL.remainderFinder(), p_390310_ -> p_390310_.createMap(Map.of(p_390310_.createString("data"), p_390310_)))
      );
   }
}
