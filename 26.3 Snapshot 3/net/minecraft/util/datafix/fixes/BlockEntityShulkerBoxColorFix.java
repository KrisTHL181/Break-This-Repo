package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class BlockEntityShulkerBoxColorFix extends NamedEntityFix {
   public BlockEntityShulkerBoxColorFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "BlockEntityShulkerBoxColorFix", References.BLOCK_ENTITY, "minecraft:shulker_box");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), tag -> tag.remove("Color"));
   }
}
