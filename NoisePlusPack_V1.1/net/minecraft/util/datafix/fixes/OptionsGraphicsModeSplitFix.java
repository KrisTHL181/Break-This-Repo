package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsGraphicsModeSplitFix extends DataFix {
   private final String newFieldName;
   private final String valueIfFast;
   private final String valueIfFancy;
   private final String valueIfFabulous;

   public OptionsGraphicsModeSplitFix(Schema p_452599_, String p_459191_, String p_456526_, String p_458400_, String p_453865_) {
      super(p_452599_, true);
      this.newFieldName = p_459191_;
      this.valueIfFast = p_456526_;
      this.valueIfFancy = p_458400_;
      this.valueIfFabulous = p_453865_;
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "graphicsMode split to " + this.newFieldName,
         this.getInputSchema().getType(References.OPTIONS),
         p_455613_ -> p_455613_.update(
            DSL.remainderFinder(),
            p_452604_ -> (Dynamic)DataFixUtils.orElseGet(
               p_452604_.get("graphicsMode")
                  .asString()
                  .map(p_453500_ -> p_452604_.set(this.newFieldName, p_452604_.createString(this.getValue(p_453500_))))
                  .result(),
               () -> p_452604_.set(this.newFieldName, p_452604_.createString(this.valueIfFancy))
            )
         )
      );
   }

   private String getValue(String p_456527_) {
      return switch (p_456527_) {
         case "2" -> this.valueIfFabulous;
         case "0" -> this.valueIfFast;
         default -> this.valueIfFancy;
      };
   }
}
