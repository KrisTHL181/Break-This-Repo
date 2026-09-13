package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackMapIdFix extends DataFix {
   public ItemStackMapIdFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> idF = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> tagF = itemStackType.findField("tag");
      return this.fixTypeEverywhereTyped("ItemInstanceMapIdFix", itemStackType, input -> {
         Optional<Pair<String, String>> id = input.getOptional(idF);
         if (id.isPresent() && Objects.equals(id.get().getSecond(), "minecraft:filled_map")) {
            Dynamic<?> rest = (Dynamic<?>)input.get(DSL.remainderFinder());
            Typed<?> tag = input.getOrCreateTyped(tagF);
            Dynamic<?> tagRest = (Dynamic<?>)tag.get(DSL.remainderFinder());
            tagRest = tagRest.set("map", tagRest.createInt(rest.get("Damage").asInt(0)));
            return input.set(tagF, tag.set(DSL.remainderFinder(), tagRest));
         } else {
            return input;
         }
      });
   }
}
