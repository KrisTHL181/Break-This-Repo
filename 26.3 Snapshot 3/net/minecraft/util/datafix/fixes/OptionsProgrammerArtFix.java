package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsProgrammerArtFix extends DataFix {
   public OptionsProgrammerArtFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsProgrammerArtFix",
         this.getInputSchema().getType(References.OPTIONS),
         input -> input.update(DSL.remainderFinder(), tag -> tag.update("resourcePacks", this::fixList).update("incompatibleResourcePacks", this::fixList))
      );
   }

   private <T> Dynamic<T> fixList(final Dynamic<T> entry) {
      return entry.asString().result().map(s -> entry.createString(s.replace("\"programer_art\"", "\"programmer_art\""))).orElse(entry);
   }
}
