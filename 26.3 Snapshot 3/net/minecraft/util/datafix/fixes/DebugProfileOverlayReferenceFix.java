package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class DebugProfileOverlayReferenceFix extends DataFix {
   public DebugProfileOverlayReferenceFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "DebugProfileOverlayReferenceFix",
         this.getInputSchema().getType(References.DEBUG_PROFILE),
         typed -> typed.update(
            DSL.remainderFinder(),
            file -> file.update(
               "custom",
               custom -> custom.updateMapValues(pair -> pair.mapSecond(value -> value.asString("").equals("inF3") ? value.createString("inOverlay") : value))
            )
         )
      );
   }
}
