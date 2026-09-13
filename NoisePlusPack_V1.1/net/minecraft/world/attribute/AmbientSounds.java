package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;

public record AmbientSounds(Optional<Holder<SoundEvent>> loop, Optional<AmbientMoodSettings> mood, List<AmbientAdditionsSettings> additions) {
   public static final AmbientSounds EMPTY = new AmbientSounds(Optional.empty(), Optional.empty(), List.of());
   public static final AmbientSounds LEGACY_CAVE_SETTINGS = new AmbientSounds(
      Optional.empty(), Optional.of(AmbientMoodSettings.LEGACY_CAVE_SETTINGS), List.of()
   );
   public static final Codec<AmbientSounds> CODEC = RecordCodecBuilder.create(
      p_458586_ -> p_458586_.group(
            SoundEvent.CODEC.optionalFieldOf("loop").forGetter(AmbientSounds::loop),
            AmbientMoodSettings.CODEC.optionalFieldOf("mood").forGetter(AmbientSounds::mood),
            ExtraCodecs.compactListCodec(AmbientAdditionsSettings.CODEC).optionalFieldOf("additions", List.of()).forGetter(AmbientSounds::additions)
         )
         .apply(p_458586_, AmbientSounds::new)
   );
}
