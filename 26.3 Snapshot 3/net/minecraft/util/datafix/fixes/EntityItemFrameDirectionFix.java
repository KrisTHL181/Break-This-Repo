package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntityItemFrameDirectionFix extends NamedEntityFix {
   public EntityItemFrameDirectionFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "EntityItemFrameDirectionFix", References.ENTITY, "minecraft:item_frame");
   }

   public Dynamic<?> fixTag(final Dynamic<?> input) {
      return input.set("Facing", input.createByte(direction2dTo3d(input.get("Facing").asByte((byte)0))));
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixTag);
   }

   private static byte direction2dTo3d(final byte dir) {
      switch (dir) {
         case 0:
            return 3;
         case 1:
            return 4;
         case 2:
         default:
            return 2;
         case 3:
            return 5;
      }
   }
}
