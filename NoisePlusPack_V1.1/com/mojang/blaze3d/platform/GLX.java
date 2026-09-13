package com.mojang.blaze3d.platform;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class GLX {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static @Nullable String cpuInfo;

   public static int _getRefreshRate(Window p_69342_) {
      RenderSystem.assertOnRenderThread();
      long i = GLFW.glfwGetWindowMonitor(p_69342_.handle());
      if (i == 0L) {
         i = GLFW.glfwGetPrimaryMonitor();
      }

      GLFWVidMode glfwvidmode = i == 0L ? null : GLFW.glfwGetVideoMode(i);
      return glfwvidmode == null ? 0 : glfwvidmode.refreshRate();
   }

   public static String _getLWJGLVersion() {
      return Version.getVersion();
   }

   public static LongSupplier _initGlfw() {
      Window.checkGlfwError((p_242032_, p_242033_) -> {
         throw new IllegalStateException(String.format(Locale.ROOT, "GLFW error before init: [0x%X]%s", p_242032_, p_242033_));
      });
      List<String> list = Lists.newArrayList();
      GLFWErrorCallback glfwerrorcallback = GLFW.glfwSetErrorCallback((p_69365_, p_69366_) -> {
         String s1 = p_69366_ == 0L ? "" : MemoryUtil.memUTF8(p_69366_);
         list.add(String.format(Locale.ROOT, "GLFW error during init: [0x%X]%s", p_69365_, s1));
      });
      if (!GLFW.glfwInit()) {
         throw new IllegalStateException("Failed to initialize GLFW, errors: " + Joiner.on(",").join(list));
      }

      LongSupplier longsupplier = () -> (long)(GLFW.glfwGetTime() * 1.0E9);

      for (String s : list) {
         LOGGER.error("GLFW error collected during initialization: {}", s);
      }

      RenderSystem.setErrorCallback(glfwerrorcallback);
      return longsupplier;
   }

   public static void _setGlfwErrorCallback(GLFWErrorCallbackI p_69353_) {
      GLFWErrorCallback glfwerrorcallback = GLFW.glfwSetErrorCallback(p_69353_);
      if (glfwerrorcallback != null) {
         glfwerrorcallback.free();
      }
   }

   public static boolean _shouldClose(Window p_69356_) {
      return GLFW.glfwWindowShouldClose(p_69356_.handle());
   }

   public static String _getCpuInfo() {
      if (cpuInfo == null) {
         cpuInfo = "<unknown>";

         try {
            CentralProcessor centralprocessor = new SystemInfo().getHardware().getProcessor();
            cpuInfo = String.format(Locale.ROOT, "%dx %s", centralprocessor.getLogicalProcessorCount(), centralprocessor.getProcessorIdentifier().getName())
               .replaceAll("\\s+", " ");
         } catch (Throwable var1) {
         }
      }

      return cpuInfo;
   }

   public static <T> T make(Supplier<T> p_69374_) {
      return p_69374_.get();
   }

   public static <T> T make(T p_69371_, Consumer<T> p_69372_) {
      p_69372_.accept(p_69371_);
      return p_69371_;
   }
}
