package com.mojang.blaze3d.platform;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.StringDecomposer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class ClipboardManager {
   public static final int FORMAT_UNAVAILABLE = 65545;
   private final ByteBuffer clipboardScratchBuffer = BufferUtils.createByteBuffer(8192);

   public String getClipboard(Window p_429923_, GLFWErrorCallbackI p_83997_) {
      GLFWErrorCallback glfwerrorcallback = GLFW.glfwSetErrorCallback(p_83997_);
      String s = GLFW.glfwGetClipboardString(p_429923_.handle());
      s = s != null ? StringDecomposer.filterBrokenSurrogates(s) : "";
      GLFWErrorCallback glfwerrorcallback1 = GLFW.glfwSetErrorCallback(glfwerrorcallback);
      if (glfwerrorcallback1 != null) {
         glfwerrorcallback1.free();
      }

      return s;
   }

   private static void pushClipboard(Window p_430538_, ByteBuffer p_83993_, byte[] p_83994_) {
      p_83993_.clear();
      p_83993_.put(p_83994_);
      p_83993_.put((byte)0);
      p_83993_.flip();
      GLFW.glfwSetClipboardString(p_430538_.handle(), p_83993_);
   }

   public void setClipboard(Window p_423182_, String p_83990_) {
      byte[] abyte = p_83990_.getBytes(StandardCharsets.UTF_8);
      int i = abyte.length + 1;
      if (i < this.clipboardScratchBuffer.capacity()) {
         pushClipboard(p_423182_, this.clipboardScratchBuffer, abyte);
      } else {
         ByteBuffer bytebuffer = MemoryUtil.memAlloc(i);

         try {
            pushClipboard(p_423182_, bytebuffer, abyte);
         } finally {
            MemoryUtil.memFree(bytebuffer);
         }
      }
   }
}
