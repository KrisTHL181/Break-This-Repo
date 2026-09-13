package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class StructureReferenceCountFix extends DataFix {
   public StructureReferenceCountFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> structureInfo = this.getInputSchema().getType(References.STRUCTURE_FEATURE);
      return this.fixTypeEverywhereTyped(
         "Structure Reference Fix", structureInfo, input -> input.update(DSL.remainderFinder(), StructureReferenceCountFix::setCountToAtLeastOne)
      );
   }

   private static <T> Dynamic<T> setCountToAtLeastOne(final Dynamic<T> structureTag) {
      return structureTag.update(
         "references", references -> references.createInt(references.asNumber().map(Number::intValue).result().filter(number -> number > 0).orElse(1))
      );
   }
}
