package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class LodestoneCompassComponentFix extends DataComponentRemainderFix {
   public LodestoneCompassComponentFix(final Schema outputSchema) {
      super(outputSchema, "LodestoneCompassComponentFix", "minecraft:lodestone_target", "minecraft:lodestone_tracker");
   }

   @Override
   protected <T> Dynamic<T> fixComponent(Dynamic<T> input) {
      Optional<Dynamic<T>> pos = input.get("pos").result();
      Optional<Dynamic<T>> dimension = input.get("dimension").result();
      input = input.remove("pos").remove("dimension");
      if (pos.isPresent() && dimension.isPresent()) {
         input = input.set("target", input.emptyMap().set("pos", pos.get()).set("dimension", dimension.get()));
      }

      return input;
   }
}
