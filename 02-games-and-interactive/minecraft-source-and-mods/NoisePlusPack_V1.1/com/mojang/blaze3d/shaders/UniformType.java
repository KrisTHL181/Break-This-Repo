package com.mojang.blaze3d.shaders;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum UniformType {
   UNIFORM_BUFFER("ubo"),
   TEXEL_BUFFER("utb");

   final String name;

   UniformType(final String p_392368_) {
      this.name = p_392368_;
   }
}
