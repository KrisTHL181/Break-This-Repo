package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityBoundSoundInstance extends AbstractTickableSoundInstance {
   private final Entity entity;

   public EntityBoundSoundInstance(SoundEvent p_235080_, SoundSource p_235081_, float p_235082_, float p_235083_, Entity p_235084_, long p_235085_) {
      super(p_235080_, p_235081_, RandomSource.create(p_235085_));
      this.volume = p_235082_;
      this.pitch = p_235083_;
      this.entity = p_235084_;
      this.x = (float)this.entity.getX();
      this.y = (float)this.entity.getY();
      this.z = (float)this.entity.getZ();
   }

   @Override
   public boolean canPlaySound() {
      return !this.entity.isSilent();
   }

   @Override
   public void tick() {
      if (this.entity.isRemoved()) {
         this.stop();
      } else {
         this.x = (float)this.entity.getX();
         this.y = (float)this.entity.getY();
         this.z = (float)this.entity.getZ();
      }
   }
}
