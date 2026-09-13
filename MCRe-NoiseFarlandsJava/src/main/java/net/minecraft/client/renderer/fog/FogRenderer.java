package net.minecraft.client.renderer.fog;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class FogRenderer implements AutoCloseable {
    public static final int FOG_UBO_SIZE = new Std140SizeCalculator().putVec4().putFloat().putFloat().putFloat().putFloat().putFloat().putFloat().get();
    private static final List<FogEnvironment> FOG_ENVIRONMENTS = Lists.newArrayList(
        new LavaFogEnvironment(),
        new PowderedSnowFogEnvironment(),
        new BlindnessFogEnvironment(),
        new DarknessFogEnvironment(),
        new WaterFogEnvironment(),
        new AtmosphericFogEnvironment()
    );
    private static boolean fogEnabled = true;
    private final GpuBuffer emptyBuffer;
    private final MappableRingBuffer regularBuffer;

    public FogRenderer() {
        GpuDevice device = RenderSystem.getDevice();
        this.regularBuffer = new MappableRingBuffer(() -> "Fog UBO", 130, FOG_UBO_SIZE);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(FOG_UBO_SIZE);
            this.updateBuffer(
                buffer, 0, new Vector4f(0.0F), Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE
            );
            this.emptyBuffer = device.createBuffer(() -> "Empty fog", 128, buffer.flip());
        }

        RenderSystem.setShaderFog(this.getBuffer(FogRenderer.FogMode.NONE));
    }

    @Override
    public void close() {
        this.emptyBuffer.close();
        this.regularBuffer.close();
    }

    public void endFrame() {
        this.regularBuffer.rotate();
    }

    public GpuBufferSlice getBuffer(final FogRenderer.FogMode mode) {
        if (!fogEnabled) {
            return this.emptyBuffer.slice(0L, FOG_UBO_SIZE);
        }

        return switch (mode) {
            case NONE -> this.emptyBuffer.slice(0L, FOG_UBO_SIZE);
            case WORLD -> this.regularBuffer.currentBuffer().slice(0L, FOG_UBO_SIZE);
        };
    }

    private void computeFogColor(
        final Camera camera, final float partialTicks, final ClientLevel level, final int renderDistance, final float darkenWorldAmount, final Vector4f dest
    ) {
        FogType fogType = this.getFogType(camera);
        Entity entity = camera.entity();
        FogEnvironment colorSourceEnvironment = null;
        FogEnvironment darknessModifyingEnvironment = null;

        for (FogEnvironment fogEnvironment : FOG_ENVIRONMENTS) {
            if (fogEnvironment.isApplicable(fogType, entity)) {
                if (colorSourceEnvironment == null && fogEnvironment.providesColor()) {
                    colorSourceEnvironment = fogEnvironment;
                }

                if (darknessModifyingEnvironment == null && fogEnvironment.modifiesDarkness()) {
                    darknessModifyingEnvironment = fogEnvironment;
                }
            }
        }

        if (colorSourceEnvironment == null) {
            throw new IllegalStateException("No color source environment found");
        }

        int color = colorSourceEnvironment.getBaseColor(level, camera, renderDistance, partialTicks);
        float voidDarknessOnsetRange = level.getLevelData().voidDarknessOnsetRange();
        float darkness = Mth.clamp((voidDarknessOnsetRange + level.getMinY() - (float)camera.position().y) / voidDarknessOnsetRange, 0.0F, 1.0F);
        if (darknessModifyingEnvironment != null) {
            LivingEntity livingEntity = (LivingEntity)entity;
            darkness = darknessModifyingEnvironment.getModifiedDarkness(livingEntity, darkness, partialTicks);
        }

        float fogRed = ARGB.redFloat(color);
        float fogGreen = ARGB.greenFloat(color);
        float fogBlue = ARGB.blueFloat(color);
        if (darkness > 0.0F && fogType != FogType.LAVA && fogType != FogType.POWDER_SNOW) {
            float brightness = Mth.square(1.0F - darkness);
            fogRed *= brightness;
            fogGreen *= brightness;
            fogBlue *= brightness;
        }

        if (darkenWorldAmount > 0.0F) {
            fogRed = Mth.lerp(darkenWorldAmount, fogRed, fogRed * 0.7F);
            fogGreen = Mth.lerp(darkenWorldAmount, fogGreen, fogGreen * 0.6F);
            fogBlue = Mth.lerp(darkenWorldAmount, fogBlue, fogBlue * 0.6F);
        }

        float brightenFactor;
        if (fogType == FogType.WATER) {
            if (entity instanceof LocalPlayer localPlayer) {
                brightenFactor = localPlayer.getWaterVision();
            } else {
                brightenFactor = 1.0F;
            }
        } else if (entity instanceof LivingEntity livingEntity
            && livingEntity.hasEffect(MobEffects.NIGHT_VISION)
            && !livingEntity.hasEffect(MobEffects.DARKNESS)) {
            brightenFactor = GameRenderer.nightVisionScale(livingEntity, partialTicks);
        } else {
            brightenFactor = 0.0F;
        }

        if (fogRed != 0.0F && fogGreen != 0.0F && fogBlue != 0.0F) {
            float maxColor = Math.max(fogRed, Math.max(fogGreen, fogBlue));
            float targetScale = 1.0F / Math.clamp(maxColor, 0.07F, 1.0F);
            float targetScaleMax = 1.0F / maxColor;
            float scale = maxColor != fogRed ? targetScale : targetScaleMax;
            fogRed = Mth.lerp(brightenFactor, fogRed, fogRed * scale);
            scale = maxColor != fogGreen ? targetScale : targetScaleMax;
            fogGreen = Mth.lerp(brightenFactor, fogGreen, fogGreen * scale);
            scale = maxColor != fogBlue ? targetScale : targetScaleMax;
            fogBlue = Mth.lerp(brightenFactor, fogBlue, fogBlue * scale);
        }

        dest.set(fogRed, fogGreen, fogBlue, 1.0F);
    }

    public static boolean toggleFog() {
        return fogEnabled = !fogEnabled;
    }

    public FogData setupFog(
        final Camera camera, final int renderDistanceInChunks, final DeltaTracker deltaTracker, final float darkenWorldAmount, final ClientLevel level
    ) {
        float partialTickTime = deltaTracker.getGameTimeDeltaPartialTick(false);
        float renderDistanceInBlocks = renderDistanceInChunks * 16;
        FogType fogType = this.getFogType(camera);
        Entity entity = camera.entity();
        FogData fog = new FogData();
        this.computeFogColor(camera, partialTickTime, level, renderDistanceInChunks, darkenWorldAmount, fog.color);

        for (FogEnvironment fogEnvironment : FOG_ENVIRONMENTS) {
            if (fogEnvironment.isApplicable(fogType, entity)) {
                fogEnvironment.setupFog(fog, camera, level, renderDistanceInBlocks, deltaTracker);
                break;
            }
        }

        float renderDistanceFogSpan = Mth.clamp(renderDistanceInBlocks / 10.0F, 4.0F, 64.0F);
        fog.renderDistanceStart = renderDistanceInBlocks - renderDistanceFogSpan;
        fog.renderDistanceEnd = renderDistanceInBlocks;
        return fog;
    }

    public void updateBuffer(final FogData fog) {
        try (GpuBufferSlice.MappedView view = this.regularBuffer.currentBuffer().map(false, true)) {
            this.updateBuffer(
                view.data(),
                0,
                fog.color,
                fog.environmentalStart,
                fog.environmentalEnd,
                fog.renderDistanceStart,
                fog.renderDistanceEnd,
                fog.skyEnd,
                fog.cloudEnd
            );
        }
    }

    private FogType getFogType(final Camera camera) {
        FogType blockFogType = camera.getFluidInCamera();
        return blockFogType == FogType.NONE ? FogType.ATMOSPHERIC : blockFogType;
    }

    private void updateBuffer(
        final ByteBuffer byteBuffer,
        final int offset,
        final Vector4f fogColor,
        final float environmentalStart,
        final float environmentalEnd,
        final float renderDistanceStart,
        final float renderDistanceEnd,
        final float skyEnd,
        final float endClouds
    ) {
        byteBuffer.position(offset);
        Std140Builder.intoBuffer(byteBuffer)
            .putVec4(fogColor)
            .putFloat(environmentalStart)
            .putFloat(environmentalEnd)
            .putFloat(renderDistanceStart)
            .putFloat(renderDistanceEnd)
            .putFloat(skyEnd)
            .putFloat(endClouds);
    }

    @OnlyIn(Dist.CLIENT)
    public enum FogMode {
        NONE,
        WORLD;
    }
}