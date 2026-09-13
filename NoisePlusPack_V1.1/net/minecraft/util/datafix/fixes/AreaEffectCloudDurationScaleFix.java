package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class AreaEffectCloudDurationScaleFix extends NamedEntityFix {
   public AreaEffectCloudDurationScaleFix(Schema p_395420_) {
      super(p_395420_, false, "AreaEffectCloudDurationScaleFix", References.ENTITY, "minecraft:area_effect_cloud");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_394738_) {
      return p_394738_.update(DSL.remainderFinder(), p_393974_ -> p_393974_.set("potion_duration_scale", p_393974_.createFloat(0.25F)));
   }
}
