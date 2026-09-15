package com.mojang.blaze3d;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class Blaze3D {
    public static void youJustLostTheGame() {
        MemoryUtil.memSet(0L, 0, 1L);
    }

    public static double getTime() {
        return GLFW.glfwGetTime();
    }

    private Blaze3D() {
    }
}