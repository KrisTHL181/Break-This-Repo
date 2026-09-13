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

   public SignTextStrictJsonFix(final Schema outputSchema) {
      super(outputSchema, false, "SignTextStrictJsonFix", References.BLOCK_ENTITY, "Sign");
   }

   @Override
   protected Typed<?> fix(Typed<?> entity) {
      for (String lineField : LINE_FIELDS) {
         OpticFinder<?> lineF = entity.getType().findField(lineField);
         OpticFinder<Pair<String, String>> textComponentF = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
         entity = entity.updateTyped(
            lineF, line -> line.update(textComponentF, textComponent -> textComponent.mapSecond(LegacyComponentDataFixUtils::rewriteFromLenient))
         );
      }

      return entity;
   }
}
