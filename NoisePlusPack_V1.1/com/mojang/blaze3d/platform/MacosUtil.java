package com.mojang.blaze3d.platform;

import ca.weblite.objc.Client;
import ca.weblite.objc.NSObject;
import com.sun.jna.Pointer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFWNativeCocoa;

@OnlyIn(Dist.CLIENT)
public class MacosUtil {
   public static final boolean IS_MACOS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");
   private static final int NS_RESIZABLE_WINDOW_MASK = 8;
   private static final int NS_FULL_SCREEN_WINDOW_MASK = 16384;

   public static void exitNativeFullscreen(Window p_422925_) {
      getNsWindow(p_422925_).filter(MacosUtil::isInNativeFullscreen).ifPresent(MacosUtil::toggleNativeFullscreen);
   }

   public static void clearResizableBit(Window p_431140_) {
      getNsWindow(p_431140_).ifPresent(p_312903_ -> {
         long i = getStyleMask(p_312903_);
         p_312903_.send("setStyleMask:", new Object[]{i & -9L});
      });
   }

   private static Optional<NSObject> getNsWindow(Window p_424671_) {
      long i = GLFWNativeCocoa.glfwGetCocoaWindow(p_424671_.handle());
      return i != 0L ? Optional.of(new NSObject(new Pointer(i))) : Optional.empty();
   }

   private static boolean isInNativeFullscreen(NSObject p_311944_) {
      return (getStyleMask(p_311944_) & 16384L) != 0L;
   }

   private static long getStyleMask(NSObject p_309879_) {
      return (Long)p_309879_.sendRaw("styleMask", new Object[0]);
   }

   private static void toggleNativeFullscreen(NSObject p_182524_) {
      p_182524_.send("toggleFullScreen:", new Object[]{Pointer.NULL});
   }

   public static void loadIcon(IoSupplier<InputStream> p_250929_) throws IOException {
      try (InputStream inputstream = p_250929_.get()) {
         String s = Base64.getEncoder().encodeToString(inputstream.readAllBytes());
         Client client = Client.getInstance();
         Object object = client.sendProxy("NSData", "alloc", new Object[0]).send("initWithBase64Encoding:", new Object[]{s});
         Object object1 = client.sendProxy("NSImage", "alloc", new Object[0]).send("initWithData:", new Object[]{object});
         client.sendProxy("NSApplication", "sharedApplication", new Object[0]).send("setApplicationIconImage:", new Object[]{object1});
      }
   }
}
