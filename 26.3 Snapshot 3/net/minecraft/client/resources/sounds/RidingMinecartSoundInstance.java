package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;

public class RidingMinecartSoundInstance extends RidingEntitySoundInstance {
   private final Player player;
   private final AbstractMinecart minecart;
   private final boolean underwaterSound;

   public RidingMinecartSoundInstance(
      final Player player,
      final AbstractMinecart minecart,
      final boolean underwaterSound,
      final SoundEvent soundEvent,
      final float volumeMin,
      final float volumeMax,
      final float volumeAmplifier
   ) {
      super(player, minecart, underwaterSound, soundEvent, SoundSource.NEUTRAL, volumeMin, volumeMax, volumeAmplifier);
      this.player = player;
      this.minecart = minecart;
      this.underwaterSound = underwaterSound;
   }

   @Override
   protected boolean shouldNotPlayUnderwaterSound() {
      return this.underwaterSound != this.player.isUnderWater();
   }

   @Override
   protected float getEntitySpeed() {
      return (float)this.minecart.getDeltaMovement().horizontalDistance();
   }

   @Override
   protected boolean shoudlPlaySound() {
      return this.minecart.isOnRails() || !(this.minecart.getBehavior() instanceof NewMinecartBehavior);
   }
}
