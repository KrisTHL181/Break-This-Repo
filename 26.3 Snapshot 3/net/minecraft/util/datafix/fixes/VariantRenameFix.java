package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;

public class VariantRenameFix extends NamedEntityFix {
   private final Map<String, String> renames;

   public VariantRenameFix(final Schema outputSchema, final String name, final TypeReference type, final String entityName, final Map<String, String> renames) {
      super(outputSchema, false, name, type, entityName);
      this.renames = renames;
   }

   @Override
   protected Typed<?> fix(final Typed<?> typed) {
      return typed.update(
         DSL.remainderFinder(),
         remainder -> remainder.update(
            "variant",
            variant -> (Dynamic)DataFixUtils.orElse(variant.asString().map(v -> variant.createString(this.renames.getOrDefault(v, v))).result(), variant)
         )
      );
   }
}
