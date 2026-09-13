package com.mojang.renderpearl.backend.api;

import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Collection;

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
