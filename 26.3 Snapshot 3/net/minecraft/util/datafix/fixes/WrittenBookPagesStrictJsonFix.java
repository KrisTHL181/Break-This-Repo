package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class WrittenBookPagesStrictJsonFix extends ItemStackTagFix {
   public WrittenBookPagesStrictJsonFix(final Schema outputSchema) {
      super(outputSchema, "WrittenBookPagesStrictJsonFix", id -> id.equals("minecraft:written_book"));
   }

   @Override
   protected Typed<?> fixItemStackTag(final Typed<?> tag) {
      Type<Pair<String, String>> textComponentType = this.getInputSchema().getType(References.TEXT_COMPONENT);
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> tagF = itemStackType.findField("tag");
      OpticFinder<?> pagesF = tagF.type().findField("pages");
      OpticFinder<Pair<String, String>> pageF = DSL.typeFinder(textComponentType);
      return tag.updateTyped(pagesF, pages -> pages.update(pageF, page -> page.mapSecond(LegacyComponentDataFixUtils::rewriteFromLenient)));
   }
}
