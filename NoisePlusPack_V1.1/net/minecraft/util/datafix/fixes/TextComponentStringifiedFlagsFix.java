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
   public TextComponentStringifiedFlagsFix(Schema p_396784_) {
      super(p_396784_, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, Either<?, Pair<?, Pair<?, Pair<?, Dynamic<?>>>>>>> type = this.getInputSchema().getType(References.TEXT_COMPONENT);
      return this.fixTypeEverywhere(
         "TextComponentStringyFlagsFix",
         type,
         p_394433_ -> p_394628_ -> p_394628_.mapSecond(
            p_394878_ -> p_394878_.mapRight(
               p_392497_ -> p_392497_.mapSecond(
                  p_395199_ -> p_395199_.mapSecond(
                     p_391281_ -> p_391281_.mapSecond(
                        p_392850_ -> p_392850_.update("bold", TextComponentStringifiedFlagsFix::stringToBool)
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

   private static <T> Dynamic<T> stringToBool(Dynamic<T> p_395938_) {
      Optional<String> optional = p_395938_.asString().result();
      return optional.isPresent() ? p_395938_.createBoolean(Boolean.parseBoolean(optional.get())) : p_395938_;
   }
}
