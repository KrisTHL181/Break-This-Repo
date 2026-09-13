package net.minecraft.client.renderer.state.level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CameraEntityRenderState {
    public float hurtTime;
    public int hurtDuration;
    public float deathTime;
    public boolean isSleeping;
    public boolean isLiving;
    public boolean isPlayer;
    public boolean isDeadOrDying;
    public boolean doesMobEffectBlockSky;
    public float hurtDir;
    public float backwardsInterpolatedWalkDistance;
    public float bob;
}