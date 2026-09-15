package net.minecraft.client.sounds;

import net.minecraft.client.resources.sounds.SoundInstance;

public interface SoundEventListener {
   void onPlaySound(SoundInstance sound, WeighedSoundEvents soundEvent, float range);
}
