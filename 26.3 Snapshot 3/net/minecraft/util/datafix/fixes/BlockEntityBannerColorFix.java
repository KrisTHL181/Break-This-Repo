package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class BlockEntityBannerColorFix extends NamedEntityFix {
   public BlockEntityBannerColorFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "BlockEntityBannerColorFix", References.BLOCK_ENTITY, "minecraft:banner");
   }

   public Dynamic<?> fixTag(Dynamic<?> input) {
      input = input.update("Base", base -> base.createInt(15 - base.asInt(0)));
      return input.update(
         "Patterns",
         list -> (Dynamic)DataFixUtils.orElse(
            list.asStreamOpt()
               .map(stream -> stream.map(pattern -> pattern.update("Color", color -> color.createInt(15 - color.asInt(0)))))
               .map(list::createList)
               .result(),
            list
         )
      );
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixTag);
   }
}
