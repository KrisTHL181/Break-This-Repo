package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractTexture implements AutoCloseable {
   protected @Nullable GpuTexture texture;
   protected @Nullable GpuTextureView textureView;
   protected GpuSampler sampler = RenderSystem.getSamplerCache()
      .getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.LINEAR, false);

   @Override
   public void close() {
      if (this.texture != null) {
         this.texture.close();
         this.texture = null;
      }

      if (this.textureView != null) {
         this.textureView.close();
         this.textureView = null;
      }
   }

   public GpuTexture getTexture() {
      if (this.texture == null) {
         throw new IllegalStateException("Texture does not exist, can't get it before something initializes it");
      } else {
         return this.texture;
      }
   }

   public GpuTextureView getTextureView() {
      if (this.textureView == null) {
         throw new IllegalStateException("Texture view does not exist, can't get it before something initializes it");
      } else {
         return this.textureView;
      }
   }

   public GpuSampler getSampler() {
      return this.sampler;
   }
}
