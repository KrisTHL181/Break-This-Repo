package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class OptionsAccessibilityOnboardFix extends DataFix {
   public OptionsAccessibilityOnboardFix(Schema p_265364_) {
      super(p_265364_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsAccessibilityOnboardFix",
         this.getInputSchema().getType(References.OPTIONS),
         p_265152_ -> p_265152_.update(DSL.remainderFinder(), p_390335_ -> p_390335_.set("onboardAccessibility", p_390335_.createString("false")))
      );
   }
}
