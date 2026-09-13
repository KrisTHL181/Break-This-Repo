package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public record AmbientMoodSettings(Holder<SoundEvent> soundEvent, int tickDelay, int blockSearchExtent, double soundPositionOffset) {
   public static final Codec<AmbientMoodSettings> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            SoundEvent.CODEC.fieldOf("sound").forGetter(s -> s.soundEvent),
            Codec.INT.fieldOf("tick_delay").forGetter(s -> s.tickDelay),
            Codec.INT.fieldOf("block_search_extent").forGetter(s -> s.blockSearchExtent),
            Codec.DOUBLE.fieldOf("offset").forGetter(s -> s.soundPositionOffset)
         )
         .apply(i, AmbientMoodSettings::new)
   );
   public static final AmbientMoodSettings LEGACY_CAVE_SETTINGS = new AmbientMoodSettings(SoundEvents.AMBIENT_CAVE, 6000, 8, 2.0);
}
