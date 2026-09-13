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
   public ItemLoreFix(Schema p_15958_) {
      super(p_15958_, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      Type<Pair<String, String>> type1 = this.getInputSchema().getType(References.TEXT_COMPONENT);
      OpticFinder<?> opticfinder = type.findField("tag");
      OpticFinder<?> opticfinder1 = opticfinder.type().findField("display");
      OpticFinder<?> opticfinder2 = opticfinder1.type().findField("Lore");
      OpticFinder<Pair<String, String>> opticfinder3 = DSL.typeFinder(type1);
      return this.fixTypeEverywhereTyped(
         "Item Lore componentize",
         type,
         p_390274_ -> p_390274_.updateTyped(
            opticfinder,
            p_390269_ -> p_390269_.updateTyped(
               opticfinder1,
               p_390277_ -> p_390277_.updateTyped(
                  opticfinder2,
                  p_390280_ -> p_390280_.update(opticfinder3, p_390278_ -> p_390278_.mapSecond(LegacyComponentDataFixUtils::createTextComponentJson))
               )
            )
         )
      );
   }
}
