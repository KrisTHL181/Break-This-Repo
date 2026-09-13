package com.mojang.renderpearl.api.textures;

import java.util.OptionalDouble;

public abstract class GpuSampler implements AutoCloseable {
   public abstract AddressMode getAddressModeU();

   public abstract AddressMode getAddressModeV();

   public abstract FilterMode getMinFilter();

   public abstract FilterMode getMagFilter();

   public abstract int getMaxAnisotropy();

   public abstract OptionalDouble getMaxLod();

   @Override
   public abstract void close();
}
