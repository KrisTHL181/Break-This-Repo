package net.minecraft.client.resources.sounds;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface SoundInstance {
    Identifier getIdentifier();

    @Nullable WeighedSoundEvents resolve(SoundManager soundManager);

    @Nullable Sound getSound();

    SoundSource getSource();

    boolean isLooping();

    boolean isRelative();

    int getDelay();

    float getVolume();

    float getPitch();

    double getX();

    double getY();

    double getZ();

    SoundInstance.Attenuation getAttenuation();

    default boolean canStartSilent() {
        return false;
    }

    default boolean canPlaySound() {
        return true;
    }

    static RandomSource createUnseededRandom() {
        return RandomSource.create();
    }

    @OnlyIn(Dist.CLIENT)
    enum Attenuation {
        NONE,
        LINEAR;
    }
}