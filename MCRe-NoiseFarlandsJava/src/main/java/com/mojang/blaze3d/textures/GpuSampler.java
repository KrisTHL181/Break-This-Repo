package com.mojang.blaze3d.textures;

import java.util.OptionalDouble;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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