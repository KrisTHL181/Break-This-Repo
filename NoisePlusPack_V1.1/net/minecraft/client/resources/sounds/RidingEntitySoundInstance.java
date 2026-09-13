package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RidingEntitySoundInstance extends AbstractTickableSoundInstance {
   private final Player player;
   private final Entity entity;
   private final boolean underwaterSound;
   private final float volumeMin;
   private final float volumeMax;
   private final float volumeAmplifier;

   public RidingEntitySoundInstance(
      Player p_450550_, Entity p_457061_, boolean p_452668_, SoundEvent p_454518_, SoundSource p_460935_, float p_458377_, float p_457734_, float p_452396_
   ) {
      super(p_454518_, p_460935_, SoundInstance.createUnseededRandom());
      this.player = p_450550_;
      this.entity = p_457061_;
      this.underwaterSound = p_452668_;
      this.volumeMin = p_458377_;
      this.volumeMax = p_457734_;
      this.volumeAmplifier = p_452396_;
      this.attenuation = SoundInstance.Attenuation.NONE;
      this.looping = true;
      this.delay = 0;
      this.volume = p_458377_;
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
         float f = this.getEntitySpeed();
         if (f >= 0.01F && this.shoudlPlaySound()) {
            this.volume = this.volumeAmplifier * Mth.clampedLerp(f, this.volumeMin, this.volumeMax);
         } else {
            this.volume = this.volumeMin;
         }
      }
   }
}
