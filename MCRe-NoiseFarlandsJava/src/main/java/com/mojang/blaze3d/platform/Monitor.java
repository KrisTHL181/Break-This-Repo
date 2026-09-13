package com.mojang.blaze3d.platform;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.GLFWErrorScope;
import com.mojang.logging.LogUtils;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVidMode.Buffer;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record Monitor(String monitorName, long monitor, List<VideoMode> videoModes, VideoMode currentMode, int x, int y) {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

    public static @Nullable Monitor tryCreate(final long monitor) {
        GLFWErrorCapture glfwErrors = new GLFWErrorCapture();

        try (GLFWErrorScope var3 = new GLFWErrorScope(glfwErrors)) {
            String monitorName = queryMonitorName(monitor);
            Builder<VideoMode> videoModes = ImmutableList.builder();
            Buffer modes = GLFW.glfwGetVideoModes(monitor);
            if (modes == null) {
                LOGGER.warn("Failed to query video modes of monitor {}", monitorName);
                return null;
            }

            for (int i = modes.limit() - 1; i >= 0; i--) {
                modes.position(i);
                VideoMode mode = new VideoMode(modes);
                if (mode.getRedBits() >= 8 && mode.getGreenBits() >= 8 && mode.getBlueBits() >= 8) {
                    videoModes.add(mode);
                }
            }

            int[] x = new int[1];
            int[] y = new int[1];
            GLFW.glfwGetMonitorPos(monitor, x, y);
            GLFWVidMode currentMode = GLFW.glfwGetVideoMode(monitor);
            if (currentMode == null) {
                LOGGER.warn("Failed to query current video mode of monitor {}", monitorName);
                return null;
            } else {
                return new Monitor(monitorName, monitor, videoModes.build(), new VideoMode(currentMode), x[0], y[0]);
            }
        } finally {
            for (GLFWErrorCapture.Error error : glfwErrors) {
                LOGGER.error("GLFW error collected during monitor 0x{} query: {}", HEX_FORMAT.toHexDigits(monitor), error);
            }
        }
    }

    private static String queryMonitorName(final long monitor) {
        String monitorName = Objects.requireNonNull(GLFW.glfwGetMonitorName(monitor), "unknown");
        return monitorName + "[0x" + HEX_FORMAT.toHexDigits(monitor) + "]";
    }

    public VideoMode getPreferredVidMode(final Optional<VideoMode> expectedMode) {
        if (expectedMode.isPresent()) {
            VideoMode videoMode = expectedMode.get();

            for (VideoMode mode : this.videoModes) {
                if (mode.equals(videoMode)) {
                    return mode;
                }
            }
        }

        return this.currentMode;
    }

    public int indexOfMode(final VideoMode videoMode) {
        return this.videoModes.indexOf(videoMode);
    }

    public VideoMode mode(final int mode) {
        return this.videoModes.get(mode);
    }

    public int modeCount() {
        return this.videoModes.size();
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s(%s at (%d,%d))", this.monitorName, this.currentMode, this.x, this.y);
    }
}