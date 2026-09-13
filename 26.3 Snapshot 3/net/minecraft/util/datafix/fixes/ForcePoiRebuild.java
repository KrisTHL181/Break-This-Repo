package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;

public class ForcePoiRebuild extends DataFix {
   public ForcePoiRebuild(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, Dynamic<?>>> poiChunkType = DSL.named(References.POI_CHUNK.typeName(), DSL.remainderType());
      if (!Objects.equals(poiChunkType, this.getInputSchema().getType(References.POI_CHUNK))) {
         throw new IllegalStateException("Poi type is not what was expected.");
      } else {
         return this.fixTypeEverywhere("POI rebuild", poiChunkType, ops -> input -> input.mapSecond(ForcePoiRebuild::cap));
      }
   }

   private static <T> Dynamic<T> cap(final Dynamic<T> input) {
      return input.update("Sections", sections -> sections.updateMapValues(entry -> entry.mapSecond(section -> section.remove("Valid"))));
   }
}
