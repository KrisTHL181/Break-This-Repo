package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RenameEnchantmentsFix extends DataFix {
   private final String name;
   private final Map<String, String> renames;

   public RenameEnchantmentsFix(final Schema outputSchema, final String name, final Map<String, String> renames) {
      super(outputSchema, false);
      this.name = name;
      this.renames = renames;
   }

   protected TypeRewriteRule makeRule() {
      Type<?> item = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> tagFinder = item.findField("tag");
      return this.fixTypeEverywhereTyped(this.name, item, input -> input.updateTyped(tagFinder, tag -> tag.update(DSL.remainderFinder(), this::fixTag)));
   }

   private Dynamic<?> fixTag(Dynamic<?> tag) {
      tag = this.fixEnchantmentList(tag, "Enchantments");
      return this.fixEnchantmentList(tag, "StoredEnchantments");
   }

   private Dynamic<?> fixEnchantmentList(final Dynamic<?> itemStack, final String field) {
      return itemStack.update(
         field,
         tag -> (Dynamic)tag.asStreamOpt()
            .map(
               s -> s.map(
                  element -> element.update(
                     "id",
                     id -> (Dynamic)id.asString()
                        .map(stringId -> element.createString(this.renames.getOrDefault(NamespacedSchema.ensureNamespaced(stringId), stringId)))
                        .mapOrElse(Function.identity(), fail -> id)
                  )
               )
            )
            .map(tag::createList)
            .mapOrElse(Function.identity(), fail -> tag)
      );
   }
}
