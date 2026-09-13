package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Collection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GpuSurfaceBackend extends AutoCloseable {
    void configure(GpuSurface.Configuration config) throws SurfaceException;

    boolean isSuboptimal();

    void acquireNextTexture() throws SurfaceException;

    void blitFromTexture(CommandEncoderBackend commandEncoder, GpuTextureView textureView);

    void present();

    @Override
    void close();

    Collection<GpuSurface.PresentMode> supportedPresentModes();
}