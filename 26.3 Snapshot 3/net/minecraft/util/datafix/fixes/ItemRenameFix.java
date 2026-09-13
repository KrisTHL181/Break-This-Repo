package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class ItemRenameFix extends DataFix {
   private final String name;

   public ItemRenameFix(final Schema outputSchema, final String name) {
      super(outputSchema, false);
      this.name = name;
   }

   public TypeRewriteRule makeRule() {
      Type<Pair<String, String>> itemNameType = DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString());
      if (!Objects.equals(this.getInputSchema().getType(References.ITEM_NAME), itemNameType)) {
         throw new IllegalStateException("item name type is not what was expected.");
      } else {
         return this.fixTypeEverywhere(this.name, itemNameType, ops -> input -> input.mapSecond(this::fixItem));
      }
   }

   protected abstract String fixItem(final String item);

   public static DataFix create(final Schema outputSchema, final String name, final Function<String, String> fixItem) {
      return new ItemRenameFix(outputSchema, name) {
         @Override
         protected String fixItem(final String item) {
            return fixItem.apply(item);
         }
      };
   }
}
