package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class GoatHornIdFix extends ItemStackTagRemainderFix {
   private static final String[] INSTRUMENTS = new String[]{
      "minecraft:ponder_goat_horn",
      "minecraft:sing_goat_horn",
      "minecraft:seek_goat_horn",
      "minecraft:feel_goat_horn",
      "minecraft:admire_goat_horn",
      "minecraft:call_goat_horn",
      "minecraft:yearn_goat_horn",
      "minecraft:dream_goat_horn"
   };

   public GoatHornIdFix(final Schema outputSchema) {
      super(outputSchema, "GoatHornIdFix", id -> id.equals("minecraft:goat_horn"));
   }

   @Override
   protected <T> Dynamic<T> fixItemStackTag(final Dynamic<T> tag) {
      int soundVariant = tag.get("SoundVariant").asInt(0);
      String soundId = INSTRUMENTS[soundVariant >= 0 && soundVariant < INSTRUMENTS.length ? soundVariant : 0];
      return tag.remove("SoundVariant").set("instrument", tag.createString(soundId));
   }
}
