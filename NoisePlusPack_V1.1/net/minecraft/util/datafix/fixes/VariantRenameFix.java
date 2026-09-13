package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;

public class VariantRenameFix extends NamedEntityFix {
   private final Map<String, String> renames;

   public VariantRenameFix(Schema p_216742_, String p_216743_, TypeReference p_216744_, String p_216745_, Map<String, String> p_216746_) {
      super(p_216742_, false, p_216743_, p_216744_, p_216745_);
      this.renames = p_216746_;
   }

   @Override
   protected Typed<?> fix(Typed<?> p_216748_) {
      return p_216748_.update(
         DSL.remainderFinder(),
         p_216750_ -> p_216750_.update(
            "variant",
            p_326660_ -> (Dynamic)DataFixUtils.orElse(
               p_326660_.asString().map(p_216753_ -> p_326660_.createString(this.renames.getOrDefault(p_216753_, p_216753_))).result(), p_326660_
            )
         )
      );
   }
}
