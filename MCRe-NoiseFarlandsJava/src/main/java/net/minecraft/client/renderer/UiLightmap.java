package net.minecraft.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UiLightmap implements AutoCloseable {
    private final DynamicTexture texture = new DynamicTexture("UI Lightmap", 1, 1, false);

    public UiLightmap() {
        NativeImage pixels = this.texture.getPixels();
        pixels.setPixel(0, 0, -1);
        this.texture.upload();
    }

    public GpuTextureView getTextureView() {
        return this.texture.getTextureView();
    }

    @Override
    public void close() {
        this.texture.close();
    }
}