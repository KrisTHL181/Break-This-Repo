package net.minecraft.client.resources.sounds;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class ElytraOnPlayerSoundInstance extends AbstractTickableSoundInstance {
   public static final int DELAY = 20;
   private final LocalPlayer player;
   private int time;

   public ElytraOnPlayerSoundInstance(final LocalPlayer player) {
      super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
      this.player = player;
      this.looping = true;
      this.delay = 0;
      this.volume = 0.1F;
   }

   @Override
   public void tick() {
      this.time++;
      if (!this.player.isRemoved() && (this.time <= 20 || this.player.isFallFlying())) {
         this.x = (float)this.player.getX();
         this.y = (float)this.player.getY();
         this.z = (float)this.player.getZ();
         float speed = (float)this.player.getDeltaMovement().lengthSqr();
         if (speed >= 1.0E-7) {
            this.volume = Mth.clamp(speed / 4.0F, 0.0F, 1.0F);
         } else {
            this.volume = 0.0F;
         }

         if (this.time < 20) {
            this.volume = 0.0F;
         } else if (this.time < 40) {
            this.volume = this.volume * ((this.time - 20) / 20.0F);
         }

         float pitchThreshold = 0.8F;
         if (this.volume > 0.8F) {
            this.pitch = 1.0F + (this.volume - 0.8F);
         } else {
            this.pitch = 1.0F;
         }
      } else {
         this.stop();
      }
   }
}
