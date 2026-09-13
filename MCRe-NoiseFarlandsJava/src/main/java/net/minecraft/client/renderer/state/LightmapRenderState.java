package net.minecraft.client.renderer.state;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class LightmapRenderState {
    public boolean needsUpdate = false;
    public float blockFactor;
    public Vector3fc blockLightTint = LightmapRenderStateExtractor.WHITE;
    public float skyFactor;
    public Vector3fc skyLightColor = LightmapRenderStateExtractor.WHITE;
    public Vector3fc ambientColor = LightmapRenderStateExtractor.WHITE;
    public float brightness;
    public float darknessEffectScale;
    public float nightVisionEffectIntensity;
    public Vector3fc nightVisionColor = LightmapRenderStateExtractor.WHITE;
    public float bossOverlayWorldDarkening;
}