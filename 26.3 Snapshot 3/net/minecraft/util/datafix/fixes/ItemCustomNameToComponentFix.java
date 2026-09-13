package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class ItemCustomNameToComponentFix extends DataFix {
   public ItemCustomNameToComponentFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      Type<Pair<String, String>> textComponentType = this.getInputSchema().getType(References.TEXT_COMPONENT);
      OpticFinder<?> tagFinder = itemStackType.findField("tag");
      OpticFinder<?> displayFinder = tagFinder.type().findField("display");
      OpticFinder<?> customNameFinder = displayFinder.type().findField("Name");
      OpticFinder<Pair<String, String>> textComponentFinder = DSL.typeFinder(textComponentType);
      return this.fixTypeEverywhereTyped(
         "ItemCustomNameToComponentFix",
         itemStackType,
         itemStack -> itemStack.updateTyped(
            tagFinder,
            tag -> tag.updateTyped(
               displayFinder,
               display -> display.updateTyped(
                  customNameFinder,
                  customName -> customName.update(
                     textComponentFinder, textComponent -> textComponent.mapSecond(LegacyComponentDataFixUtils::createTextComponentJson)
                  )
               )
            )
         )
      );
   }
}
