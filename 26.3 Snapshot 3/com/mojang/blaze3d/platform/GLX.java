package com.mojang.blaze3d.platform;

import com.google.common.base.Joiner;
import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.GLFWErrorScope;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import org.jspecify.annotations.Nullable;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class GLX {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static int glfwPlatformType = 393221;
   private static @Nullable String cpuInfo;

   public static int _getRefreshRate(final Window window) {
      RenderSystem.assertOnRenderThread();
      long monitor = GLFW.glfwGetWindowMonitor(window.handle());
      if (monitor == 0L) {
         monitor = GLFW.glfwGetPrimaryMonitor();
      }

      GLFWVidMode videoMode = monitor == 0L ? null : GLFW.glfwGetVideoMode(monitor);
      return videoMode == null ? 0 : videoMode.refreshRate();
   }

   public static String _getLWJGLVersion() {
      return Version.getVersion();
   }

   public static LongSupplier _initGlfw() {
      Window.checkGlfwError((errorx, description) -> {
         throw new IllegalStateException(String.format(Locale.ROOT, "GLFW error before init: [0x%X]%s", errorx, description));
      });
      GLFWErrorCapture collectedErrors = new GLFWErrorCapture();

      LongSupplier timeSource;
      try (GLFWErrorScope var2 = new GLFWErrorScope(collectedErrors)) {
         if (GLFW.glfwPlatformSupported(393219) && GLFW.glfwPlatformSupported(393220) && !SharedConstants.DEBUG_PREFER_WAYLAND) {
            GLFW.glfwInitHint(327683, 393220);
         }

         if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW, errors: " + Joiner.on(",").join(collectedErrors));
         }

         timeSource = () -> (long)(GLFW.glfwGetTime() * 1.0E9);
         glfwPlatformType = GLFW.glfwGetPlatform();
      }

      for (GLFWErrorCapture.Error error : collectedErrors) {
         LOGGER.error("GLFW error collected during initialization: {}", error);
      }

      return timeSource;
   }

   public static int getGlfwPlatform() {
      return glfwPlatformType;
   }

   public static void _setGlfwErrorCallback(final GLFWErrorCallbackI onFullscreenError) {
      GLFWErrorCallback previousCallback = GLFW.glfwSetErrorCallback(onFullscreenError);
      if (previousCallback != null) {
         previousCallback.free();
      }
   }

   public static boolean _shouldClose(final Window window) {
      return GLFW.glfwWindowShouldClose(window.handle());
   }

   public static String _getCpuInfo() {
      if (cpuInfo == null) {
         cpuInfo = "<unknown>";

         try {
            CentralProcessor processor = new SystemInfo().getHardware().getProcessor();
            cpuInfo = String.format(Locale.ROOT, "%dx %s", processor.getLogicalProcessorCount(), processor.getProcessorIdentifier().getName())
               .replaceAll("\\s+", " ");
         } catch (Throwable var1) {
         }
      }

      return cpuInfo;
   }

   public static <T> T make(final Supplier<T> factory) {
      return factory.get();
   }

   public static int glfwBool(final boolean value) {
      return value ? 1 : 0;
   }
}
