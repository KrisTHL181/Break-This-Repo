package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.GLFWErrorScope;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuBackend;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

public final class Window implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();
   public static final int BASE_WIDTH = 320;
   public static final int BASE_HEIGHT = 240;
   private final GLFWErrorCallback defaultErrorCallback = GLFWErrorCallback.create(this::defaultErrorCallback);
   private final WindowEventHandler eventHandler;
   private final MonitorManager monitorManager;
   private final long handle;
   private int windowedX;
   private int windowedY;
   private int windowedWidth;
   private int windowedHeight;
   private Optional<VideoMode> preferredFullscreenVideoMode;
   private boolean fullscreen;
   private boolean actuallyFullscreen;
   private int x;
   private int y;
   private int width;
   private int height;
   private int framebufferWidth;
   private int framebufferHeight;
   private int guiScaledWidth;
   private int guiScaledHeight;
   private int guiScale;
   private String errorSection = "Startup";
   private boolean dirty;
   private boolean vsync;
   private boolean iconified;
   private boolean focused = true;
   private boolean minimized;
   private boolean allowCursorChanges;
   private CursorType currentCursor = CursorType.DEFAULT;
   private final boolean exclusiveFullscreen;
   private final GpuBackend backend;

   public Window(
      final WindowEventHandler eventHandler,
      final DisplayData displayData,
      final @Nullable String fullscreenVideoModeString,
      final boolean exclusiveFullscreen,
      final String title,
      final MonitorManager monitorManager,
      final GpuBackend backend
   ) throws BackendCreationException {
      this.monitorManager = monitorManager;
      this.exclusiveFullscreen = exclusiveFullscreen;
      this.eventHandler = eventHandler;
      Optional<VideoMode> optionsMode = VideoMode.read(fullscreenVideoModeString);
      if (optionsMode.isPresent()) {
         this.preferredFullscreenVideoMode = optionsMode;
      } else if (displayData.fullscreenWidth().isPresent() && displayData.fullscreenHeight().isPresent()) {
         this.preferredFullscreenVideoMode = Optional.of(
            new VideoMode(displayData.fullscreenWidth().getAsInt(), displayData.fullscreenHeight().getAsInt(), 8, 8, 8, 60)
         );
      } else {
         this.preferredFullscreenVideoMode = Optional.empty();
      }

      this.actuallyFullscreen = this.fullscreen = displayData.isFullscreen();
      Monitor initialMonitor = monitorManager.getMonitor(GLFW.glfwGetPrimaryMonitor());
      this.windowedWidth = this.width = allowedWindowMinSize(displayData.width());
      this.windowedHeight = this.height = allowedWindowMinSize(displayData.height());
      this.handle = this.createWindow(backend, this.width, this.height, title, this.fullscreen && initialMonitor != null ? initialMonitor.monitor() : 0L);
      this.backend = backend;
      if (initialMonitor != null) {
         VideoMode mode = initialMonitor.getPreferredVidMode(this.fullscreen ? this.preferredFullscreenVideoMode : Optional.empty());
         this.windowedX = this.x = initialMonitor.x() + mode.getWidth() / 2 - this.width / 2;
         this.windowedY = this.y = initialMonitor.y() + mode.getHeight() / 2 - this.height / 2;
      } else {
         int[] actualX = new int[1];
         int[] actualY = new int[1];
         GLFW.glfwGetWindowPos(this.handle, actualX, actualY);
         this.windowedX = this.x = actualX[0];
         this.windowedY = this.y = actualY[0];
      }

      this.setMode();
      this.refreshFramebufferSize();
      GLFW.glfwSetFramebufferSizeCallback(this.handle, this::onFramebufferResize);
      GLFW.glfwSetWindowPosCallback(this.handle, this::onMove);
      GLFW.glfwSetWindowSizeCallback(this.handle, this::onResize);
      GLFW.glfwSetWindowFocusCallback(this.handle, this::onFocus);
      GLFW.glfwSetCursorEnterCallback(this.handle, this::onEnter);
      GLFW.glfwSetWindowIconifyCallback(this.handle, this::onIconify);
   }

   public static long createGlfwWindow(final int width, final int height, final String title, final long monitor, final GpuBackend backend) throws BackendCreationException {
      GLFWErrorCapture glfwErrors = new GLFWErrorCapture();

      long windowHandle;
      try (GLFWErrorScope var9 = new GLFWErrorScope(glfwErrors)) {
         backend.setWindowHints();
         GLFW.glfwWindowHint(131076, 0);
         windowHandle = GLFW.glfwCreateWindow(width, height, title, monitor, 0L);
         if (windowHandle == 0L) {
            backend.handleWindowCreationErrors(glfwErrors.firstError());
         }
      }

      for (GLFWErrorCapture.Error error : glfwErrors) {
         LOGGER.error("GLFW error collected during GL backend initialization: {}", error);
      }

      return windowHandle;
   }

   private long createWindow(final GpuBackend backend, final int width, final int height, final String title, final long initialMonitor) throws BackendCreationException {
      return createGlfwWindow(width, height, title, initialMonitor, backend);
   }

   public static String getPlatform() {
      int platform = GLX.getGlfwPlatform();

      return switch (platform) {
         case 0 -> "<error>";
         case 393217 -> "win32";
         case 393218 -> "cocoa";
         case 393219 -> "wayland";
         case 393220 -> "x11";
         case 393221 -> "null";
         default -> String.format(Locale.ROOT, "unknown (%08X)", platform);
      };
   }

   public int getRefreshRate() {
      RenderSystem.assertOnRenderThread();
      return GLX._getRefreshRate(this);
   }

   public boolean shouldClose() {
      return GLX._shouldClose(this);
   }

   public static void checkGlfwError(final BiConsumer<Integer, String> errorConsumer) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         PointerBuffer errorDescription = stack.mallocPointer(1);
         int errorCode = GLFW.glfwGetError(errorDescription);
         if (errorCode != 0) {
            long errorDescriptionAddress = errorDescription.get();
            String errorMessage = errorDescriptionAddress == 0L ? "" : MemoryUtil.memUTF8(errorDescriptionAddress);
            errorConsumer.accept(errorCode, errorMessage);
         }
      } catch (Throwable var8) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (stack != null) {
         stack.close();
      }
   }

   public void setIcon(final PackResources resources, final IconSet iconSet) throws IOException {
      int platform = GLX.getGlfwPlatform();
      switch (platform) {
         case 393217:
         case 393220:
            List<IoSupplier<InputStream>> iconStreams = iconSet.getStandardIcons(resources);
            List<ByteBuffer> allocatedBuffers = new ArrayList<>(iconStreams.size());

            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  Buffer icons = GLFWImage.malloc(iconStreams.size(), stack);

                  for (int i = 0; i < iconStreams.size(); i++) {
                     try (NativeImage image = NativeImage.read(iconStreams.get(i).get())) {
                        ByteBuffer pixels = MemoryUtil.memAlloc(image.getWidth() * image.getHeight() * 4);
                        allocatedBuffers.add(pixels);
                        pixels.asIntBuffer().put(image.getPixelsABGR());
                        icons.position(i);
                        icons.width(image.getWidth());
                        icons.height(image.getHeight());
                        icons.pixels(pixels);
                     }
                  }

                  GLFW.glfwSetWindowIcon(this.handle, (Buffer)icons.position(0));
               } catch (Throwable var21) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var18) {
                        var21.addSuppressed(var18);
                     }
                  }

                  throw var21;
               }

               if (stack != null) {
                  stack.close();
               }
               break;
            } finally {
               allocatedBuffers.forEach(MemoryUtil::memFree);
            }
         case 393218:
            MacosUtil.loadIcon(iconSet.getMacIcon(resources));
         case 393219:
         case 393221:
            break;
         default:
            LOGGER.warn("Not setting icon for unrecognized platform: {}", platform);
      }
   }

   public void setErrorSection(final String string) {
      this.errorSection = string;
   }

   public static void setBootErrorCallback() {
      GLFW.glfwSetErrorCallback(Window::bootCrash);
   }

   private static void bootCrash(final int error, final long description) {
      String message = "Unexpected GLFW error " + HEX_FORMAT.toHexDigits(error) + ": " + MemoryUtil.memUTF8(description);
      throw new IllegalStateException(message);
   }

   public void defaultErrorCallback(final int errorCode, final long description) {
      if (!RenderSystem.isOnRenderThread()) {
         LOGGER.error("Error callback from invalid thread ({})", Thread.currentThread().getName());
      }

      String errorString = MemoryUtil.memUTF8(description);
      LOGGER.error("GLFW error @ {}: {}[{}]", new Object[]{this.errorSection, errorString, HEX_FORMAT.toHexDigits(errorCode)});
   }

   public void setDefaultErrorCallback() {
      GLFWErrorCallback previousCallback = GLFW.glfwSetErrorCallback(this.defaultErrorCallback);
      if (previousCallback != null) {
         previousCallback.free();
      }
   }

   @Override
   public void close() {
      RenderSystem.assertOnRenderThread();
      Callbacks.glfwFreeCallbacks(this.handle);
      GLFW.glfwSetErrorCallback(null);
      this.defaultErrorCallback.close();
      GLFW.glfwDestroyWindow(this.handle);
   }

   private void onMove(final long handle, final int x, final int y) {
      this.x = x;
      this.y = y;
   }

   private boolean isSoftScreen() {
      return Util.getPlatform() == Util.OS.WINDOWS && this.fullscreen && !this.exclusiveFullscreen;
   }

   private void onFramebufferResize(final long handle, final int newWidth, final int newHeight) {
      if (handle == this.handle) {
         int oldWidth = this.getWidth();
         int oldHeight = this.getHeight();
         if (newWidth != 0 && newHeight != 0) {
            int newFrameBufferHeight = this.isSoftScreen() ? newHeight - 1 : newHeight;
            if (newWidth != oldWidth || newFrameBufferHeight != oldHeight || this.minimized) {
               this.minimized = false;
               this.framebufferWidth = newWidth;
               this.framebufferHeight = newFrameBufferHeight;

               try {
                  this.eventHandler.framebufferSizeChanged();
               } catch (Exception e) {
                  CrashReport report = CrashReport.forThrowable(e, "Window resize");
                  CrashReportCategory windowSizeDetails = report.addCategory("Window Dimensions");
                  windowSizeDetails.setDetail("Old", oldWidth + "x" + oldHeight);
                  windowSizeDetails.setDetail("New", newWidth + "x" + newHeight);
                  throw new ReportedException(report);
               }
            }
         } else {
            this.minimized = true;
         }
      }
   }

   private void refreshFramebufferSize() {
      int[] outWidth = new int[1];
      int[] outHeight = new int[1];
      GLFW.glfwGetFramebufferSize(this.handle, outWidth, outHeight);
      outHeight[0] = this.isSoftScreen() ? outHeight[0] - 1 : outHeight[0];
      this.framebufferWidth = outWidth[0] > 0 ? outWidth[0] : 1;
      this.framebufferHeight = outHeight[0] > 0 ? outHeight[0] : 1;
   }

   private void onResize(final long handle, final int newWidth, final int newHeight) {
      this.width = newWidth;
      this.height = newHeight;
   }

   private void onFocus(final long handle, final boolean focused) {
      if (handle == this.handle) {
         this.focused = focused;
      }
   }

   private void onEnter(final long handle, final boolean entered) {
      if (entered) {
         this.eventHandler.cursorEntered();
      }
   }

   private void onIconify(final long handle, final boolean iconified) {
      this.iconified = iconified;
   }

   public void updateFullscreenIfChanged() {
      if (this.fullscreen != this.actuallyFullscreen) {
         this.actuallyFullscreen = this.fullscreen;

         try {
            this.setMode();
            this.eventHandler.framebufferSizeChanged();
         } catch (Exception e) {
            LOGGER.error("Couldn't toggle fullscreen", e);
         }
      }
   }

   public Optional<VideoMode> getPreferredFullscreenVideoMode() {
      return this.preferredFullscreenVideoMode;
   }

   public void setPreferredFullscreenVideoMode(final Optional<VideoMode> preferredFullscreenVideoMode) {
      boolean changed = !preferredFullscreenVideoMode.equals(this.preferredFullscreenVideoMode);
      this.preferredFullscreenVideoMode = preferredFullscreenVideoMode;
      if (changed) {
         this.dirty = true;
      }
   }

   public void changeFullscreenVideoMode() {
      if (this.fullscreen && this.dirty) {
         this.dirty = false;
         this.setMode();
         this.eventHandler.framebufferSizeChanged();
      }
   }

   private void setMode() {
      boolean wasFullscreen = GLFW.glfwGetWindowMonitor(this.handle) != 0L;
      if (this.fullscreen) {
         Monitor monitor = this.monitorManager.findBestMonitor(this);
         if (monitor == null) {
            LOGGER.warn("Failed to find suitable monitor for fullscreen mode");
            this.fullscreen = false;
         } else {
            if (MacosUtil.IS_MACOS) {
               MacosUtil.exitNativeFullscreen(this);
            }

            VideoMode mode = monitor.getPreferredVidMode(this.preferredFullscreenVideoMode);
            if (!wasFullscreen) {
               this.windowedX = this.x;
               this.windowedY = this.y;
               this.windowedWidth = allowedWindowMinSize(this.width);
               this.windowedHeight = allowedWindowMinSize(this.height);
            }

            this.x = 0;
            this.y = 0;
            this.width = allowedWindowMinSize(mode.getWidth());
            this.height = allowedWindowMinSize(mode.getHeight());
            GLFW.glfwSetWindowMonitor(this.handle, monitor.monitor(), this.x, this.y, this.width, this.height, mode.getRefreshRate());
            if (MacosUtil.IS_MACOS) {
               MacosUtil.clearResizableBit(this);
            }
         }
      } else {
         this.x = this.windowedX;
         this.y = this.windowedY;
         this.width = allowedWindowMinSize(this.windowedWidth);
         this.height = allowedWindowMinSize(this.windowedHeight);
         GLFW.glfwSetWindowMonitor(this.handle, 0L, this.x, this.y, this.width, this.height, -1);
      }
   }

   public void toggleFullScreen() {
      this.fullscreen = !this.fullscreen;
   }

   public void setWindowed(final int width, final int height) {
      this.windowedWidth = allowedWindowMinSize(width);
      this.windowedHeight = allowedWindowMinSize(height);
      this.fullscreen = false;
      this.setMode();
   }

   public int calculateScale(final int maxScale, final boolean enforceUnicode) {
      int scale = 1;

      while (
         scale != maxScale
            && scale < this.framebufferWidth
            && scale < this.framebufferHeight
            && this.framebufferWidth / (scale + 1) >= 320
            && this.framebufferHeight / (scale + 1) >= 240
      ) {
         scale++;
      }

      if (enforceUnicode && scale % 2 != 0) {
         scale++;
      }

      return scale;
   }

   public void setGuiScale(final int guiScale) {
      this.guiScale = guiScale;
      double doubleGuiScale = guiScale;
      int width = (int)(this.framebufferWidth / doubleGuiScale);
      this.guiScaledWidth = this.framebufferWidth / doubleGuiScale > width ? width + 1 : width;
      int height = (int)(this.framebufferHeight / doubleGuiScale);
      this.guiScaledHeight = this.framebufferHeight / doubleGuiScale > height ? height + 1 : height;
   }

   public void setTitle(final String title) {
      GLFW.glfwSetWindowTitle(this.handle, title);
   }

   public long handle() {
      return this.handle;
   }

   public boolean isFullscreen() {
      return this.fullscreen;
   }

   public boolean isIconified() {
      return this.iconified;
   }

   public boolean isFocused() {
      return this.focused;
   }

   public int getWidth() {
      return this.framebufferWidth;
   }

   public int getHeight() {
      return this.framebufferHeight;
   }

   public void setWidth(final int width) {
      this.framebufferWidth = width;
   }

   public void setHeight(final int height) {
      this.framebufferHeight = height;
   }

   public int getScreenWidth() {
      return this.width;
   }

   public int getScreenHeight() {
      return this.height;
   }

   public int getGuiScaledWidth() {
      return this.guiScaledWidth;
   }

   public int getGuiScaledHeight() {
      return this.guiScaledHeight;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getGuiScale() {
      return this.guiScale;
   }

   public @Nullable Monitor findBestMonitor() {
      return this.monitorManager.findBestMonitor(this);
   }

   public void updateRawMouseInput(final boolean value) {
      InputConstants.updateRawMouseInput(this, value);
   }

   public void setWindowCloseCallback(final Runnable task) {
      GLFWWindowCloseCallback prev = GLFW.glfwSetWindowCloseCallback(this.handle, id -> task.run());
      if (prev != null) {
         prev.free();
      }
   }

   public boolean isMinimized() {
      return this.minimized;
   }

   public void setAllowCursorChanges(final boolean value) {
      this.allowCursorChanges = value;
   }

   public void selectCursor(final CursorType cursor) {
      CursorType effectiveCursor = this.allowCursorChanges ? cursor : CursorType.DEFAULT;
      if (this.currentCursor != effectiveCursor) {
         this.currentCursor = effectiveCursor;
         effectiveCursor.select(this);
      }
   }

   public float getAppropriateLineWidth() {
      return Math.max(2.5F, this.getWidth() / 1920.0F * 2.5F);
   }

   public GpuBackend backend() {
      return this.backend;
   }

   private static int allowedWindowMinSize(final int size) {
      return Math.max(size, 1);
   }

   public static class WindowInitFailed extends SilentInitException {
      public WindowInitFailed(final String message) {
         super(message);
      }
   }
}
