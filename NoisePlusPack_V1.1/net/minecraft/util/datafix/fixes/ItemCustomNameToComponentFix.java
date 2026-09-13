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
   public ItemCustomNameToComponentFix(Schema p_15927_) {
      super(p_15927_, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      Type<Pair<String, String>> type1 = this.getInputSchema().getType(References.TEXT_COMPONENT);
      OpticFinder<?> opticfinder = type.findField("tag");
      OpticFinder<?> opticfinder1 = opticfinder.type().findField("display");
      OpticFinder<?> opticfinder2 = opticfinder1.type().findField("Name");
      OpticFinder<Pair<String, String>> opticfinder3 = DSL.typeFinder(type1);
      return this.fixTypeEverywhereTyped(
         "ItemCustomNameToComponentFix",
         type,
         p_390264_ -> p_390264_.updateTyped(
            opticfinder,
            p_390259_ -> p_390259_.updateTyped(
               opticfinder1,
               p_15931_ -> p_15931_.updateTyped(
                  opticfinder2,
                  p_390255_ -> p_390255_.update(opticfinder3, p_390265_ -> p_390265_.mapSecond(LegacyComponentDataFixUtils::createTextComponentJson))
               )
            )
         )
      );
   }
}
