package net.minecraft.client.renderer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class LightmapRenderStateExtractor {
    public static final Vector3fc WHITE = new Vector3f(1.0F, 1.0F, 1.0F);
    private boolean needsUpdate;
    private final GameRenderer renderer;
    private final Minecraft minecraft;
    private final RandomSource randomSource = RandomSource.create();
    private float blockLightFlicker;

    public LightmapRenderStateExtractor(final GameRenderer renderer, final Minecraft minecraft) {
        this.renderer = renderer;
        this.minecraft = minecraft;
    }

    public void tick() {
        this.blockLightFlicker = this.blockLightFlicker
            + (this.randomSource.nextFloat() - this.randomSource.nextFloat()) * this.randomSource.nextFloat() * this.randomSource.nextFloat() * 0.1F;
        this.blockLightFlicker *= 0.9F;
        this.needsUpdate = true;
    }

    private float calculateDarknessScale(final LivingEntity camera, final float darknessGamma, final float partialTickTime) {
        float darkness = 0.45F * darknessGamma;
        return Math.max(0.0F, Mth.cos((camera.tickCount - partialTickTime) * (float) Math.PI * 0.025F) * darkness);
    }

    public void extract(final LightmapRenderState renderState, final float partialTicks) {
        renderState.needsUpdate = this.needsUpdate;
        if (this.needsUpdate) {
            ClientLevel level = this.minecraft.level;
            LocalPlayer player = this.minecraft.player;
            if (level != null && player != null) {
                ProfilerFiller profiler = Profiler.get();
                profiler.push("lightmap");
                Camera camera = this.renderer.mainCamera();
                renderState.blockFactor = this.blockLightFlicker + 1.4F;
                renderState.blockLightTint = ARGB.vector3fFromRGB24(camera.attributeProbe().getValue(EnvironmentAttributes.BLOCK_LIGHT_TINT, partialTicks));
                renderState.skyFactor = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, partialTicks);
                renderState.skyLightColor = ARGB.vector3fFromRGB24(camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_COLOR, partialTicks));
                EndFlashState endFlashState = level.endFlashState();
                if (endFlashState != null && !this.minecraft.options.hideLightningFlash().get()) {
                    float intensity = endFlashState.getIntensity(partialTicks);
                    if (this.minecraft.gui.hud.getBossOverlay().shouldCreateWorldFog()) {
                        renderState.skyFactor += intensity / 3.0F;
                    } else {
                        renderState.skyFactor += intensity;
                    }
                }

                renderState.ambientColor = ARGB.vector3fFromRGB24(camera.attributeProbe().getValue(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, partialTicks));
                float brightnessOption = this.minecraft.options.gamma().get().floatValue();
                float darknessEffectScaleOption = this.minecraft.options.darknessEffectScale().get().floatValue();
                float darknessEffectBrightnessModifier = player.getEffectBlendFactor(MobEffects.DARKNESS, partialTicks) * darknessEffectScaleOption;
                renderState.brightness = Math.max(0.0F, brightnessOption - darknessEffectBrightnessModifier);
                renderState.darknessEffectScale = this.calculateDarknessScale(player, darknessEffectBrightnessModifier, partialTicks)
                    * darknessEffectScaleOption;
                float waterVision = player.getWaterVision();
                if (player.hasEffect(MobEffects.NIGHT_VISION)) {
                    renderState.nightVisionEffectIntensity = GameRenderer.nightVisionScale(player, partialTicks);
                } else if (waterVision > 0.0F && player.hasEffect(MobEffects.CONDUIT_POWER)) {
                    renderState.nightVisionEffectIntensity = waterVision;
                } else {
                    renderState.nightVisionEffectIntensity = 0.0F;
                }

                renderState.nightVisionColor = ARGB.vector3fFromRGB24(camera.attributeProbe().getValue(EnvironmentAttributes.NIGHT_VISION_COLOR, partialTicks));
                renderState.bossOverlayWorldDarkening = this.renderer.bossOverlayWorldDarkening(partialTicks);
                profiler.pop();
                this.needsUpdate = false;
            }
        }
    }
}