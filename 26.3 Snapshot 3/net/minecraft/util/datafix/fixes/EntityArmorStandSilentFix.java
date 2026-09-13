package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntityArmorStandSilentFix extends NamedEntityFix {
   public EntityArmorStandSilentFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "EntityArmorStandSilentFix", References.ENTITY, "ArmorStand");
   }

   public Dynamic<?> fixTag(final Dynamic<?> input) {
      return input.get("Silent").asBoolean(false) && !input.get("Marker").asBoolean(false) ? input.remove("Silent") : input;
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixTag);
   }
}
