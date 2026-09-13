package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class OptionsForceDefaultGraphicsApiFix extends DataFix {
   public OptionsForceDefaultGraphicsApiFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsForceDefaultGraphicsApiFix",
         this.getInputSchema().getType(References.OPTIONS),
         input -> input.update(DSL.remainderFinder(), tag -> tag.set("preferredGraphicsBackend", tag.createString("default")))
      );
   }
}
