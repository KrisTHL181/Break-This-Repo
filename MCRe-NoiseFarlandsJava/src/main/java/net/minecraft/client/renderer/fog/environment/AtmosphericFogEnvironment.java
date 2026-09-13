package net.minecraft.client.renderer.fog.environment;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class AtmosphericFogEnvironment extends FogEnvironment {
    private static final int MIN_RAIN_FOG_SKY_LIGHT = 8;
    private static final float RAIN_FOG_START_OFFSET = -160.0F;
    private static final float RAIN_FOG_END_OFFSET = -256.0F;
    private float rainFogMultiplier;

    @Override
    public int getBaseColor(final ClientLevel level, final Camera camera, final int renderDistance, final float partialTicks) {
        int fogColor = camera.attributeProbe().getValue(EnvironmentAttributes.FOG_COLOR, partialTicks);
        if (renderDistance >= 4) {
            float sunAngle = camera.attributeProbe().getValue(EnvironmentAttributes.SUN_ANGLE, partialTicks) * (float) (Math.PI / 180.0);
            float sunX = Mth.sin(sunAngle) > 0.0F ? -1.0F : 1.0F;
            Vector3fc forwardVector = camera.isPanoramicMode() ? camera.panoramicForwards() : camera.forwardVector();
            float lookingAtTheSunFactor = forwardVector.dot(sunX, 0.0F, 0.0F);
            if (lookingAtTheSunFactor > 0.0F) {
                int color = camera.attributeProbe().getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, partialTicks);
                float alpha = ARGB.alphaFloat(color);
                if (alpha > 0.0F) {
                    fogColor = ARGB.srgbLerp(lookingAtTheSunFactor * alpha, fogColor, ARGB.opaque(color));
                }
            }
        }

        int skyColor = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_COLOR, partialTicks);
        skyColor = applyWeatherDarken(skyColor, level.getRainLevel(partialTicks), level.getThunderLevel(partialTicks));
        float skyFogEnd = Math.min(camera.attributeProbe().getValue(EnvironmentAttributes.SKY_FOG_END_DISTANCE, partialTicks) / 16.0F, renderDistance);
        float skyColorMixFactor = Mth.clampedLerp(skyFogEnd / 32.0F, 0.25F, 1.0F);
        skyColorMixFactor = 1.0F - (float)Math.pow(skyColorMixFactor, 0.25);
        return ARGB.srgbLerp(skyColorMixFactor, fogColor, skyColor);
    }

    private static int applyWeatherDarken(int color, final float rainLevel, final float thunderLevel) {
        if (rainLevel > 0.0F) {
            float rainColorModifier = 1.0F - rainLevel * 0.5F;
            float rainBlueColorModifier = 1.0F - rainLevel * 0.4F;
            color = ARGB.scaleRGB(color, rainColorModifier, rainColorModifier, rainBlueColorModifier);
        }

        if (thunderLevel > 0.0F) {
            color = ARGB.scaleRGB(color, 1.0F - thunderLevel * 0.5F);
        }

        return color;
    }

    @Override
    public void setupFog(final FogData fog, final Camera camera, final ClientLevel level, final float renderDistance, final DeltaTracker deltaTracker) {
        this.updateRainFogState(camera, level, deltaTracker);
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        fog.environmentalStart = camera.attributeProbe().getValue(EnvironmentAttributes.FOG_START_DISTANCE, partialTicks);
        fog.environmentalEnd = camera.attributeProbe().getValue(EnvironmentAttributes.FOG_END_DISTANCE, partialTicks);
        fog.environmentalStart = fog.environmentalStart + -160.0F * this.rainFogMultiplier;
        float minRainFogEnd = Math.min(96.0F, fog.environmentalEnd);
        fog.environmentalEnd = Math.max(minRainFogEnd, fog.environmentalEnd + -256.0F * this.rainFogMultiplier);
        fog.skyEnd = Math.min(renderDistance, camera.attributeProbe().getValue(EnvironmentAttributes.SKY_FOG_END_DISTANCE, partialTicks));
        fog.cloudEnd = Math.min(
            Minecraft.getInstance().options.cloudRange().get() * 16,
            camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_FOG_END_DISTANCE, partialTicks)
        );
        if (Minecraft.getInstance().gui.hud.getBossOverlay().shouldCreateWorldFog()) {
            fog.environmentalStart = Math.min(fog.environmentalStart, 10.0F);
            fog.environmentalEnd = Math.min(fog.environmentalEnd, 96.0F);
            fog.skyEnd = fog.environmentalEnd;
            fog.cloudEnd = fog.environmentalEnd;
        }
    }

    private void updateRainFogState(final Camera camera, final ClientLevel level, final DeltaTracker deltaTracker) {
        BlockPos blockPos = camera.blockPosition();
        Biome biome = level.getBiome(blockPos).value();
        float deltaTicks = deltaTracker.getGameTimeDeltaTicks();
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        boolean rainsInBiome = biome.hasPrecipitation();
        float skyLightLevelMultiplier = Mth.clamp((level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(blockPos) - 8.0F) / 7.0F, 0.0F, 1.0F);
        float targetRainFogMultiplier = level.getRainLevel(partialTicks) * skyLightLevelMultiplier * (rainsInBiome ? 1.0F : 0.5F);
        this.rainFogMultiplier = this.rainFogMultiplier + (targetRainFogMultiplier - this.rainFogMultiplier) * deltaTicks * 0.2F;
    }

    @Override
    public boolean isApplicable(final @Nullable FogType fogType, final Entity entity) {
        return fogType == FogType.ATMOSPHERIC;
    }
}