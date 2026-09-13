package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.stream.Collectors;

public class OptionsKeyTranslationFix extends DataFix {
   public OptionsKeyTranslationFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsKeyTranslationFix",
         this.getInputSchema().getType(References.OPTIONS),
         input -> input.update(DSL.remainderFinder(), tag -> tag.getMapValues().map(map1 -> tag.createMap(map1.entrySet().stream().map(entry -> {
            if (((Dynamic)entry.getKey()).asString("").startsWith("key_")) {
               String oldValue = ((Dynamic)entry.getValue()).asString("");
               if (!oldValue.startsWith("key.mouse") && !oldValue.startsWith("scancode.")) {
                  return Pair.of((Dynamic)entry.getKey(), tag.createString("key.keyboard." + oldValue.substring("key.".length())));
               }
            }

            return Pair.of((Dynamic)entry.getKey(), (Dynamic)entry.getValue());
         }).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)))).result().orElse(tag))
      );
   }
}
