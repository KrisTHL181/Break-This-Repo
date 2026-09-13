package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsAddTextBackgroundFix extends DataFix {
   public OptionsAddTextBackgroundFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsAddTextBackgroundFix",
         this.getInputSchema().getType(References.OPTIONS),
         input -> input.update(DSL.remainderFinder(), tag -> (Dynamic)DataFixUtils.orElse(tag.get("chatOpacity").asString().map(value -> {
            double opacity = this.calculateBackground(value);
            return tag.set("textBackgroundOpacity", tag.createString(String.valueOf(opacity)));
         }).result(), tag))
      );
   }

   private double calculateBackground(final String textOpacity) {
      try {
         double textAlpha = 0.9 * Double.parseDouble(textOpacity) + 0.1;
         return textAlpha / 2.0;
      } catch (NumberFormatException e) {
         return 0.5;
      }
   }
}
