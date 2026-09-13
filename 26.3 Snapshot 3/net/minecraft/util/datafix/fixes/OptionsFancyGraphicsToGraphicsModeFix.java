package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsFancyGraphicsToGraphicsModeFix extends DataFix {
   public OptionsFancyGraphicsToGraphicsModeFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "fancyGraphics to graphicsMode",
         this.getInputSchema().getType(References.OPTIONS),
         input -> input.update(
            DSL.remainderFinder(), tag -> tag.renameAndFixField("fancyGraphics", "graphicsMode", OptionsFancyGraphicsToGraphicsModeFix::fixGraphicsMode)
         )
      );
   }

   private static <T> Dynamic<T> fixGraphicsMode(final Dynamic<T> field) {
      return "true".equals(field.asString("true")) ? field.createString("1") : field.createString("0");
   }
}
