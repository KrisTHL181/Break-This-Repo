package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class CatTypeFix extends NamedEntityFix {
   public CatTypeFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "CatTypeFix", References.ENTITY, "minecraft:cat");
   }

   public Dynamic<?> fixTag(final Dynamic<?> input) {
      return input.get("CatType").asInt(0) == 9 ? input.set("CatType", input.createInt(10)) : input;
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixTag);
   }
}
