package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

public record AmbientAdditionsSettings(Holder<SoundEvent> soundEvent, double tickChance) {
   public static final Codec<AmbientAdditionsSettings> CODEC = RecordCodecBuilder.create(
      p_451984_ -> p_451984_.group(
            SoundEvent.CODEC.fieldOf("sound").forGetter(p_459561_ -> p_459561_.soundEvent),
            Codec.DOUBLE.fieldOf("tick_chance").forGetter(p_450886_ -> p_450886_.tickChance)
         )
         .apply(p_451984_, AmbientAdditionsSettings::new)
   );
}
