package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;
import java.util.function.IntFunction;

public class EntityVariantFix extends NamedEntityFix {
   private final String fieldName;
   private final IntFunction<String> idConversions;

   public EntityVariantFix(Schema p_216623_, String p_216624_, TypeReference p_216625_, String p_216626_, String p_216627_, IntFunction<String> p_216628_) {
      super(p_216623_, false, p_216624_, p_216625_, p_216626_);
      this.fieldName = p_216627_;
      this.idConversions = p_216628_;
   }

   private static <T> Dynamic<T> updateAndRename(Dynamic<T> p_216637_, String p_216638_, String p_216639_, Function<Dynamic<T>, Dynamic<T>> p_216640_) {
      return p_216637_.map(
         p_326583_ -> {
            DynamicOps<T> dynamicops = p_216637_.getOps();
            Function<T, T> function = p_216656_ -> (T)p_216640_.apply(new Dynamic(dynamicops, p_216656_)).getValue();
            return dynamicops.get(p_326583_, p_216638_)
               .map(p_216652_ -> dynamicops.set(p_326583_, p_216639_, function.apply((T)p_216652_)))
               .result()
               .orElse(p_326583_);
         }
      );
   }

   @Override
   protected Typed<?> fix(Typed<?> p_216630_) {
      return p_216630_.update(
         DSL.remainderFinder(),
         p_216632_ -> updateAndRename(
            p_216632_,
            this.fieldName,
            "variant",
            p_326578_ -> (Dynamic)DataFixUtils.orElse(
               p_326578_.asNumber().map(p_216635_ -> p_326578_.createString(this.idConversions.apply(p_216635_.intValue()))).result(), p_326578_
            )
         )
      );
   }
}
