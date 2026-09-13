package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class LodestoneCompassComponentFix extends DataComponentRemainderFix {
   public LodestoneCompassComponentFix(Schema p_332355_) {
      super(p_332355_, "LodestoneCompassComponentFix", "minecraft:lodestone_target", "minecraft:lodestone_tracker");
   }

   @Override
   protected <T> Dynamic<T> fixComponent(Dynamic<T> p_335889_) {
      Optional<Dynamic<T>> optional = p_335889_.get("pos").result();
      Optional<Dynamic<T>> optional1 = p_335889_.get("dimension").result();
      p_335889_ = p_335889_.remove("pos").remove("dimension");
      if (optional.isPresent() && optional1.isPresent()) {
         p_335889_ = p_335889_.set("target", p_335889_.emptyMap().set("pos", optional.get()).set("dimension", optional1.get()));
      }

      return p_335889_;
   }
}
