package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.stream.Stream;

public class FoodToConsumableFix extends DataFix {
   public FoodToConsumableFix(Schema p_365602_) {
      super(p_365602_, true);
   }

   protected TypeRewriteRule makeRule() {
      return this.writeFixAndRead(
         "Food to consumable fix",
         this.getInputSchema().getType(References.DATA_COMPONENTS),
         this.getOutputSchema().getType(References.DATA_COMPONENTS),
         p_363478_ -> {
            Optional<? extends Dynamic<?>> optional = p_363478_.get("minecraft:food").result();
            if (optional.isPresent()) {
               float f = optional.get().get("eat_seconds").asFloat(1.6F);
               Stream<? extends Dynamic<?>> stream = optional.get().get("effects").asStream();
               Stream<? extends Dynamic<?>> stream1 = stream.map(
                  p_369531_ -> p_369531_.emptyMap()
                     .set("type", p_369531_.createString("minecraft:apply_effects"))
                     .set("effects", p_369531_.createList(p_369531_.get("effect").result().stream()))
                     .set("probability", p_369531_.createFloat(p_369531_.get("probability").asFloat(1.0F)))
               );
               p_363478_ = Dynamic.copyField(optional.get(), "using_converts_to", p_363478_, "minecraft:use_remainder");
               p_363478_ = p_363478_.set("minecraft:food", optional.get().remove("eat_seconds").remove("effects").remove("using_converts_to"));
               return p_363478_.set(
                  "minecraft:consumable",
                  p_363478_.emptyMap().set("consume_seconds", p_363478_.createFloat(f)).set("on_consume_effects", p_363478_.createList(stream1))
               );
            } else {
               return p_363478_;
            }
         }
      );
   }
}
