package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class PlayerHeadBlockProfileFix extends NamedEntityFix {
   public PlayerHeadBlockProfileFix(Schema p_334849_) {
      super(p_334849_, false, "PlayerHeadBlockProfileFix", References.BLOCK_ENTITY, "minecraft:skull");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_332910_) {
      return p_332910_.update(DSL.remainderFinder(), this::fix);
   }

   private <T> Dynamic<T> fix(Dynamic<T> p_332985_) {
      Optional<Dynamic<T>> optional = p_332985_.get("SkullOwner").result();
      Optional<Dynamic<T>> optional1 = p_332985_.get("ExtraType").result();
      Optional<Dynamic<T>> optional2 = optional.or(() -> optional1);
      if (optional2.isEmpty()) {
         return p_332985_;
      }

      p_332985_ = p_332985_.remove("SkullOwner").remove("ExtraType");
      return p_332985_.set("profile", ItemStackComponentizationFix.fixProfile(optional2.get()));
   }
}
