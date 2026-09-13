package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemapChunkStatusFix extends DataFix {
   private final String name;
   private final UnaryOperator<String> mapper;

   public RemapChunkStatusFix(Schema p_281350_, String p_283581_, UnaryOperator<String> p_282501_) {
      super(p_281350_, false);
      this.name = p_283581_;
      this.mapper = p_282501_;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.name,
         this.getInputSchema().getType(References.CHUNK),
         p_283662_ -> p_283662_.update(
            DSL.remainderFinder(),
            p_284697_ -> p_284697_.update("Status", this::fixStatus)
               .update("below_zero_retrogen", p_282869_ -> p_282869_.update("target_status", this::fixStatus))
         )
      );
   }

   private <T> Dynamic<T> fixStatus(Dynamic<T> p_281410_) {
      Optional<Dynamic<T>> optional = p_281410_.asString().result().map(NamespacedSchema::ensureNamespaced).map(this.mapper).map(p_281410_::createString);
      return (Dynamic<T>)DataFixUtils.orElse(optional, p_281410_);
   }
}
