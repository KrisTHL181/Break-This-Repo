package net.minecraft.client.gui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record TextureSetup(
    @Nullable GpuTextureView texure0,
    @Nullable GpuTextureView texure1,
    @Nullable GpuTextureView texure2,
    @Nullable GpuSampler sampler0,
    @Nullable GpuSampler sampler1,
    @Nullable GpuSampler sampler2
) {
    private static final TextureSetup NO_TEXTURE_SETUP = new TextureSetup(null, null, null, null, null, null);
    private static int sortKeySeed;

    public static TextureSetup singleTexture(final GpuTextureView texture, final GpuSampler sampler) {
        return new TextureSetup(texture, null, null, sampler, null, null);
    }

    public static TextureSetup singleTextureWithLightmap(final GpuTextureView texture, final GpuSampler sampler) {
        return new TextureSetup(
            texture, null, Minecraft.getInstance().gameRenderer.lightmap(), sampler, null, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
        );
    }

    public static TextureSetup doubleTexture(final GpuTextureView texture0, final GpuSampler sampler0, final GpuTextureView texture1, final GpuSampler sampler1) {
        return new TextureSetup(texture0, texture1, null, sampler0, sampler1, null);
    }

    public static TextureSetup noTexture() {
        return NO_TEXTURE_SETUP;
    }

    public int getSortKey() {
        return SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER ? this.hashCode() * (sortKeySeed + 1) : this.hashCode();
    }

    public static void updateSortKeySeed() {
        sortKeySeed = Math.round(100000.0F * (float)Math.random());
    }
}