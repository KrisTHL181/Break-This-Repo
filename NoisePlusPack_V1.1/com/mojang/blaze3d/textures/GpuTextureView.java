package com.mojang.blaze3d.textures;

import com.mojang.blaze3d.DontObfuscate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public abstract class GpuTextureView implements AutoCloseable {
   private final GpuTexture texture;
   private final int baseMipLevel;
   private final int mipLevels;

   public GpuTextureView(GpuTexture p_409038_, int p_405936_, int p_408978_) {
      this.texture = p_409038_;
      this.baseMipLevel = p_405936_;
      this.mipLevels = p_408978_;
   }

   @Override
   public abstract void close();

   public GpuTexture texture() {
      return this.texture;
   }

   public int baseMipLevel() {
      return this.baseMipLevel;
   }

   public int mipLevels() {
      return this.mipLevels;
   }

   public int getWidth(int p_409315_) {
      return this.texture.getWidth(p_409315_ + this.baseMipLevel);
   }

   public int getHeight(int p_408169_) {
      return this.texture.getHeight(p_408169_ + this.baseMipLevel);
   }

   public abstract boolean isClosed();
}
