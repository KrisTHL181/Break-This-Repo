package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DynamicTexture extends AbstractTexture implements Dumpable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private @Nullable NativeImage pixels;

   public DynamicTexture(Supplier<String> p_395494_, NativeImage p_392708_) {
      this.pixels = p_392708_;
      this.createTexture(p_395494_);
      this.upload();
   }

   public DynamicTexture(String p_397850_, int p_391329_, int p_394428_, boolean p_393593_) {
      this.pixels = new NativeImage(p_391329_, p_394428_, p_393593_);
      this.createTexture(p_397850_);
   }

   public DynamicTexture(Supplier<String> p_392557_, int p_117980_, int p_117981_, boolean p_117982_) {
      this.pixels = new NativeImage(p_117980_, p_117981_, p_117982_);
      this.createTexture(p_392557_);
   }

   private void createTexture(Supplier<String> p_406248_) {
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.texture = gpudevice.createTexture(p_406248_, 5, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
      this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
      this.textureView = gpudevice.createTextureView(this.texture);
   }

   private void createTexture(String p_409829_) {
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.texture = gpudevice.createTexture(p_409829_, 5, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
      this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
      this.textureView = gpudevice.createTextureView(this.texture);
   }

   public void upload() {
      if (this.pixels != null && this.texture != null) {
         RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.pixels);
      } else {
         LOGGER.warn("Trying to upload disposed texture {}", this.getTexture().getLabel());
      }
   }

   public @Nullable NativeImage getPixels() {
      return this.pixels;
   }

   public void setPixels(NativeImage p_117989_) {
      if (this.pixels != null) {
         this.pixels.close();
      }

      this.pixels = p_117989_;
   }

   @Override
   public void close() {
      if (this.pixels != null) {
         this.pixels.close();
         this.pixels = null;
      }

      super.close();
   }

   @Override
   public void dumpContents(Identifier p_451595_, Path p_276105_) throws IOException {
      if (this.pixels != null) {
         String s = p_451595_.toDebugFileName() + ".png";
         Path path = p_276105_.resolve(s);
         this.pixels.writeToFile(path);
      }
   }
}
