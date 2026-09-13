package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public abstract class ReloadableTexture extends AbstractTexture {
   private final Identifier resourceId;

   public ReloadableTexture(final Identifier resourceId) {
      this.resourceId = resourceId;
   }

   public Identifier resourceId() {
      return this.resourceId;
   }

   public void apply(final TextureContents contents) {
      boolean clamp = contents.clamp();
      boolean blur = contents.blur();
      AddressMode addressMode = clamp ? AddressMode.CLAMP_TO_EDGE : AddressMode.REPEAT;
      FilterMode minMag = blur ? FilterMode.LINEAR : FilterMode.NEAREST;
      this.sampler = RenderSystem.getSamplerCache().getSampler(addressMode, addressMode, minMag, minMag, false);

      try (NativeImage image = contents.image()) {
         this.doLoad(image);
      }
   }

   protected void doLoad(final NativeImage image) {
      GpuDevice device = RenderSystem.getDevice();
      this.close();
      this.texture = device.createTexture(this.resourceId::toString, 5, GpuFormat.RGBA8_UNORM, image.getWidth(), image.getHeight(), 1, 1);
      this.textureView = device.createTextureView(this.texture);
      device.createCommandEncoder().writeToTexture(this.texture, image);
   }

   public abstract TextureContents loadContents(ResourceManager resourceManager) throws IOException;
}
