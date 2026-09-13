package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsGraphicsModeSplitFix extends DataFix {
   private final String newFieldName;
   private final String valueIfFast;
   private final String valueIfFancy;
   private final String valueIfFabulous;

   public OptionsGraphicsModeSplitFix(
      final Schema outputSchema, final String newFieldName, final String valueIfFast, final String valueIfFancy, final String valueIfFabulous
   ) {
      super(outputSchema, true);
      this.newFieldName = newFieldName;
      this.valueIfFast = valueIfFast;
      this.valueIfFancy = valueIfFancy;
      this.valueIfFabulous = valueIfFabulous;
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "graphicsMode split to " + this.newFieldName,
         this.getInputSchema().getType(References.OPTIONS),
         input -> input.update(
            DSL.remainderFinder(),
            tag -> (Dynamic)DataFixUtils.orElseGet(
               tag.get("graphicsMode").asString().map(mode -> tag.set(this.newFieldName, tag.createString(this.getValue(mode)))).result(),
               () -> tag.set(this.newFieldName, tag.createString(this.valueIfFancy))
            )
         )
      );
   }

   private String getValue(final String mode) {
      return switch (mode) {
         case "2" -> this.valueIfFabulous;
         case "0" -> this.valueIfFast;
         default -> this.valueIfFancy;
      };
   }
}
