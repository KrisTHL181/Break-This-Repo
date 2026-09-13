package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class TippedArrowPotionToItemFix extends NamedEntityWriteReadFix {
   public TippedArrowPotionToItemFix(final Schema outputSchema) {
      super(outputSchema, false, "TippedArrowPotionToItemFix", References.ENTITY, "minecraft:arrow");
   }

   @Override
   protected <T> Dynamic<T> fix(final Dynamic<T> input) {
      Optional<Dynamic<T>> potion = input.get("Potion").result();
      Optional<Dynamic<T>> customPotionEffects = input.get("custom_potion_effects").result();
      Optional<Dynamic<T>> color = input.get("Color").result();
      return potion.isEmpty() && customPotionEffects.isEmpty() && color.isEmpty()
         ? input
         : input.remove("Potion").remove("custom_potion_effects").remove("Color").update("item", itemStack -> {
            Dynamic<?> tag = itemStack.get("tag").orElseEmptyMap();
            if (potion.isPresent()) {
               tag = tag.set("Potion", potion.get());
            }

            if (customPotionEffects.isPresent()) {
               tag = tag.set("custom_potion_effects", customPotionEffects.get());
            }

            if (color.isPresent()) {
               tag = tag.set("CustomPotionColor", color.get());
            }

            return itemStack.set("tag", tag);
         });
   }
}
