package net.minecraft.client.sounds;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MusicManager {
   private static final int STARTING_DELAY = 100;
   private final RandomSource random = RandomSource.create();
   private final Minecraft minecraft;
   private @Nullable SoundInstance currentMusic;
   private MusicManager.MusicFrequency gameMusicFrequency;
   private float currentGain = 1.0F;
   private int nextSongDelay = 100;
   private boolean toastShown = false;

   public MusicManager(Minecraft p_120182_) {
      this.minecraft = p_120182_;
      this.gameMusicFrequency = p_120182_.options.musicFrequency().get();
   }

   public void tick() {
      float f = this.minecraft.getMusicVolume();
      if (this.currentMusic != null && this.currentGain != f) {
         boolean flag = this.fadePlaying(f);
         if (!flag) {
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

         this.nextSongDelay = Math.min(this.nextSongDelay, this.gameMusicFrequency.getNextSongDelay(music, this.random));
         if (this.currentMusic == null && this.nextSongDelay-- <= 0) {
            this.startPlaying(music);
         }
      }
   }

   private static boolean canReplace(Music p_453339_, SoundInstance p_451463_) {
      return p_453339_.replaceCurrentMusic() && !p_453339_.sound().value().location().equals(p_451463_.getIdentifier());
   }

   public void startPlaying(Music p_454803_) {
      SoundEvent soundevent = p_454803_.sound().value();
      this.currentMusic = SimpleSoundInstance.forMusic(soundevent);
      switch (this.minecraft.getSoundManager().play(this.currentMusic)) {
         case STARTED:
            this.minecraft.getToastManager().showNowPlayingToast();
            this.toastShown = true;
            break;
         case STARTED_SILENTLY:
            this.toastShown = false;
      }

      this.nextSongDelay = Integer.MAX_VALUE;
   }

   public void showNowPlayingToastIfNeeded() {
      if (!this.toastShown) {
         this.minecraft.getToastManager().showNowPlayingToast();
         this.toastShown = true;
      }
   }

   public void stopPlaying(Music p_278295_) {
      if (this.isPlayingMusic(p_278295_)) {
         this.stopPlaying();
      }
   }

   public void stopPlaying() {
      if (this.currentMusic != null) {
         this.minecraft.getSoundManager().stop(this.currentMusic);
         this.currentMusic = null;
         this.minecraft.getToastManager().hideNowPlayingToast();
      }

      this.nextSongDelay += 100;
   }

   private boolean fadePlaying(float p_375585_) {
      if (this.currentMusic == null) {
         return false;
      }

      if (this.currentGain == p_375585_) {
         return true;
      }

      if (this.currentGain < p_375585_) {
         this.currentGain = this.currentGain + Mth.clamp(this.currentGain, 5.0E-4F, 0.005F);
         if (this.currentGain > p_375585_) {
            this.currentGain = p_375585_;
         }
      } else {
         this.currentGain = 0.03F * p_375585_ + 0.97F * this.currentGain;
         if (Math.abs(this.currentGain - p_375585_) < 1.0E-4F || this.currentGain < p_375585_) {
            this.currentGain = p_375585_;
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

   public boolean isPlayingMusic(Music p_120188_) {
      return this.currentMusic == null ? false : p_120188_.sound().value().location().equals(this.currentMusic.getIdentifier());
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

   public void setMinutesBetweenSongs(MusicManager.MusicFrequency p_409813_) {
      this.gameMusicFrequency = p_409813_;
      this.nextSongDelay = this.gameMusicFrequency.getNextSongDelay(this.minecraft.getSituationalMusic(), this.random);
   }

   @OnlyIn(Dist.CLIENT)
   public enum MusicFrequency implements StringRepresentable {
      DEFAULT("DEFAULT", "options.music_frequency.default", 20),
      FREQUENT("FREQUENT", "options.music_frequency.frequent", 10),
      CONSTANT("CONSTANT", "options.music_frequency.constant", 0);

      public static final Codec<MusicManager.MusicFrequency> CODEC = StringRepresentable.fromEnum(MusicManager.MusicFrequency::values);
      private final String name;
      private final int maxFrequency;
      private final Component caption;

      MusicFrequency(final String p_457182_, final String p_455835_, final int p_408860_) {
         this.name = p_457182_;
         this.maxFrequency = p_408860_ * 1200;
         this.caption = Component.translatable(p_455835_);
      }

      int getNextSongDelay(@Nullable Music p_408535_, RandomSource p_409383_) {
         if (p_408535_ == null) {
            return this.maxFrequency;
         }

         if (this == CONSTANT) {
            return 100;
         }

         int i = Math.min(p_408535_.minDelay(), this.maxFrequency);
         int j = Math.min(p_408535_.maxDelay(), this.maxFrequency);
         return Mth.nextInt(p_409383_, i, j);
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
