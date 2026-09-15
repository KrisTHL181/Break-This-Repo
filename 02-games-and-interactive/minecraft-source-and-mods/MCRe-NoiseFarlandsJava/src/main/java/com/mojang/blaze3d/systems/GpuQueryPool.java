package com.mojang.blaze3d.systems;

import java.util.OptionalLong;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GpuQueryPool extends AutoCloseable {
    int size();

    OptionalLong getValue(int index);

    OptionalLong[] getValues(int index, int count);

    @Override
    void close();
}