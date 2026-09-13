package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemapChunkStatusFix extends DataFix {
   private final String name;
   private final UnaryOperator<String> mapper;

   public RemapChunkStatusFix(final Schema schema, final String name, final UnaryOperator<String> mapper) {
      super(schema, false);
      this.name = name;
      this.mapper = mapper;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.name,
         this.getInputSchema().getType(References.CHUNK),
         input -> input.update(
            DSL.remainderFinder(),
            data -> data.update("Status", this::fixStatus)
               .update("below_zero_retrogen", belowZeroRetrogen -> belowZeroRetrogen.update("target_status", this::fixStatus))
         )
      );
   }

   private <T> Dynamic<T> fixStatus(final Dynamic<T> dynamic) {
      Optional<Dynamic<T>> remapped = dynamic.asString().result().map(NamespacedSchema::ensureNamespaced).map(this.mapper).map(dynamic::createString);
      return (Dynamic<T>)DataFixUtils.orElse(remapped, dynamic);
   }
}
