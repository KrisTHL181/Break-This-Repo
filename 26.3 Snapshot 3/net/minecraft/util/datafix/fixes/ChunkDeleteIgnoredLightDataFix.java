package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class ChunkDeleteIgnoredLightDataFix extends DataFix {
   public ChunkDeleteIgnoredLightDataFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> chunkType = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<?> sectionsFinder = chunkType.findField("sections");
      return this.fixTypeEverywhereTyped(
         "ChunkDeleteIgnoredLightDataFix",
         chunkType,
         chunk -> {
            boolean isLightOn = ((Dynamic)chunk.get(DSL.remainderFinder())).get("isLightOn").asBoolean(false);
            return !isLightOn
               ? chunk.updateTyped(sectionsFinder, section -> section.update(DSL.remainderFinder(), tag -> tag.remove("BlockLight").remove("SkyLight")))
               : chunk;
         }
      );
   }
}
