package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.Function;

public class AdvancementsRenameFix extends DataFix {
   private final String name;
   private final Function<String, String> renamer;

   public AdvancementsRenameFix(final Schema outputSchema, final boolean changesType, final String name, final Function<String, String> renamer) {
      super(outputSchema, changesType);
      this.name = name;
      this.renamer = renamer;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.name, this.getInputSchema().getType(References.ADVANCEMENTS), input -> input.update(DSL.remainderFinder(), tag -> tag.updateMapValues(entry -> {
            String id = ((Dynamic)entry.getFirst()).asString("");
            return entry.mapFirst(f -> tag.createString(this.renamer.apply(id)));
         }))
      );
   }
}
