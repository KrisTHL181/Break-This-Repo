package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class PlayerHeadBlockProfileFix extends NamedEntityFix {
   public PlayerHeadBlockProfileFix(final Schema outputSchema) {
      super(outputSchema, false, "PlayerHeadBlockProfileFix", References.BLOCK_ENTITY, "minecraft:skull");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fix);
   }

   private <T> Dynamic<T> fix(Dynamic<T> entity) {
      Optional<Dynamic<T>> skullOwner = entity.get("SkullOwner").result();
      Optional<Dynamic<T>> extraType = entity.get("ExtraType").result();
      Optional<Dynamic<T>> profile = skullOwner.or(() -> extraType);
      if (profile.isEmpty()) {
         return entity;
      }

      entity = entity.remove("SkullOwner").remove("ExtraType");
      return entity.set("profile", ItemStackComponentizationFix.fixProfile(profile.get()));
   }
}
