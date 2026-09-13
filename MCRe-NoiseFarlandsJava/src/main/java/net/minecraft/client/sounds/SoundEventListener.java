package net.minecraft.client.sounds;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SoundEventListener {
    void onPlaySound(SoundInstance sound, WeighedSoundEvents soundEvent, float range);
}