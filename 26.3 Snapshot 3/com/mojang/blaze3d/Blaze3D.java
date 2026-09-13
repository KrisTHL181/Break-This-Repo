package com.mojang.blaze3d;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

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
