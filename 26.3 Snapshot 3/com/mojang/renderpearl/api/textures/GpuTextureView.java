package com.mojang.renderpearl.api.textures;

public abstract class GpuTextureView implements AutoCloseable {
   private final GpuTexture texture;
   private final int baseMipLevel;
   private final int mipLevels;

   protected GpuTextureView(final GpuTexture texture, final int baseMipLevel, final int mipLevels) {
      this.texture = texture;
      this.baseMipLevel = baseMipLevel;
      this.mipLevels = mipLevels;
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

   public int getWidth(final int mipLevel) {
      return this.texture.getWidth(mipLevel + this.baseMipLevel);
   }

   public int getHeight(final int mipLevel) {
      return this.texture.getHeight(mipLevel + this.baseMipLevel);
   }

   public abstract boolean isClosed();
}
