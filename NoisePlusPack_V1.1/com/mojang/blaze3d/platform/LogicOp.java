package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.DontObfuscate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public enum LogicOp {
   NONE,
   OR_REVERSE;
}
