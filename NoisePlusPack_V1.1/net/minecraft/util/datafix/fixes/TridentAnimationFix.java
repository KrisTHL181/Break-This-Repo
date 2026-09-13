package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import org.jspecify.annotations.Nullable;

public class TridentAnimationFix extends DataComponentRemainderFix {
   public TridentAnimationFix(Schema p_451906_) {
      super(p_451906_, "TridentAnimationFix", "minecraft:consumable");
   }

   @Override
   protected <T> @Nullable Dynamic<T> fixComponent(Dynamic<T> p_450456_) {
      return p_450456_.update("animation", p_460201_ -> {
         String s = p_460201_.asString().result().orElse("");
         return "spear".equals(s) ? p_460201_.createString("trident") : p_460201_;
      });
   }
}
