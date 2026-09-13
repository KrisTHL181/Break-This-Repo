package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class WrittenBookPagesStrictJsonFix extends ItemStackTagFix {
   public WrittenBookPagesStrictJsonFix(Schema p_407058_) {
      super(p_407058_, "WrittenBookPagesStrictJsonFix", p_406989_ -> p_406989_.equals("minecraft:written_book"));
   }

   @Override
   protected Typed<?> fixItemStackTag(Typed<?> p_408117_) {
      Type<Pair<String, String>> type = this.getInputSchema().getType(References.TEXT_COMPONENT);
      Type<?> type1 = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> opticfinder = type1.findField("tag");
      OpticFinder<?> opticfinder1 = opticfinder.type().findField("pages");
      OpticFinder<Pair<String, String>> opticfinder2 = DSL.typeFinder(type);
      return p_408117_.updateTyped(
         opticfinder1, p_409226_ -> p_409226_.update(opticfinder2, p_409536_ -> p_409536_.mapSecond(LegacyComponentDataFixUtils::rewriteFromLenient))
      );
   }
}
