package net.minecraft.client.sounds;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

public class MusicManager {
   private static final int STARTING_DELAY = 100;
   private final RandomSource random = RandomSource.create();
   private final Minecraft minecraft;
   private @Nullable SoundInstance currentMusic;
   private MusicManager.MusicFrequency gameMusicFrequency;
   private float currentGain = 1.0F;
   private int nextSongDelay = 100;
   private boolean toastShown = false;

   public MusicManager(final Minecraft minecraft) {
      this.minecraft = minecraft;
      this.gameMusicFrequency = minecraft.options.musicFrequency().get();
   }

   public void tick() {
      float volume = this.minecraft.getMusicVolume();
      if (this.currentMusic != null && this.currentGain != volume) {
         boolean stillPlaying = this.fadePlaying(volume);
         if (!stillPlaying) {
            return;
         }
      }

      Music music = this.minecraft.getSituationalMusic();
      if (music == null) {
         this.nextSongDelay = Math.max(this.nextSongDelay, 100);
      } else {
         if (this.currentMusic != null) {
            if (canReplace(music, this.currentMusic)) {
               this.minecraft.getSoundManager().stop(this.currentMusic);
               this.nextSongDelay = Mth.nextInt(this.random, 0, music.minDelay() / 2);
            }

            if (!this.minecraft.getSoundManager().isActive(this.currentMusic)) {
               this.currentMusic = null;
               this.nextSongDelay = Math.min(this.nextSongDelay, this.gameMusicFrequency.getNextSongDelay(music, this.random));
            }
         }

         this.nextSongDelay = Math.min(this.nextSongDelay, music.maxDelay());
         if (this.currentMusic == null && !(this.minecraft.gui.screen() instanceof LevelLoadingScreen) && --this.nextSongDelay <= 0) {
            this.startPlaying(music);
         }
      }
   }

   private static boolean canReplace(final Music music, final SoundInstance currentMusic) {
      return music.replaceCurrentMusic() && !music.sound().value().location().equals(currentMusic.getIdentifier());
   }

   public void startPlaying(final Music music) {
      SoundEvent soundEvent = music.sound().value();
      this.currentMusic = SimpleSoundInstance.forMusic(soundEvent);
      switch (this.minecraft.getSoundManager().play(this.currentMusic)) {
         case STARTED:
            this.minecraft.gui.toastManager().showNowPlayingToast();
            this.toastShown = true;
            break;
         case STARTED_SILENTLY:
            this.toastShown = false;
      }

      this.nextSongDelay = Integer.MAX_VALUE;
   }

   public void showNowPlayingToastIfNeeded() {
      if (!this.toastShown) {
         this.minecraft.gui.toastManager().showNowPlayingToast();
         this.toastShown = true;
      }
   }

   public void stopPlaying(final Music music) {
      if (this.isPlayingMusic(music)) {
         this.stopPlaying();
      }
   }

   public void stopPlaying() {
      if (this.currentMusic != null) {
         this.minecraft.getSoundManager().stop(this.currentMusic);
         this.currentMusic = null;
         this.minecraft.gui.toastManager().hideNowPlayingToast();
      }

      this.nextSongDelay = this.gameMusicFrequency.getNextSongDelay(this.minecraft.getSituationalMusic(), this.random) + 100;
   }

   private boolean fadePlaying(final float volume) {
      if (this.currentMusic == null) {
         return false;
      }

      if (this.currentGain == volume) {
         return true;
      }

      if (this.currentGain < volume) {
         this.currentGain = this.currentGain + Mth.clamp(this.currentGain, 5.0E-4F, 0.005F);
         if (this.currentGain > volume) {
            this.currentGain = volume;
         }
      } else {
         this.currentGain = 0.03F * volume + 0.97F * this.currentGain;
         if (Math.abs(this.currentGain - volume) < 1.0E-4F || this.currentGain < volume) {
            this.currentGain = volume;
         }
      }

      this.currentGain = Mth.clamp(this.currentGain, 0.0F, 1.0F);
      if (this.currentGain <= 1.0E-4F) {
         this.stopPlaying();
         return false;
      } else {
         this.minecraft.getSoundManager().updateCategoryVolume(SoundSource.MUSIC, this.currentGain);
         return true;
      }
   }

   public boolean isPlayingMusic(final Music music) {
      return this.currentMusic == null ? false : music.sound().value().location().equals(this.currentMusic.getIdentifier());
   }

   public @Nullable String getCurrentMusicTranslationKey() {
      if (this.currentMusic != null) {
         Sound sound = this.currentMusic.getSound();
         if (sound != null) {
            return sound.getLocation().toShortLanguageKey();
         }
      }

      return null;
   }

   public void setMinutesBetweenSongs(final MusicManager.MusicFrequency musicFrequency) {
      this.gameMusicFrequency = musicFrequency;
      this.nextSongDelay = this.gameMusicFrequency.getNextSongDelay(this.minecraft.getSituationalMusic(), this.random);
   }

   public enum MusicFrequency implements StringRepresentable {
      DEFAULT("DEFAULT", "options.music_frequency.default", 20),
      FREQUENT("FREQUENT", "options.music_frequency.frequent", 10),
      CONSTANT("CONSTANT", "options.music_frequency.constant", 0);

      public static final Codec<MusicManager.MusicFrequency> CODEC = StringRepresentable.fromEnum(MusicManager.MusicFrequency::values);
      private final String name;
      private final int maxFrequency;
      private final Component caption;

      MusicFrequency(final String name, final String translationKey, final int maxFrequencyMinutes) {
         this.name = name;
         this.maxFrequency = maxFrequencyMinutes * 1200;
         this.caption = Component.translatable(translationKey);
      }

      private int getNextSongDelay(final @Nullable Music music, final RandomSource random) {
         if (music == null) {
            return this.maxFrequency;
         }

         if (this == CONSTANT) {
            return 100;
         }

         int minFrequency = Math.min(music.minDelay(), this.maxFrequency);
         int maxFrequency = Math.min(music.maxDelay(), this.maxFrequency);
         return Mth.nextInt(random, minFrequency, maxFrequency);
      }

      public Component caption() {
         return this.caption;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
