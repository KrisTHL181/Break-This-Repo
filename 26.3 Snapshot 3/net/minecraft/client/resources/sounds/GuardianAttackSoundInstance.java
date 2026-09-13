package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Guardian;

public class GuardianAttackSoundInstance extends AbstractTickableSoundInstance {
   private static final float VOLUME_MIN = 0.0F;
   private static final float VOLUME_SCALE = 1.0F;
   private static final float PITCH_MIN = 0.7F;
   private static final float PITCH_SCALE = 0.5F;
   private final Guardian guardian;

   public GuardianAttackSoundInstance(final Guardian guardian) {
      super(SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
      this.guardian = guardian;
      this.attenuation = SoundInstance.Attenuation.NONE;
      this.looping = true;
      this.delay = 0;
   }

   @Override
   public boolean canPlaySound() {
      return !this.guardian.isSilent();
   }

   @Override
   public void tick() {
      if (!this.guardian.isRemoved() && this.guardian.getTarget() == null) {
         this.x = (float)this.guardian.getX();
         this.y = (float)this.guardian.getY();
         this.z = (float)this.guardian.getZ();
         float scale = this.guardian.getAttackAnimationScale(0.0F);
         this.volume = 0.0F + 1.0F * scale * scale;
         this.pitch = 0.7F + 0.5F * scale;
      } else {
         this.stop();
      }
   }
}
