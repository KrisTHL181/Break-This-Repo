package net.minecraft.client.resources.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.bee.Bee;

public abstract class BeeSoundInstance extends AbstractTickableSoundInstance {
   private static final float VOLUME_MIN = 0.0F;
   private static final float VOLUME_MAX = 1.2F;
   private static final float PITCH_MIN = 0.0F;
   protected final Bee bee;
   private boolean hasSwitched;

   public BeeSoundInstance(final Bee bee, final SoundEvent event, final SoundSource source) {
      super(event, source, SoundInstance.createUnseededRandom());
      this.bee = bee;
      this.x = (float)bee.getX();
      this.y = (float)bee.getY();
      this.z = (float)bee.getZ();
      this.looping = true;
      this.delay = 0;
      this.volume = 0.0F;
   }

   @Override
   public void tick() {
      boolean shouldSwitchSounds = this.shouldSwitchSounds();
      if (shouldSwitchSounds && !this.isStopped()) {
         Minecraft.getInstance().getSoundManager().queueTickingSound(this.getAlternativeSoundInstance());
         this.hasSwitched = true;
      }

      if (!this.bee.isRemoved() && !this.hasSwitched) {
         this.x = (float)this.bee.getX();
         this.y = (float)this.bee.getY();
         this.z = (float)this.bee.getZ();
         float speed = (float)this.bee.getDeltaMovement().horizontalDistance();
         if (speed >= 0.01F) {
            this.pitch = Mth.lerp(Mth.clamp(speed, this.getMinPitch(), this.getMaxPitch()), this.getMinPitch(), this.getMaxPitch());
            this.volume = Mth.lerp(Mth.clamp(speed, 0.0F, 0.5F), 0.0F, 1.2F);
         } else {
            this.pitch = 0.0F;
            this.volume = 0.0F;
         }
      } else {
         this.stop();
      }
   }

   private float getMinPitch() {
      return this.bee.isBaby() ? 1.1F : 0.7F;
   }

   private float getMaxPitch() {
      return this.bee.isBaby() ? 1.5F : 1.1F;
   }

   @Override
   public boolean canStartSilent() {
      return true;
   }

   @Override
   public boolean canPlaySound() {
      return !this.bee.isSilent();
   }

   protected abstract AbstractTickableSoundInstance getAlternativeSoundInstance();

   protected abstract boolean shouldSwitchSounds();
}
