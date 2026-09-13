package com.mojang.blaze3d.shaders;

import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface ShaderSource {
   @Nullable String get(Identifier var1, ShaderType var2);
}
