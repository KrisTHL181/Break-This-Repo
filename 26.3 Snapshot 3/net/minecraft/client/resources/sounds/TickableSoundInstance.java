package net.minecraft.client.resources.sounds;

public interface TickableSoundInstance extends SoundInstance {
   boolean isStopped();

   void tick();
}
