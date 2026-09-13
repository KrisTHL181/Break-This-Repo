package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class OptionsRenameFieldFix extends DataFix {
   private final String fixName;
   private final String fieldFrom;
   private final String fieldTo;

   public OptionsRenameFieldFix(final Schema outputSchema, final boolean changesType, final String fixName, final String fieldFrom, final String fieldTo) {
      super(outputSchema, changesType);
      this.fixName = fixName;
      this.fieldFrom = fieldFrom;
      this.fieldTo = fieldTo;
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.fixName,
         this.getInputSchema().getType(References.OPTIONS),
         input -> input.update(DSL.remainderFinder(), tag -> tag.renameField(this.fieldFrom, this.fieldTo))
      );
   }
}
