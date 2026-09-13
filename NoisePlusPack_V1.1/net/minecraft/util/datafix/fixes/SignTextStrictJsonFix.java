package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class SignTextStrictJsonFix extends NamedEntityFix {
   private static final List<String> LINE_FIELDS = List.of("Text1", "Text2", "Text3", "Text4");

   public SignTextStrictJsonFix(Schema p_408226_) {
      super(p_408226_, false, "SignTextStrictJsonFix", References.BLOCK_ENTITY, "Sign");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_406923_) {
      for (String s : LINE_FIELDS) {
         OpticFinder<?> opticfinder = p_406923_.getType().findField(s);
         OpticFinder<Pair<String, String>> opticfinder1 = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
         p_406923_ = p_406923_.updateTyped(
            opticfinder, p_407278_ -> p_407278_.update(opticfinder1, p_408660_ -> p_408660_.mapSecond(LegacyComponentDataFixUtils::rewriteFromLenient))
         );
      }

      return p_406923_;
   }
}
