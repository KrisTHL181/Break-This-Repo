package com.mojang.blaze3d.platform.cursor;

import com.mojang.blaze3d.platform.Window;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class CursorType {
   public static final CursorType DEFAULT = new CursorType("default", 0L);
   private final String name;
   private final long handle;

   private CursorType(String p_427269_, long p_429018_) {
      this.name = p_427269_;
      this.handle = p_429018_;
   }

   public void select(Window p_422842_) {
      GLFW.glfwSetCursor(p_422842_.handle(), this.handle);
   }

   @Override
   public String toString() {
      return this.name;
   }

   public static CursorType createStandardCursor(int p_431672_, String p_426543_, CursorType p_430206_) {
      long i = GLFW.glfwCreateStandardCursor(p_431672_);
      return i == 0L ? p_430206_ : new CursorType(p_426543_, i);
   }
}
