package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.Util;

public class PotDecorationsBlockEntityUnflatteningFix extends NamedEntityFix {
   public PotDecorationsBlockEntityUnflatteningFix(final Schema outputSchema) {
      super(outputSchema, true, "PotDecorationsBlockEntityUnflatteningFix", References.BLOCK_ENTITY, "minecraft:decorated_pot");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      Type<?> newType = (Type<?>)this.getOutputSchema().findChoiceType(References.BLOCK_ENTITY).types().get("minecraft:decorated_pot");
      return Util.writeAndReadTypedOrThrow(entity, newType, contents -> contents.update("sherds", PotDecorationsComponentUnflatteningFix::unpackList));
   }
}
