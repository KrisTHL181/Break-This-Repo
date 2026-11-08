package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.DontObfuscate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public enum DepthTestFunction {
   NO_DEPTH_TEST,
   EQUAL_DEPTH_TEST,
   LEQUAL_DEPTH_TEST,
   LESS_DEPTH_TEST,
   GREATER_DEPTH_TEST;
}
