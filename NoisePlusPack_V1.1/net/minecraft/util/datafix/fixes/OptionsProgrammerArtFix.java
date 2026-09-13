package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsProgrammerArtFix extends DataFix {
   public OptionsProgrammerArtFix(Schema p_250038_) {
      super(p_250038_, false);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsProgrammerArtFix",
         this.getInputSchema().getType(References.OPTIONS),
         p_248578_ -> p_248578_.update(
            DSL.remainderFinder(), p_250184_ -> p_250184_.update("resourcePacks", this::fixList).update("incompatibleResourcePacks", this::fixList)
         )
      );
   }

   private <T> Dynamic<T> fixList(Dynamic<T> p_249761_) {
      return p_249761_.asString()
         .result()
         .map(p_250930_ -> p_249761_.createString(p_250930_.replace("\"programer_art\"", "\"programmer_art\"")))
         .orElse(p_249761_);
   }
}
