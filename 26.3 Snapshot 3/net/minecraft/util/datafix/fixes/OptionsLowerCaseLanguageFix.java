package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.Locale;
import java.util.Optional;

public class OptionsLowerCaseLanguageFix extends DataFix {
   public OptionsLowerCaseLanguageFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsLowerCaseLanguageFix", this.getInputSchema().getType(References.OPTIONS), input -> input.update(DSL.remainderFinder(), tag -> {
            Optional<String> lang = tag.get("lang").asString().result();
            return lang.isPresent() ? tag.set("lang", tag.createString(lang.get().toLowerCase(Locale.ROOT))) : tag;
         })
      );
   }
}
