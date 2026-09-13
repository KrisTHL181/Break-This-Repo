package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class IglooMetadataRemovalFix extends DataFix {
   public IglooMetadataRemovalFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.STRUCTURE_FEATURE);
      return this.fixTypeEverywhereTyped("IglooMetadataRemovalFix", type, typed -> typed.update(DSL.remainderFinder(), IglooMetadataRemovalFix::fixTag));
   }

   private static <T> Dynamic<T> fixTag(final Dynamic<T> input) {
      boolean isIglooOnly = input.get("Children").asStreamOpt().map(s -> s.allMatch(IglooMetadataRemovalFix::isIglooPiece)).result().orElse(false);
      return isIglooOnly
         ? input.set("id", input.createString("Igloo")).remove("Children")
         : input.update("Children", IglooMetadataRemovalFix::removeIglooPieces);
   }

   private static <T> Dynamic<T> removeIglooPieces(final Dynamic<T> children) {
      return children.asStreamOpt().map(s -> s.filter(v -> !isIglooPiece((Dynamic<?>)v))).map(children::createList).result().orElse(children);
   }

   private static boolean isIglooPiece(final Dynamic<?> tag) {
      return tag.get("id").asString("").equals("Iglu");
   }
}
