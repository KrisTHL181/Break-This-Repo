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
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemWaterPotionFix extends DataFix {
   public ItemWaterPotionFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> idF = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<?> tagF = itemStackType.findField("tag");
      return this.fixTypeEverywhereTyped(
         "ItemWaterPotionFix",
         itemStackType,
         input -> {
            Optional<Pair<String, String>> idOpt = input.getOptional(idF);
            if (idOpt.isPresent()) {
               String id = (String)idOpt.get().getSecond();
               if ("minecraft:potion".equals(id)
                  || "minecraft:splash_potion".equals(id)
                  || "minecraft:lingering_potion".equals(id)
                  || "minecraft:tipped_arrow".equals(id)) {
                  Typed<?> tag = input.getOrCreateTyped(tagF);
                  Dynamic<?> tagRest = (Dynamic<?>)tag.get(DSL.remainderFinder());
                  if (tagRest.get("Potion").asString().result().isEmpty()) {
                     tagRest = tagRest.set("Potion", tagRest.createString("minecraft:water"));
                  }

                  return input.set(tagF, tag.set(DSL.remainderFinder(), tagRest));
               }
            }

            return input;
         }
      );
   }
}
