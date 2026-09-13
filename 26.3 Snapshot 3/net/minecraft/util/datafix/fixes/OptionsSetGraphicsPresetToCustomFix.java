package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class OptionsSetGraphicsPresetToCustomFix extends DataFix {
   public OptionsSetGraphicsPresetToCustomFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "graphicsPreset set to \"custom\"",
         this.getInputSchema().getType(References.OPTIONS),
         input -> input.update(DSL.remainderFinder(), tag -> tag.set("graphicsPreset", tag.createString("custom")))
      );
   }
}
