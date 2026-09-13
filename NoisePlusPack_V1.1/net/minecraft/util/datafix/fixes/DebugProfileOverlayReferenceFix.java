package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class DebugProfileOverlayReferenceFix extends DataFix {
   public DebugProfileOverlayReferenceFix(Schema p_454056_) {
      super(p_454056_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "DebugProfileOverlayReferenceFix",
         this.getInputSchema().getType(References.DEBUG_PROFILE),
         p_453232_ -> p_453232_.update(
            DSL.remainderFinder(),
            p_452069_ -> p_452069_.update(
               "custom",
               p_450465_ -> p_450465_.updateMapValues(
                  p_452097_ -> p_452097_.mapSecond(p_453841_ -> p_453841_.asString("").equals("inF3") ? p_453841_.createString("inOverlay") : p_453841_)
               )
            )
         )
      );
   }
}
