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

   public AbstractPoiSectionFix(Schema p_216536_, String p_216537_) {
      super(p_216536_, false);
      this.name = p_216537_;
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, Dynamic<?>>> type = DSL.named(References.POI_CHUNK.typeName(), DSL.remainderType());
      if (!Objects.equals(type, this.getInputSchema().getType(References.POI_CHUNK))) {
         throw new IllegalStateException("Poi type is not what was expected.");
      } else {
         return this.fixTypeEverywhere(this.name, type, p_216546_ -> p_216549_ -> p_216549_.mapSecond(this::cap));
      }
   }

   private <T> Dynamic<T> cap(Dynamic<T> p_216541_) {
      return p_216541_.update("Sections", p_216555_ -> p_216555_.updateMapValues(p_216539_ -> p_216539_.mapSecond(this::processSection)));
   }

   private Dynamic<?> processSection(Dynamic<?> p_216551_) {
      return p_216551_.update("Records", this::processSectionRecords);
   }

   private <T> Dynamic<T> processSectionRecords(Dynamic<T> p_216553_) {
      return (Dynamic<T>)DataFixUtils.orElse(
         p_216553_.asStreamOpt().result().map(p_216544_ -> p_216553_.createList(this.processRecords((Stream<Dynamic<T>>)p_216544_))), p_216553_
      );
   }

   protected abstract <T> Stream<Dynamic<T>> processRecords(Stream<Dynamic<T>> var1);
}
