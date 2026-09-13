package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;

public class ChunkDeleteLightFix extends DataFix {
   public ChunkDeleteLightFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> chunkType = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<?> sectionsFinder = chunkType.findField("sections");
      return this.fixTypeEverywhereTyped("ChunkDeleteLightFix for " + this.getOutputSchema().getVersionKey(), chunkType, chunk -> {
         chunk = chunk.update(DSL.remainderFinder(), tag -> tag.remove("isLightOn"));
         return chunk.updateTyped(sectionsFinder, section -> section.update(DSL.remainderFinder(), tag -> tag.remove("BlockLight").remove("SkyLight")));
      });
   }
}
