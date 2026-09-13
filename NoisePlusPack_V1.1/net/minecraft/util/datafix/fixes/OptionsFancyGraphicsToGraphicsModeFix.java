package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsFancyGraphicsToGraphicsModeFix extends DataFix {
   public OptionsFancyGraphicsToGraphicsModeFix(Schema p_453740_) {
      super(p_453740_, true);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "fancyGraphics to graphicsMode",
         this.getInputSchema().getType(References.OPTIONS),
         p_454138_ -> p_454138_.update(
            DSL.remainderFinder(),
            p_460744_ -> p_460744_.renameAndFixField("fancyGraphics", "graphicsMode", OptionsFancyGraphicsToGraphicsModeFix::fixGraphicsMode)
         )
      );
   }

   private static <T> Dynamic<T> fixGraphicsMode(Dynamic<T> p_454181_) {
      return "true".equals(p_454181_.asString("true")) ? p_454181_.createString("1") : p_454181_.createString("0");
   }
}
