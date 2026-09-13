package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class NamedEntityConvertUncheckedFix extends NamedEntityFix {
   public NamedEntityConvertUncheckedFix(final Schema outputSchema, final String name, final TypeReference type, final String entityName) {
      super(outputSchema, true, name, type, entityName);
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      Type<?> outputType = this.getOutputSchema().getChoiceType(this.type, this.entityName);
      return ExtraDataFixUtils.cast(outputType, entity);
   }
}
