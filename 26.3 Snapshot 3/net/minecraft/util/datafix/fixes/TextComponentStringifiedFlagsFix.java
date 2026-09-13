package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class TextComponentStringifiedFlagsFix extends DataFix {
   public TextComponentStringifiedFlagsFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, Either<?, Pair<?, Pair<?, Pair<?, Dynamic<?>>>>>>> textComponentType = this.getInputSchema().getType(References.TEXT_COMPONENT);
      return this.fixTypeEverywhere(
         "TextComponentStringyFlagsFix",
         textComponentType,
         ops -> named -> named.mapSecond(
            e1 -> e1.mapRight(
               p2 -> p2.mapSecond(
                  p3 -> p3.mapSecond(
                     p4 -> p4.mapSecond(
                        remainder -> remainder.update("bold", TextComponentStringifiedFlagsFix::stringToBool)
                           .update("italic", TextComponentStringifiedFlagsFix::stringToBool)
                           .update("underlined", TextComponentStringifiedFlagsFix::stringToBool)
                           .update("strikethrough", TextComponentStringifiedFlagsFix::stringToBool)
                           .update("obfuscated", TextComponentStringifiedFlagsFix::stringToBool)
                     )
                  )
               )
            )
         )
      );
   }

   private static <T> Dynamic<T> stringToBool(final Dynamic<T> input) {
      Optional<String> string = input.asString().result();
      return string.isPresent() ? input.createBoolean(Boolean.parseBoolean(string.get())) : input;
   }
}
