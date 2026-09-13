package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class DynamicTexture extends AbstractTexture implements Dumpable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private NativeImage pixels;

   public DynamicTexture(final Supplier<String> label, final NativeImage image) {
      this.pixels = image;
      this.createTexture(label);
      this.upload();
   }

   public DynamicTexture(final String label, final int width, final int height, final boolean zero) {
      this.pixels = new NativeImage(width, height, zero);
      this.createTexture(label);
   }

   public DynamicTexture(final Supplier<String> label, final int width, final int height, final boolean zero) {
      this.pixels = new NativeImage(width, height, zero);
      this.createTexture(label);
   }

   private void createTexture(final Supplier<String> label) {
      GpuDevice device = RenderSystem.getDevice();
      this.texture = device.createTexture(label, 5, GpuFormat.RGBA8_UNORM, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
      this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
      this.textureView = device.createTextureView(this.texture);
   }

   private void createTexture(final String label) {
      GpuDevice device = RenderSystem.getDevice();
      this.texture = device.createTexture(label, 5, GpuFormat.RGBA8_UNORM, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
      this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
      this.textureView = device.createTextureView(this.texture);
   }

   public void upload() {
      if (this.texture != null) {
         RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.pixels);
      } else {
         LOGGER.warn("Trying to upload disposed texture {}", this.getTexture().getLabel());
      }
   }

   public NativeImage getPixels() {
      return this.pixels;
   }

   public void setPixels(final NativeImage pixels) {
      this.pixels.close();
      this.pixels = pixels;
   }

   @Override
   public void close() {
      this.pixels.close();
      super.close();
   }

   @Override
   public void dumpContents(final Identifier selfId, final Path dir) throws IOException {
      if (!this.pixels.isClosed()) {
         String outputId = selfId.toDebugFileName() + ".png";
         Path path = dir.resolve(outputId);
         this.pixels.writeToFile(path);
      }
   }
}
