package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.Locale;
import java.util.Optional;

public class OptionsLowerCaseLanguageFix extends DataFix {
   public OptionsLowerCaseLanguageFix(Schema p_16659_, boolean p_16660_) {
      super(p_16659_, p_16660_);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsLowerCaseLanguageFix", this.getInputSchema().getType(References.OPTIONS), p_16662_ -> p_16662_.update(DSL.remainderFinder(), p_326637_ -> {
            Optional<String> optional = p_326637_.get("lang").asString().result();
            return optional.isPresent() ? p_326637_.set("lang", p_326637_.createString(optional.get().toLowerCase(Locale.ROOT))) : p_326637_;
         })
      );
   }
}
