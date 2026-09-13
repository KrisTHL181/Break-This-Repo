package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class TippedArrowPotionToItemFix extends NamedEntityWriteReadFix {
   public TippedArrowPotionToItemFix(Schema p_332998_) {
      super(p_332998_, false, "TippedArrowPotionToItemFix", References.ENTITY, "minecraft:arrow");
   }

   @Override
   protected <T> Dynamic<T> fix(Dynamic<T> p_332038_) {
      Optional<Dynamic<T>> optional = p_332038_.get("Potion").result();
      Optional<Dynamic<T>> optional1 = p_332038_.get("custom_potion_effects").result();
      Optional<Dynamic<T>> optional2 = p_332038_.get("Color").result();
      return optional.isEmpty() && optional1.isEmpty() && optional2.isEmpty()
         ? p_332038_
         : p_332038_.remove("Potion").remove("custom_potion_effects").remove("Color").update("item", p_333381_ -> {
            Dynamic<?> dynamic = p_333381_.get("tag").orElseEmptyMap();
            if (optional.isPresent()) {
               dynamic = dynamic.set("Potion", optional.get());
            }

            if (optional1.isPresent()) {
               dynamic = dynamic.set("custom_potion_effects", optional1.get());
            }

            if (optional2.isPresent()) {
               dynamic = dynamic.set("CustomPotionColor", optional2.get());
            }

            return p_333381_.set("tag", dynamic);
         });
   }
}
