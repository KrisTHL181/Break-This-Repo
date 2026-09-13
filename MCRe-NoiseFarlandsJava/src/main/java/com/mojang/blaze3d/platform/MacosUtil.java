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

    public static void exitNativeFullscreen(final Window window) {
        getNsWindow(window).filter(MacosUtil::isInNativeFullscreen).ifPresent(MacosUtil::toggleNativeFullscreen);
    }

    public static void clearResizableBit(final Window window) {
        getNsWindow(window).ifPresent(nsWindow -> {
            long styleMask = getStyleMask(nsWindow);
            nsWindow.send("setStyleMask:", styleMask & -9L);
        });
    }

    private static Optional<NSObject> getNsWindow(final Window window) {
        return getNsWindow(window.handle());
    }

    private static Optional<NSObject> getNsWindow(final long windowHandle) {
        long nsWindow = GLFWNativeCocoa.glfwGetCocoaWindow(windowHandle);
        return nsWindow != 0L ? Optional.of(new NSObject(new Pointer(nsWindow))) : Optional.empty();
    }

    private static boolean isInNativeFullscreen(final NSObject nsWindow) {
        return (getStyleMask(nsWindow) & 16384L) != 0L;
    }

    private static long getStyleMask(final NSObject nsWindow) {
        return (Long)nsWindow.sendRaw("styleMask");
    }

    private static void toggleNativeFullscreen(final NSObject nsWindow) {
        nsWindow.send("toggleFullScreen:", Pointer.NULL);
    }

    public static void loadIcon(final IoSupplier<InputStream> icon) throws IOException {
        try (InputStream iconStream = icon.get()) {
            String base64Icon = Base64.getEncoder().encodeToString(iconStream.readAllBytes());
            Client objc = Client.getInstance();
            Object data = objc.sendProxy("NSData", "alloc").send("initWithBase64Encoding:", base64Icon);
            Object image = objc.sendProxy("NSImage", "alloc").send("initWithData:", data);
            objc.sendProxy("NSApplication", "sharedApplication").send("setApplicationIconImage:", image);
        }
    }

    public static void setWindowColorSpaceForOpenGLBecauseGLFWDoesnt(final long glfwWindowHandle) {
        getNsWindow(glfwWindowHandle).ifPresent(nsWindow -> {
            Object sRGBColorSpace = nsWindow.getClient().send("NSColorSpace", "sRGBColorSpace");
            nsWindow.send("setColorSpace:", sRGBColorSpace);
        });
    }
}