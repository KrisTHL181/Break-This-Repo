package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class HorseBodyArmorItemFix extends NamedEntityWriteReadFix {
   private final String previousBodyArmorTag;
   private final boolean clearArmorItems;

   public HorseBodyArmorItemFix(final Schema outputSchema, final String entityName, final String previousBodyArmorTag, final boolean clearArmorItems) {
      super(outputSchema, true, "Horse armor fix for " + entityName, References.ENTITY, entityName);
      this.previousBodyArmorTag = previousBodyArmorTag;
      this.clearArmorItems = clearArmorItems;
   }

   @Override
   protected <T> Dynamic<T> fix(final Dynamic<T> input) {
      Optional<? extends Dynamic<?>> previousBodyArmor = input.get(this.previousBodyArmorTag).result();
      if (previousBodyArmor.isPresent()) {
         Dynamic<?> bodyArmorItem = (Dynamic<?>)previousBodyArmor.get();
         Dynamic<T> output = input.remove(this.previousBodyArmorTag);
         if (this.clearArmorItems) {
            output = output.update(
               "ArmorItems",
               armorItems -> armorItems.createList(Streams.mapWithIndex(armorItems.asStream(), (entry, index) -> index == 2L ? entry.emptyMap() : entry))
            );
            output = output.update(
               "ArmorDropChances",
               armorDropChances -> armorDropChances.createList(
                  Streams.mapWithIndex(armorDropChances.asStream(), (entry, index) -> index == 2L ? entry.createFloat(0.085F) : entry)
               )
            );
         }

         output = output.set("body_armor_item", bodyArmorItem);
         return output.set("body_armor_drop_chance", input.createFloat(2.0F));
      } else {
         return input;
      }
   }
}
