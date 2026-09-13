package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class OptionsMusicToastFix extends DataFix {
   public OptionsMusicToastFix(Schema p_450671_, boolean p_460364_) {
      super(p_450671_, p_460364_);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsMusicToastFix",
         this.getInputSchema().getType(References.OPTIONS),
         p_456114_ -> p_456114_.update(
            DSL.remainderFinder(),
            p_458849_ -> p_458849_.renameAndFixField(
               "showNowPlayingToast",
               "musicToast",
               p_460891_ -> p_458849_.createString(p_460891_.asString("false").equals("false") ? "never" : "pause_and_toast")
            )
         )
      );
   }
}
