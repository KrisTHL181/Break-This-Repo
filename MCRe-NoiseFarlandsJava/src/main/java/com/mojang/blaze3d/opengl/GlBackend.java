package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;
import java.util.Locale;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class GlBackend implements GpuBackend {
    private static final int VERSION_MAJOR = 3;
    private static final int VERSION_MINOR = 3;

    @Override
    public String getName() {
        return "OpenGL";
    }

    @Override
    public void setWindowHints() {
        GLFW.glfwWindowHint(139265, 196609);
        GLFW.glfwWindowHint(139275, 221185);
        GLFW.glfwWindowHint(139266, 3);
        GLFW.glfwWindowHint(139267, 3);
        GLFW.glfwWindowHint(139272, 204801);
        GLFW.glfwWindowHint(139270, 1);
    }

    @Override
    public void handleWindowCreationErrors(final GLFWErrorCapture.@Nullable Error error) throws BackendCreationException {
        if (error != null) {
            if (error.error() == 65542) {
                throw new BackendCreationException("Driver does not support OpenGL", BackendCreationException.Reason.OPENGL_MISSING);
            } else if (error.error() == 65543) {
                throw new BackendCreationException("Driver does not support OpenGL 3.3", BackendCreationException.Reason.OPENGL_MISSING);
            } else {
                throw new BackendCreationException(
                    String.format(Locale.ROOT, "GLFW_ERROR: 0x%X", error.error()), BackendCreationException.Reason.OPENGL_MISSING
                );
            }
        } else {
            throw new BackendCreationException("Failed to create window with OpenGL context", BackendCreationException.Reason.OPENGL_MISSING);
        }
    }

    @Override
    public GpuDevice createDevice(
        final long window, final ShaderSource defaultShaderSource, final GpuDebugOptions debugOptions, final Runnable criticalShaderLoader
    ) {
        if (MacosUtil.IS_MACOS) {
            MacosUtil.setWindowColorSpaceForOpenGLBecauseGLFWDoesnt(window);
        }

        return new GpuDevice(new GlDevice(window, defaultShaderSource, debugOptions), criticalShaderLoader);
    }
}