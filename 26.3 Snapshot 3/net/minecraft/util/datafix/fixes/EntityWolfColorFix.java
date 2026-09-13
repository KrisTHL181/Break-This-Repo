package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntityWolfColorFix extends NamedEntityFix {
   public EntityWolfColorFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "EntityWolfColorFix", References.ENTITY, "minecraft:wolf");
   }

   public Dynamic<?> fixTag(final Dynamic<?> input) {
      return input.update("CollarColor", color -> color.createByte((byte)(15 - color.asInt(0))));
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixTag);
   }
}
