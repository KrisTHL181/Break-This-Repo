package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;

public class EntityShulkerRotationFix extends NamedEntityFix {
   public EntityShulkerRotationFix(final Schema outputSchema) {
      super(outputSchema, false, "EntityShulkerRotationFix", References.ENTITY, "minecraft:shulker");
   }

   public Dynamic<?> fixTag(final Dynamic<?> input) {
      List<Double> rotation = input.get("Rotation").asList(d -> d.asDouble(180.0));
      if (!rotation.isEmpty()) {
         rotation.set(0, rotation.get(0) - 180.0);
         return input.set("Rotation", input.createList(rotation.stream().map(input::createDouble)));
      } else {
         return input;
      }
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixTag);
   }
}
