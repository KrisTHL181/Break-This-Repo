package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class AreaEffectCloudPotionFix extends NamedEntityFix {
   public AreaEffectCloudPotionFix(final Schema outputSchema) {
      super(outputSchema, false, "AreaEffectCloudPotionFix", References.ENTITY, "minecraft:area_effect_cloud");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fix);
   }

   private <T> Dynamic<T> fix(Dynamic<T> entity) {
      Optional<Dynamic<T>> color = entity.get("Color").result();
      Optional<Dynamic<T>> effects = entity.get("effects").result();
      Optional<Dynamic<T>> potion = entity.get("Potion").result();
      entity = entity.remove("Color").remove("effects").remove("Potion");
      if (color.isEmpty() && effects.isEmpty() && potion.isEmpty()) {
         return entity;
      }

      Dynamic<T> potionContents = entity.emptyMap();
      if (color.isPresent()) {
         potionContents = potionContents.set("custom_color", color.get());
      }

      if (effects.isPresent()) {
         potionContents = potionContents.set("custom_effects", effects.get());
      }

      if (potion.isPresent()) {
         potionContents = potionContents.set("potion", potion.get());
      }

      return entity.set("potion_contents", potionContents);
   }
}
