package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Objects;

public class ChunkStatusFix extends DataFix {
   public ChunkStatusFix(final Schema schema, final boolean changesType) {
      super(schema, changesType);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> chunkType = this.getInputSchema().getType(References.CHUNK);
      Type<?> levelType = chunkType.findFieldType("Level");
      OpticFinder<?> levelF = DSL.fieldFinder("Level", levelType);
      return this.fixTypeEverywhereTyped(
         "ChunkStatusFix", chunkType, this.getOutputSchema().getType(References.CHUNK), input -> input.updateTyped(levelF, level -> {
            Dynamic<?> tag = (Dynamic<?>)level.get(DSL.remainderFinder());
            String status = tag.get("Status").asString("empty");
            if (Objects.equals(status, "postprocessed")) {
               tag = tag.set("Status", tag.createString("fullchunk"));
            }

            return level.set(DSL.remainderFinder(), tag);
         })
      );
   }
}
