package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class OptionsSetGraphicsPresetToCustomFix extends DataFix {
   public OptionsSetGraphicsPresetToCustomFix(Schema p_452353_) {
      super(p_452353_, true);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "graphicsPreset set to \"custom\"",
         this.getInputSchema().getType(References.OPTIONS),
         p_451496_ -> p_451496_.update(DSL.remainderFinder(), p_455268_ -> p_455268_.set("graphicsPreset", p_455268_.createString("custom")))
      );
   }
}
