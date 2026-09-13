package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.Function;
import java.util.stream.Stream;

public class PoiTypeRenameFix extends AbstractPoiSectionFix {
   private final Function<String, String> renamer;

   public PoiTypeRenameFix(Schema p_216710_, String p_216711_, Function<String, String> p_216712_) {
      super(p_216710_, p_216711_);
      this.renamer = p_216712_;
   }

   @Override
   protected <T> Stream<Dynamic<T>> processRecords(Stream<Dynamic<T>> p_216716_) {
      return p_216716_.map(
         p_216714_ -> p_216714_.update(
            "type", p_326642_ -> (Dynamic)DataFixUtils.orElse(p_326642_.asString().map(this.renamer).map(p_326642_::createString).result(), p_326642_)
         )
      );
   }
}
