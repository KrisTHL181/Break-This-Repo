package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class RandomSequenceSettingsFix extends DataFix {
   public RandomSequenceSettingsFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "RandomSequenceSettingsFix",
         this.getInputSchema().getType(References.SAVED_DATA_RANDOM_SEQUENCES),
         input -> input.update(DSL.remainderFinder(), container -> container.update("data", tag -> tag.emptyMap().set("sequences", tag)))
      );
   }
}
