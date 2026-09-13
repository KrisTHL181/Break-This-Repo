package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvent;

public record BackgroundMusic(Optional<Music> defaultMusic, Optional<Music> creativeMusic, Optional<Music> underwaterMusic) {
   public static final BackgroundMusic EMPTY = new BackgroundMusic(Optional.empty(), Optional.empty(), Optional.empty());
   public static final BackgroundMusic OVERWORLD = new BackgroundMusic(Optional.of(Musics.GAME), Optional.of(Musics.CREATIVE), Optional.empty());
   public static final Codec<BackgroundMusic> CODEC = RecordCodecBuilder.create(
      p_452257_ -> p_452257_.group(
            Music.CODEC.optionalFieldOf("default").forGetter(BackgroundMusic::defaultMusic),
            Music.CODEC.optionalFieldOf("creative").forGetter(BackgroundMusic::creativeMusic),
            Music.CODEC.optionalFieldOf("underwater").forGetter(BackgroundMusic::underwaterMusic)
         )
         .apply(p_452257_, BackgroundMusic::new)
   );

   public BackgroundMusic(Music p_450427_) {
      this(Optional.of(p_450427_), Optional.empty(), Optional.empty());
   }

   public BackgroundMusic(Holder<SoundEvent> p_460323_) {
      this(Musics.createGameMusic(p_460323_));
   }

   public BackgroundMusic withUnderwater(Music p_455142_) {
      return new BackgroundMusic(this.defaultMusic, this.creativeMusic, Optional.of(p_455142_));
   }

   public Optional<Music> select(boolean p_455709_, boolean p_455242_) {
      if (p_455242_ && this.underwaterMusic.isPresent()) {
         return this.underwaterMusic;
      } else {
         return p_455709_ && this.creativeMusic.isPresent() ? this.creativeMusic : this.defaultMusic;
      }
   }
}
