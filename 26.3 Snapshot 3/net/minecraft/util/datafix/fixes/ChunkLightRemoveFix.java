package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;

public class ChunkLightRemoveFix extends DataFix {
   public ChunkLightRemoveFix(final Schema schema, final boolean changesType) {
      super(schema, changesType);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> chunkType = this.getInputSchema().getType(References.CHUNK);
      Type<?> levelType = chunkType.findFieldType("Level");
      OpticFinder<?> levelF = DSL.fieldFinder("Level", levelType);
      return this.fixTypeEverywhereTyped(
         "ChunkLightRemoveFix",
         chunkType,
         this.getOutputSchema().getType(References.CHUNK),
         input -> input.updateTyped(levelF, level -> level.update(DSL.remainderFinder(), tag -> tag.remove("isLightOn")))
      );
   }
}
