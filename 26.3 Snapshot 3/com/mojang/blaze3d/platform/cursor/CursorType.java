package com.mojang.blaze3d.platform.cursor;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;

public class CursorType {
   public static final CursorType DEFAULT = new CursorType("default", 0L);
   private final String name;
   private final long handle;

   private CursorType(final String name, final long handle) {
      this.name = name;
      this.handle = handle;
   }

   public void select(final Window window) {
      GLFW.glfwSetCursor(window.handle(), this.handle);
   }

   @Override
   public String toString() {
      return this.name;
   }

   public static CursorType createStandardCursor(final int shape, final String name, final CursorType fallback) {
      long handle = GLFW.glfwCreateStandardCursor(shape);
      return handle == 0L ? fallback : new CursorType(name, handle);
   }
}
