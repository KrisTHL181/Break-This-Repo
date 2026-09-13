package net.minecraft.client.resources.sounds;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UnderwaterAmbientSoundHandler implements AmbientSoundHandler {
    public static final float CHANCE_PER_TICK = 0.01F;
    public static final float RARE_CHANCE_PER_TICK = 0.001F;
    public static final float ULTRA_RARE_CHANCE_PER_TICK = 1.0E-4F;
    private static final int MINIMUM_TICK_DELAY = 0;
    private final LocalPlayer player;
    private final SoundManager soundManager;
    private int tickDelay = 0;

    public UnderwaterAmbientSoundHandler(final LocalPlayer player, final SoundManager soundManager) {
        this.player = player;
        this.soundManager = soundManager;
    }

    @Override
    public void tick() {
        this.tickDelay--;
        if (this.tickDelay <= 0 && this.player.isUnderWater()) {
            float rand = this.player.level().getRandom().nextFloat();
            if (rand < 1.0E-4F) {
                this.tickDelay = 0;
                this.soundManager.play(new UnderwaterAmbientSoundInstances.SubSound(this.player, SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE));
            } else if (rand < 0.001F) {
                this.tickDelay = 0;
                this.soundManager.play(new UnderwaterAmbientSoundInstances.SubSound(this.player, SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE));
            } else if (rand < 0.01F) {
                this.tickDelay = 0;
                this.soundManager.play(new UnderwaterAmbientSoundInstances.SubSound(this.player, SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS));
            }
        }
    }
}