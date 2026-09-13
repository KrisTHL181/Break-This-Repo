package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;

public class BlendingDataRemoveFromNetherEndFix extends DataFix {
   public BlendingDataRemoveFromNetherEndFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> chunkType = this.getOutputSchema().getType(References.CHUNK);
      return this.fixTypeEverywhereTyped(
         "BlendingDataRemoveFromNetherEndFix",
         chunkType,
         chunk -> chunk.update(DSL.remainderFinder(), chunkTag -> updateChunkTag(chunkTag, chunkTag.get("__context")))
      );
   }

   private static Dynamic<?> updateChunkTag(final Dynamic<?> chunkTag, final OptionalDynamic<?> contextTag) {
      boolean isOverworld = "minecraft:overworld".equals(contextTag.get("dimension").asString().result().orElse(""));
      return isOverworld ? chunkTag : chunkTag.remove("blending_data");
   }
}
