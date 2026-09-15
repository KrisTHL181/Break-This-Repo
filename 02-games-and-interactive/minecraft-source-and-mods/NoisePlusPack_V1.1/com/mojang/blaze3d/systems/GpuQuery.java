package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import java.util.OptionalLong;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public interface GpuQuery extends AutoCloseable {
   OptionalLong getValue();

   @Override
   void close();
}
