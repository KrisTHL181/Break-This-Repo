package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BeeFlyingSoundInstance extends BeeSoundInstance {
   public BeeFlyingSoundInstance(Bee p_454305_) {
      super(p_454305_, SoundEvents.BEE_LOOP, SoundSource.NEUTRAL);
   }

   @Override
   protected AbstractTickableSoundInstance getAlternativeSoundInstance() {
      return new BeeAggressiveSoundInstance(this.bee);
   }

   @Override
   protected boolean shouldSwitchSounds() {
      return this.bee.isAngry();
   }
}
