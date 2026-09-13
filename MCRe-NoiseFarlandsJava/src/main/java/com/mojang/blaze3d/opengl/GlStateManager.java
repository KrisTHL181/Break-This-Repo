package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.jtracy.Plot;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.IntStream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class GlStateManager {
    private static final Plot PLOT_TEXTURES = TracyClient.createPlot("GPU Textures");
    private static int numTextures = 0;
    private static final Plot PLOT_BUFFERS = TracyClient.createPlot("GPU Buffers");
    private static int numBuffers = 0;
    private static final GlStateManager.BlendState[] BLEND = new GlStateManager.BlendState[8];
    private static final GlStateManager.DepthState DEPTH = new GlStateManager.DepthState();
    private static final GlStateManager.CullState CULL = new GlStateManager.CullState();
    private static final GlStateManager.PolygonOffsetState POLY_OFFSET = new GlStateManager.PolygonOffsetState();
    private static final GlStateManager.ColorLogicState COLOR_LOGIC = new GlStateManager.ColorLogicState();
    private static final GlStateManager.ScissorState SCISSOR = new GlStateManager.ScissorState();
    private static int activeTexture;
    private static final int TEXTURE_COUNT = 12;
    private static final GlStateManager.TextureState[] TEXTURES = IntStream.range(0, 12)
        .mapToObj(i -> new GlStateManager.TextureState())
        .toArray(GlStateManager.TextureState[]::new);
    private static final @ColorTargetState.WriteMask int[] COLOR_MASK = new int[8];
    private static int readFbo;
    private static int writeFbo;

    public static void _disableScissorTest() {
        RenderSystem.assertOnRenderThread();
        SCISSOR.mode.disable();
    }

    public static void _enableScissorTest() {
        RenderSystem.assertOnRenderThread();
        SCISSOR.mode.enable();
    }

    public static void _scissorBox(final int x, final int y, final int width, final int height) {
        RenderSystem.assertOnRenderThread();
        GL33C.glScissor(x, y, width, height);
    }

    public static void _disableDepthTest() {
        RenderSystem.assertOnRenderThread();
        DEPTH.mode.disable();
    }

    public static void _enableDepthTest() {
        RenderSystem.assertOnRenderThread();
        DEPTH.mode.enable();
    }

    public static void _depthFunc(final int func) {
        RenderSystem.assertOnRenderThread();
        if (func != DEPTH.func) {
            DEPTH.func = func;
            GL33C.glDepthFunc(func);
        }
    }

    public static void _depthMask(final boolean mask) {
        RenderSystem.assertOnRenderThread();
        if (mask != DEPTH.mask) {
            DEPTH.mask = mask;
            GL33C.glDepthMask(mask);
        }
    }

    public static void _disableBlend(int index) {
        RenderSystem.assertOnRenderThread();
        BLEND[index].mode.disable();
    }

    public static void _enableBlend(int index) {
        RenderSystem.assertOnRenderThread();
        BLEND[index].mode.enable();
    }

    public static void _blendFuncSeparate(final int srcRgb, final int dstRgb, final int srcAlpha, final int dstAlpha) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.BlendState firstBlend = BLEND[0];
        if (srcRgb != firstBlend.srcRgb || dstRgb != firstBlend.dstRgb || srcAlpha != firstBlend.srcAlpha || dstAlpha != firstBlend.dstAlpha) {
            firstBlend.srcRgb = srcRgb;
            firstBlend.dstRgb = dstRgb;
            firstBlend.srcAlpha = srcAlpha;
            firstBlend.dstAlpha = dstAlpha;
            glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        }
    }

    public static void _blendEquationSeparate(final int modeRgb, final int modeAlpha) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.BlendState firstBlend = BLEND[0];
        if (modeRgb != firstBlend.modeRgb || modeAlpha != firstBlend.modeAlpha) {
            firstBlend.modeRgb = modeRgb;
            firstBlend.modeAlpha = modeAlpha;
            glBlendEquationSeparate(modeRgb, modeAlpha);
        }
    }

    public static int glGetProgrami(final int program, final int pname) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetProgrami(program, pname);
    }

    public static void glAttachShader(final int program, final int shader) {
        RenderSystem.assertOnRenderThread();
        GL33C.glAttachShader(program, shader);
    }

    public static void glDeleteShader(final int shader) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDeleteShader(shader);
    }

    public static int glCreateShader(final int type) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glCreateShader(type);
    }

    public static void glShaderSource(final int shader, final String source) {
        RenderSystem.assertOnRenderThread();
        byte[] encoded = source.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = MemoryUtil.memAlloc(encoded.length + 1);
        buffer.put(encoded);
        buffer.put((byte)0);
        buffer.flip();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pointers = stack.mallocPointer(1);
            pointers.put(buffer);
            GL33C.nglShaderSource(shader, 1, pointers.address0(), 0L);
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    public static void glCompileShader(final int shader) {
        RenderSystem.assertOnRenderThread();
        GL33C.glCompileShader(shader);
    }

    public static int glGetShaderi(final int shader, final int pname) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetShaderi(shader, pname);
    }

    public static void _glUseProgram(final int program) {
        RenderSystem.assertOnRenderThread();
        GL33C.glUseProgram(program);
    }

    public static int glCreateProgram() {
        RenderSystem.assertOnRenderThread();
        return GL33C.glCreateProgram();
    }

    public static void glDeleteProgram(final int program) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDeleteProgram(program);
    }

    public static void glLinkProgram(final int program) {
        RenderSystem.assertOnRenderThread();
        GL33C.glLinkProgram(program);
    }

    public static int _glGetUniformLocation(final int program, final CharSequence name) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetUniformLocation(program, name);
    }

    public static void _glUniform1i(final int location, final int v0) {
        RenderSystem.assertOnRenderThread();
        GL33C.glUniform1i(location, v0);
    }

    public static void _glBindAttribLocation(final int program, final int location, final CharSequence name) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBindAttribLocation(program, location, name);
    }

    static void incrementTrackedBuffers() {
        numBuffers++;
        PLOT_BUFFERS.setValue(numBuffers);
    }

    public static int _glGenBuffers() {
        RenderSystem.assertOnRenderThread();
        incrementTrackedBuffers();
        return GL33C.glGenBuffers();
    }

    public static int _glGenVertexArrays() {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGenVertexArrays();
    }

    public static void _glBindBuffer(final int target, final int buffer) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBindBuffer(target, buffer);
    }

    public static void _glBindVertexArray(final int arrayId) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBindVertexArray(arrayId);
    }

    public static void _glBufferData(final int target, final ByteBuffer data, final int usage) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBufferData(target, data, usage);
    }

    public static void _glBufferSubData(final int target, final long offset, final ByteBuffer data) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBufferSubData(target, offset, data);
    }

    public static void _glBufferData(final int target, final long size, final int usage) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBufferData(target, size, usage);
    }

    public static @Nullable ByteBuffer _glMapBufferRange(final int target, final long offset, final long length, final int access) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glMapBufferRange(target, offset, length, access);
    }

    public static void _glUnmapBuffer(final int target) {
        RenderSystem.assertOnRenderThread();
        GL33C.glUnmapBuffer(target);
    }

    public static void _glDeleteBuffers(final int buffer) {
        RenderSystem.assertOnRenderThread();
        numBuffers--;
        PLOT_BUFFERS.setValue(numBuffers);
        GL33C.glDeleteBuffers(buffer);
    }

    public static void _glBindFramebuffer(final int target, final int framebuffer) {
        if ((target == 36008 || target == 36160) && readFbo != framebuffer) {
            GL33C.glBindFramebuffer(36008, framebuffer);
            readFbo = framebuffer;
        }

        if ((target == 36009 || target == 36160) && writeFbo != framebuffer) {
            GL33C.glBindFramebuffer(36009, framebuffer);
            writeFbo = framebuffer;
        }
    }

    public static int getFrameBuffer(final int target) {
        if (target == 36008) {
            return readFbo;
        } else {
            return target == 36009 ? writeFbo : 0;
        }
    }

    public static void _glBlitFrameBuffer(
        final int srcX0,
        final int srcY0,
        final int srcX1,
        final int srcY1,
        final int dstX0,
        final int dstY0,
        final int dstX1,
        final int dstY1,
        final int mask,
        final int filter
    ) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    public static void _glDeleteFramebuffers(final int framebuffer) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDeleteFramebuffers(framebuffer);
        if (readFbo == framebuffer) {
            readFbo = 0;
        }

        if (writeFbo == framebuffer) {
            writeFbo = 0;
        }
    }

    public static int glGenFramebuffers() {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGenFramebuffers();
    }

    public static void _glFramebufferTexture2D(final int target, final int attachment, final int textarget, final int texture, final int level) {
        RenderSystem.assertOnRenderThread();
        GL33C.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    public static void glBlendFuncSeparate(final int srcColor, final int dstColor, final int srcAlpha, final int dstAlpha) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBlendFuncSeparate(srcColor, dstColor, srcAlpha, dstAlpha);
    }

    public static void glBlendEquationSeparate(final int modeRgb, final int modeAlpha) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBlendEquationSeparate(modeRgb, modeAlpha);
    }

    public static String glGetShaderInfoLog(final int shader, final int maxLength) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetShaderInfoLog(shader, maxLength);
    }

    public static String glGetProgramInfoLog(final int program, final int maxLength) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetProgramInfoLog(program, maxLength);
    }

    public static void _enableCull() {
        RenderSystem.assertOnRenderThread();
        CULL.enable.enable();
    }

    public static void _disableCull() {
        RenderSystem.assertOnRenderThread();
        CULL.enable.disable();
    }

    public static void _polygonMode(final int face, final int mode) {
        RenderSystem.assertOnRenderThread();
        GL33C.glPolygonMode(face, mode);
    }

    public static void _enablePolygonOffset() {
        RenderSystem.assertOnRenderThread();
        POLY_OFFSET.fill.enable();
    }

    public static void _disablePolygonOffset() {
        RenderSystem.assertOnRenderThread();
        POLY_OFFSET.fill.disable();
    }

    public static void _polygonOffset(final float factor, final float units) {
        RenderSystem.assertOnRenderThread();
        if (factor != POLY_OFFSET.factor || units != POLY_OFFSET.units) {
            POLY_OFFSET.factor = factor;
            POLY_OFFSET.units = units;
            GL33C.glPolygonOffset(factor, units);
        }
    }

    public static void _enableColorLogicOp() {
        RenderSystem.assertOnRenderThread();
        COLOR_LOGIC.enable.enable();
    }

    public static void _disableColorLogicOp() {
        RenderSystem.assertOnRenderThread();
        COLOR_LOGIC.enable.disable();
    }

    public static void _logicOp(final int op) {
        RenderSystem.assertOnRenderThread();
        if (op != COLOR_LOGIC.op) {
            COLOR_LOGIC.op = op;
            GL33C.glLogicOp(op);
        }
    }

    public static void _activeTexture(final int texture) {
        RenderSystem.assertOnRenderThread();
        if (activeTexture != texture - 33984) {
            activeTexture = texture - 33984;
            GL33C.glActiveTexture(texture);
        }
    }

    public static void _texParameter(final int target, final int name, final int value) {
        RenderSystem.assertOnRenderThread();
        GL33C.glTexParameteri(target, name, value);
    }

    public static int _getTexLevelParameter(final int target, final int level, final int name) {
        return GL33C.glGetTexLevelParameteri(target, level, name);
    }

    public static int _genTexture() {
        RenderSystem.assertOnRenderThread();
        numTextures++;
        PLOT_TEXTURES.setValue(numTextures);
        return GL33C.glGenTextures();
    }

    public static void _deleteTexture(final int id) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDeleteTextures(id);

        for (GlStateManager.TextureState state : TEXTURES) {
            if (state.binding == id) {
                state.binding = -1;
            }
        }

        numTextures--;
        PLOT_TEXTURES.setValue(numTextures);
    }

    public static void _bindTexture(final int id) {
        RenderSystem.assertOnRenderThread();
        if (id != TEXTURES[activeTexture].binding) {
            TEXTURES[activeTexture].binding = id;
            GL33C.glBindTexture(3553, id);
        }
    }

    public static void _texImage2D(
        final int target,
        final int level,
        final int internalformat,
        final int width,
        final int height,
        final int border,
        final int format,
        final int type,
        final @Nullable ByteBuffer pixels
    ) {
        RenderSystem.assertOnRenderThread();
        GL33C.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    public static void _texSubImage2D(
        final int target,
        final int level,
        final int xoffset,
        final int yoffset,
        final int width,
        final int height,
        final int format,
        final int type,
        final long pixels
    ) {
        RenderSystem.assertOnRenderThread();
        GL33C.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    public static void _texSubImage2D(
        final int target,
        final int level,
        final int xoffset,
        final int yoffset,
        final int width,
        final int height,
        final int format,
        final int type,
        final ByteBuffer pixels
    ) {
        RenderSystem.assertOnRenderThread();
        GL33C.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    public static void _viewport(final int x, final int y, final int width, final int height) {
        GL33C.glViewport(x, y, width, height);
    }

    public static void _colorMask(final @ColorTargetState.WriteMask int writeMask) {
        RenderSystem.assertOnRenderThread();

        for (int i = 0; i < COLOR_MASK.length; i++) {
            if (writeMask != COLOR_MASK[i]) {
                COLOR_MASK[i] = writeMask;
                GL33C.glColorMaski(i, (writeMask & 1) != 0, (writeMask & 2) != 0, (writeMask & 4) != 0, (writeMask & 8) != 0);
            }
        }
    }

    public static void _colorMask(final int index, final @ColorTargetState.WriteMask int writeMask) {
        RenderSystem.assertOnRenderThread();
        if (writeMask != COLOR_MASK[index]) {
            COLOR_MASK[index] = writeMask;
            GL33C.glColorMaski(index, (writeMask & 1) != 0, (writeMask & 2) != 0, (writeMask & 4) != 0, (writeMask & 8) != 0);
        }
    }

    public static void _clear(final int mask) {
        RenderSystem.assertOnRenderThread();
        GL33C.glClear(mask);
        if (MacosUtil.IS_MACOS) {
            _getError();
        }
    }

    public static void _clearBuffer(final int index, final Vector4fc clearColor) {
        RenderSystem.assertOnRenderThread();
        GL33C.glClearBufferfv(6144, index, new float[]{clearColor.x(), clearColor.y(), clearColor.z(), clearColor.w()});
        if (MacosUtil.IS_MACOS) {
            _getError();
        }
    }

    public static void _clearBuffer(final double clearDepth) {
        RenderSystem.assertOnRenderThread();
        GL33C.glClearBufferfv(6145, 0, new float[]{(float)clearDepth});
        if (MacosUtil.IS_MACOS) {
            _getError();
        }
    }

    public static void _vertexAttribPointer(final int index, final int size, final int type, final boolean normalized, final int stride, final long value) {
        RenderSystem.assertOnRenderThread();
        GL33C.glVertexAttribPointer(index, size, type, normalized, stride, value);
    }

    public static void _vertexAttribIPointer(final int index, final int size, final int type, final int stride, final long value) {
        RenderSystem.assertOnRenderThread();
        GL33C.glVertexAttribIPointer(index, size, type, stride, value);
    }

    public static void _enableVertexAttribArray(final int index) {
        RenderSystem.assertOnRenderThread();
        GL33C.glEnableVertexAttribArray(index);
    }

    public static void _drawElements(final int mode, final int count, final int type, final long indices) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDrawElements(mode, count, type, indices);
    }

    public static void _drawArrays(final int mode, final int first, final int count) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDrawArrays(mode, first, count);
    }

    public static void _pixelStore(final int name, final int value) {
        RenderSystem.assertOnRenderThread();
        GL33C.glPixelStorei(name, value);
    }

    public static void _readPixels(final int x, final int y, final int width, final int height, final int format, final int type, final long pixels) {
        RenderSystem.assertOnRenderThread();
        GL33C.glReadPixels(x, y, width, height, format, type, pixels);
    }

    public static int _getError() {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetError();
    }

    public static void clearGlErrors() {
        RenderSystem.assertOnRenderThread();

        while (GL33C.glGetError() != 0) {
        }
    }

    public static String _getString(final int id) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetString(id);
    }

    public static int _getInteger(final int name) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetInteger(name);
    }

    public static long _glFenceSync(final int condition, final int flags) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glFenceSync(condition, flags);
    }

    public static int _glClientWaitSync(final long sync, final int flags, final long timeout) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glClientWaitSync(sync, flags, timeout);
    }

    public static void _glDeleteSync(final long sync) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDeleteSync(sync);
    }

    static {
        Arrays.setAll(COLOR_MASK, var0 -> 15);
        Arrays.setAll(BLEND, var0 -> new GlStateManager.BlendState());
    }

    @OnlyIn(Dist.CLIENT)
    private static class BlendState {
        public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(3042);
        public int srcRgb = 1;
        public int dstRgb = 0;
        public int modeRgb = 32774;
        public int srcAlpha = 1;
        public int dstAlpha = 0;
        public int modeAlpha = 32774;
    }

    @OnlyIn(Dist.CLIENT)
    private static class BooleanState {
        private final int state;
        private boolean enabled;

        public BooleanState(final int state) {
            this.state = state;
        }

        public void disable() {
            this.setEnabled(false);
        }

        public void enable() {
            this.setEnabled(true);
        }

        public void setEnabled(final boolean enabled) {
            RenderSystem.assertOnRenderThread();
            if (enabled != this.enabled) {
                this.enabled = enabled;
                if (enabled) {
                    GL33C.glEnable(this.state);
                } else {
                    GL33C.glDisable(this.state);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class ColorLogicState {
        public final GlStateManager.BooleanState enable = new GlStateManager.BooleanState(3058);
        public int op = 5379;
    }

    @OnlyIn(Dist.CLIENT)
    private static class CullState {
        public final GlStateManager.BooleanState enable = new GlStateManager.BooleanState(2884);
    }

    @OnlyIn(Dist.CLIENT)
    private static class DepthState {
        public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(2929);
        public boolean mask = true;
        public int func = 513;
    }

    @OnlyIn(Dist.CLIENT)
    private static class PolygonOffsetState {
        public final GlStateManager.BooleanState fill = new GlStateManager.BooleanState(32823);
        public float factor;
        public float units;
    }

    @OnlyIn(Dist.CLIENT)
    private static class ScissorState {
        public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(3089);
    }

    @OnlyIn(Dist.CLIENT)
    private static class TextureState {
        public int binding;
    }
}