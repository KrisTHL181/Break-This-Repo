package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class AreaEffectCloudDurationScaleFix extends NamedEntityFix {
   public AreaEffectCloudDurationScaleFix(final Schema outputSchema) {
      super(outputSchema, false, "AreaEffectCloudDurationScaleFix", References.ENTITY, "minecraft:area_effect_cloud");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), tag -> tag.set("potion_duration_scale", tag.createFloat(0.25F)));
   }
}
