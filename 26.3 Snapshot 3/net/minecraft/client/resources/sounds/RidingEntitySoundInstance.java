package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class RidingEntitySoundInstance extends AbstractTickableSoundInstance {
   private final Player player;
   private final Entity entity;
   private final boolean underwaterSound;
   private final float volumeMin;
   private final float volumeMax;
   private final float volumeAmplifier;

   public RidingEntitySoundInstance(
      final Player player,
      final Entity entity,
      final boolean underwaterSound,
      final SoundEvent soundEvent,
      final SoundSource soundSource,
      final float volumeMin,
      final float volumeMax,
      final float volumeAmplifier
   ) {
      super(soundEvent, soundSource, SoundInstance.createUnseededRandom());
      this.player = player;
      this.entity = entity;
      this.underwaterSound = underwaterSound;
      this.volumeMin = volumeMin;
      this.volumeMax = volumeMax;
      this.volumeAmplifier = volumeAmplifier;
      this.attenuation = SoundInstance.Attenuation.NONE;
      this.looping = true;
      this.delay = 0;
      this.volume = volumeMin;
   }

   @Override
   public boolean canPlaySound() {
      return !this.entity.isSilent();
   }

   @Override
   public boolean canStartSilent() {
      return true;
   }

   protected boolean shouldNotPlayUnderwaterSound() {
      return this.underwaterSound != this.entity.isUnderWater();
   }

   protected float getEntitySpeed() {
      return (float)this.entity.getDeltaMovement().length();
   }

   protected boolean shoudlPlaySound() {
      return true;
   }

   @Override
   public void tick() {
      if (this.entity.isRemoved() || !this.player.isPassenger() || this.player.getVehicle() != this.entity) {
         this.stop();
      } else if (this.shouldNotPlayUnderwaterSound()) {
         this.volume = this.volumeMin;
      } else {
         float speed = this.getEntitySpeed();
         if (speed >= 0.01F && this.shoudlPlaySound()) {
            this.volume = this.volumeAmplifier * Mth.clampedLerp(speed, this.volumeMin, this.volumeMax);
         } else {
            this.volume = this.volumeMin;
         }
      }
   }
}
