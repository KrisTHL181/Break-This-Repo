package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class OptionsMenuBlurrinessFix extends DataFix {
   public OptionsMenuBlurrinessFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsMenuBlurrinessFix",
         this.getInputSchema().getType(References.OPTIONS),
         input -> input.update(DSL.remainderFinder(), tag -> tag.update("menuBackgroundBlurriness", value -> {
            int intValue = this.convertToIntRange(value.asString("0.5"));
            return value.createString(String.valueOf(intValue));
         }))
      );
   }

   private int convertToIntRange(final String floatBlurriness) {
      try {
         return Math.round(Float.parseFloat(floatBlurriness) * 10.0F);
      } catch (NumberFormatException e) {
         return 5;
      }
   }
}
