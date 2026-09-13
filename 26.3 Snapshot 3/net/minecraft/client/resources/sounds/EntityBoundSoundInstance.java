package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

public class EntityBoundSoundInstance extends AbstractTickableSoundInstance {
   private final Entity entity;

   public EntityBoundSoundInstance(
      final SoundEvent event, final SoundSource source, final float volume, final float pitch, final Entity entity, final long seed
   ) {
      super(event, source, RandomSource.create(seed));
      this.volume = volume;
      this.pitch = pitch;
      this.entity = entity;
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
