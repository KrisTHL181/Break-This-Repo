package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public record AmbientMoodSettings(Holder<SoundEvent> soundEvent, int tickDelay, int blockSearchExtent, double soundPositionOffset) {
   public static final Codec<AmbientMoodSettings> CODEC = RecordCodecBuilder.create(
      p_457613_ -> p_457613_.group(
            SoundEvent.CODEC.fieldOf("sound").forGetter(p_453675_ -> p_453675_.soundEvent),
            Codec.INT.fieldOf("tick_delay").forGetter(p_458025_ -> p_458025_.tickDelay),
            Codec.INT.fieldOf("block_search_extent").forGetter(p_460320_ -> p_460320_.blockSearchExtent),
            Codec.DOUBLE.fieldOf("offset").forGetter(p_454154_ -> p_454154_.soundPositionOffset)
         )
         .apply(p_457613_, AmbientMoodSettings::new)
   );
   public static final AmbientMoodSettings LEGACY_CAVE_SETTINGS = new AmbientMoodSettings(SoundEvents.AMBIENT_CAVE, 6000, 8, 2.0);
}
