package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntityShulkerColorFix extends NamedEntityFix {
   public EntityShulkerColorFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "EntityShulkerColorFix", References.ENTITY, "minecraft:shulker");
   }

   public Dynamic<?> fixTag(final Dynamic<?> input) {
      return input.get("Color").map(Dynamic::asNumber).result().isEmpty() ? input.set("Color", input.createByte((byte)10)) : input;
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixTag);
   }
}
