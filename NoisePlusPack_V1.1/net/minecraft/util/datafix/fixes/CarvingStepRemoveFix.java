package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class CarvingStepRemoveFix extends DataFix {
   public CarvingStepRemoveFix(Schema p_361259_) {
      super(p_361259_, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped("CarvingStepRemoveFix", this.getInputSchema().getType(References.CHUNK), CarvingStepRemoveFix::fixChunk);
   }

   private static Typed<?> fixChunk(Typed<?> p_362518_) {
      return p_362518_.update(DSL.remainderFinder(), p_363979_ -> {
         Dynamic<?> dynamic = p_363979_;
         Optional<? extends Dynamic<?>> optional = p_363979_.get("CarvingMasks").result();
         if (optional.isPresent()) {
            Optional<? extends Dynamic<?>> optional1 = optional.get().get("AIR").result();
            if (optional1.isPresent()) {
               dynamic = p_363979_.set("carving_mask", optional1.get());
            }
         }

         return dynamic.remove("CarvingMasks");
      });
   }
}
