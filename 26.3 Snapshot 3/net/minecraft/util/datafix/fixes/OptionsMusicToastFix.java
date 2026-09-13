package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class OptionsMusicToastFix extends DataFix {
   public OptionsMusicToastFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsMusicToastFix",
         this.getInputSchema().getType(References.OPTIONS),
         input -> input.update(
            DSL.remainderFinder(),
            tag -> tag.renameAndFixField(
               "showNowPlayingToast", "musicToast", old -> tag.createString(old.asString("false").equals("false") ? "never" : "pause_and_toast")
            )
         )
      );
   }
}
