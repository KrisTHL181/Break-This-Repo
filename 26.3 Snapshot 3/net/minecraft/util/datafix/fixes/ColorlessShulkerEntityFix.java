package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class ColorlessShulkerEntityFix extends NamedEntityFix {
   public ColorlessShulkerEntityFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "Colorless shulker entity fix", References.ENTITY, "minecraft:shulker");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), tag -> tag.get("Color").asInt(0) == 10 ? tag.set("Color", tag.createByte((byte)16)) : tag);
   }
}
