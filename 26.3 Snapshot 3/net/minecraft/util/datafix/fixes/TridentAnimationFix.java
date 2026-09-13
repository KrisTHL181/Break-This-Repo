package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import org.jspecify.annotations.Nullable;

public class TridentAnimationFix extends DataComponentRemainderFix {
   public TridentAnimationFix(final Schema outputSchema) {
      super(outputSchema, "TridentAnimationFix", "minecraft:consumable");
   }

   @Override
   protected <T> @Nullable Dynamic<T> fixComponent(final Dynamic<T> input) {
      return input.update("animation", animation -> {
         String optional = animation.asString().result().orElse("");
         return "spear".equals(optional) ? animation.createString("trident") : animation;
      });
   }
}
