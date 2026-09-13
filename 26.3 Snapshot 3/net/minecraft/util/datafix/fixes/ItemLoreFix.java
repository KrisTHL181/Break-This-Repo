package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class ItemLoreFix extends DataFix {
   public ItemLoreFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      Type<Pair<String, String>> textComponentType = this.getInputSchema().getType(References.TEXT_COMPONENT);
      OpticFinder<?> tagFinder = itemStackType.findField("tag");
      OpticFinder<?> displayFinder = tagFinder.type().findField("display");
      OpticFinder<?> loreFinder = displayFinder.type().findField("Lore");
      OpticFinder<Pair<String, String>> textComponentFinder = DSL.typeFinder(textComponentType);
      return this.fixTypeEverywhereTyped(
         "Item Lore componentize",
         itemStackType,
         itemStack -> itemStack.updateTyped(
            tagFinder,
            tag -> tag.updateTyped(
               displayFinder,
               display -> display.updateTyped(
                  loreFinder,
                  lore -> lore.update(textComponentFinder, textComponent -> textComponent.mapSecond(LegacyComponentDataFixUtils::createTextComponentJson))
               )
            )
         )
      );
   }
}
