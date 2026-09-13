package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Instrument;

public interface InstrumentTags {
   TagKey<Instrument> REGULAR_GOAT_HORNS = create("regular_goat_horns");
   TagKey<Instrument> SCREAMING_GOAT_HORNS = create("screaming_goat_horns");
   TagKey<Instrument> GOAT_HORNS = create("goat_horns");

   private static TagKey<Instrument> create(String p_215861_) {
      return TagKey.create(Registries.INSTRUMENT, Identifier.withDefaultNamespace(p_215861_));
   }
}
