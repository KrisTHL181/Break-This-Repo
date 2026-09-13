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

   public GoatHornIdFix(Schema p_216674_) {
      super(p_216674_, "GoatHornIdFix", p_216678_ -> p_216678_.equals("minecraft:goat_horn"));
   }

   @Override
   protected <T> Dynamic<T> fixItemStackTag(Dynamic<T> p_216676_) {
      int i = p_216676_.get("SoundVariant").asInt(0);
      String s = INSTRUMENTS[i >= 0 && i < INSTRUMENTS.length ? i : 0];
      return p_216676_.remove("SoundVariant").set("instrument", p_216676_.createString(s));
   }
}
