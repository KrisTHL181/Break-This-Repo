package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class OptionsRenameFieldFix extends DataFix {
   private final String fixName;
   private final String fieldFrom;
   private final String fieldTo;

   public OptionsRenameFieldFix(Schema p_16670_, boolean p_16671_, String p_16672_, String p_16673_, String p_16674_) {
      super(p_16670_, p_16671_);
      this.fixName = p_16672_;
      this.fieldFrom = p_16673_;
      this.fieldTo = p_16674_;
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.fixName,
         this.getInputSchema().getType(References.OPTIONS),
         p_16676_ -> p_16676_.update(DSL.remainderFinder(), p_390339_ -> p_390339_.renameField(this.fieldFrom, this.fieldTo))
      );
   }
}
