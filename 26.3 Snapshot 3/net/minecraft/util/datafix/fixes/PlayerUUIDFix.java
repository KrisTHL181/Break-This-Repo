package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class PlayerUUIDFix extends AbstractUUIDFix {
   public PlayerUUIDFix(final Schema outputSchema) {
      super(outputSchema, References.PLAYER);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "PlayerUUIDFix",
         this.getInputSchema().getType(this.typeReference),
         input -> {
            OpticFinder<?> rootVehicleFinder = input.getType().findField("RootVehicle");
            return input.updateTyped(
                  rootVehicleFinder,
                  rootVehicleFinder.type(),
                  rootVehicle -> rootVehicle.update(DSL.remainderFinder(), tag -> replaceUUIDLeastMost(tag, "Attach", "Attach").orElse(tag))
               )
               .update(DSL.remainderFinder(), tag -> EntityUUIDFix.updateEntityUUID(EntityUUIDFix.updateLivingEntity(tag)));
         }
      );
   }
}
