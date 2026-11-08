package net.minecraft.client.sounds;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SoundEventListener {
   void onPlaySound(SoundInstance var1, WeighedSoundEvents var2, float var3);
}
