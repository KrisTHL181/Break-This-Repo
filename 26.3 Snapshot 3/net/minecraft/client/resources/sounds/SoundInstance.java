package net.minecraft.client.resources.sounds;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public interface SoundInstance {
   Identifier getIdentifier();

   @Nullable WeighedSoundEvents resolve(SoundManager soundManager);

   @Nullable Sound getSound();

   SoundSource getSource();

   boolean isLooping();

   boolean isRelative();

   int getDelay();

   float getVolume();

   float getPitch();

   double getX();

   double getY();

   double getZ();

   SoundInstance.Attenuation getAttenuation();

   default boolean canStartSilent() {
      return false;
   }

   default boolean canPlaySound() {
      return true;
   }

   static RandomSource createUnseededRandom() {
      return RandomSource.create();
   }

   enum Attenuation {
      NONE,
      LINEAR;
   }
}
