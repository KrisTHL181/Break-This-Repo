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
      i -> i.group(
            Music.CODEC.optionalFieldOf("default").forGetter(BackgroundMusic::defaultMusic),
            Music.CODEC.optionalFieldOf("creative").forGetter(BackgroundMusic::creativeMusic),
            Music.CODEC.optionalFieldOf("underwater").forGetter(BackgroundMusic::underwaterMusic)
         )
         .apply(i, BackgroundMusic::new)
   );

   public BackgroundMusic(final Music music) {
      this(Optional.of(music), Optional.empty(), Optional.empty());
   }

   public BackgroundMusic(final Holder<SoundEvent> sound) {
      this(Musics.createGameMusic(sound));
   }

   public BackgroundMusic withUnderwater(final Music underwaterMusic) {
      return new BackgroundMusic(this.defaultMusic, this.creativeMusic, Optional.of(underwaterMusic));
   }

   public Optional<Music> select(final boolean isCreative, final boolean isUnderwater) {
      if (isUnderwater && this.underwaterMusic.isPresent()) {
         return this.underwaterMusic;
      } else {
         return isCreative && this.creativeMusic.isPresent() ? this.creativeMusic : this.defaultMusic;
      }
   }
}
