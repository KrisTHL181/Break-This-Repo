package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class AbstractPoiSectionFix extends DataFix {
   private final String name;

   public AbstractPoiSectionFix(final Schema outputSchema, final String name) {
      super(outputSchema, false);
      this.name = name;
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, Dynamic<?>>> poiChunkType = DSL.named(References.POI_CHUNK.typeName(), DSL.remainderType());
      if (!Objects.equals(poiChunkType, this.getInputSchema().getType(References.POI_CHUNK))) {
         throw new IllegalStateException("Poi type is not what was expected.");
      } else {
         return this.fixTypeEverywhere(this.name, poiChunkType, ops -> input -> input.mapSecond(this::cap));
      }
   }

   private <T> Dynamic<T> cap(final Dynamic<T> input) {
      return input.update("Sections", sections -> sections.updateMapValues(entry -> entry.mapSecond(this::processSection)));
   }

   private Dynamic<?> processSection(final Dynamic<?> section) {
      return section.update("Records", this::processSectionRecords);
   }

   private <T> Dynamic<T> processSectionRecords(final Dynamic<T> input) {
      return (Dynamic<T>)DataFixUtils.orElse(
         input.asStreamOpt().result().map(stream -> input.createList(this.processRecords((Stream<Dynamic<T>>)stream))), input
      );
   }

   protected abstract <T> Stream<Dynamic<T>> processRecords(Stream<Dynamic<T>> records);
}
