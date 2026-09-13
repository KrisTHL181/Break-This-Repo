package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;

public class EntityFallDistanceFloatToDoubleFix extends DataFix {
   private final TypeReference type;

   public EntityFallDistanceFloatToDoubleFix(Schema p_393177_, TypeReference p_396299_) {
      super(p_393177_, false);
      this.type = p_396299_;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "EntityFallDistanceFloatToDoubleFixFor" + this.type.typeName(),
         this.getOutputSchema().getType(this.type),
         EntityFallDistanceFloatToDoubleFix::fixEntity
      );
   }

   private static Typed<?> fixEntity(Typed<?> p_393056_) {
      return p_393056_.update(
         DSL.remainderFinder(),
         p_392978_ -> p_392978_.renameAndFixField("FallDistance", "fall_distance", p_393317_ -> p_393317_.createDouble(p_393317_.asFloat(0.0F)))
      );
   }
}
