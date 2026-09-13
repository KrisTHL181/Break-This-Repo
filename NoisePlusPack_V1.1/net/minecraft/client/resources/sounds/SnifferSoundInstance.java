package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnifferSoundInstance extends AbstractTickableSoundInstance {
   private static final float VOLUME = 1.0F;
   private static final float PITCH = 1.0F;
   private final Sniffer sniffer;

   public SnifferSoundInstance(Sniffer p_273565_) {
      super(SoundEvents.SNIFFER_DIGGING, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
      this.sniffer = p_273565_;
      this.attenuation = SoundInstance.Attenuation.LINEAR;
      this.looping = false;
      this.delay = 0;
   }

   @Override
   public boolean canPlaySound() {
      return !this.sniffer.isSilent();
   }

   @Override
   public void tick() {
      if (!this.sniffer.isRemoved() && this.sniffer.getTarget() == null && this.sniffer.canPlayDiggingSound()) {
         this.x = (float)this.sniffer.getX();
         this.y = (float)this.sniffer.getY();
         this.z = (float)this.sniffer.getZ();
         this.volume = 1.0F;
         this.pitch = 1.0F;
      } else {
         this.stop();
      }
   }
}
