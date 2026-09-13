package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class AbstractBlockPropertyFix extends DataFix {
   private final String name;

   public AbstractBlockPropertyFix(final Schema outputSchema, final String name) {
      super(outputSchema, false);
      this.name = name;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.name, this.getInputSchema().getType(References.BLOCK_STATE), input -> input.update(DSL.remainderFinder(), this::fixBlockState)
      );
   }

   private Dynamic<?> fixBlockState(final Dynamic<?> tag) {
      Optional<String> blockId = tag.get("Name").asString().result().map(NamespacedSchema::ensureNamespaced);
      return blockId.isPresent() && this.shouldFix(blockId.get()) ? tag.update("Properties", properties -> this.fixProperties(blockId.get(), properties)) : tag;
   }

   protected abstract boolean shouldFix(String blockId);

   protected abstract <T> Dynamic<T> fixProperties(String blockId, Dynamic<T> properties);
}
