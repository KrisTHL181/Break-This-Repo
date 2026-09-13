package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.jtracy.Plot;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class GlStateManager {
   private static final Plot PLOT_TEXTURES = TracyClient.createPlot("GPU Textures");
   private static int numTextures = 0;
   private static final Plot PLOT_BUFFERS = TracyClient.createPlot("GPU Buffers");
   private static int numBuffers = 0;
   private static final GlStateManager.BlendState BLEND = new GlStateManager.BlendState();
   private static final GlStateManager.DepthState DEPTH = new GlStateManager.DepthState();
   private static final GlStateManager.CullState CULL = new GlStateManager.CullState();
   private static final GlStateManager.PolygonOffsetState POLY_OFFSET = new GlStateManager.PolygonOffsetState();
   private static final GlStateManager.ColorLogicState COLOR_LOGIC = new GlStateManager.ColorLogicState();
   private static final GlStateManager.ScissorState SCISSOR = new GlStateManager.ScissorState();
   private static int activeTexture;
   private static final int TEXTURE_COUNT = 12;
   private static final GlStateManager.TextureState[] TEXTURES = IntStream.range(0, 12)
      .mapToObj(p_397300_ -> new GlStateManager.TextureState())
      .toArray(GlStateManager.TextureState[]::new);
   private static final GlStateManager.ColorMask COLOR_MASK = new GlStateManager.ColorMask();
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

   public static void _scissorBox(int p_391506_, int p_397857_, int p_395718_, int p_397952_) {
      RenderSystem.assertOnRenderThread();
      GL20.glScissor(p_391506_, p_397857_, p_395718_, p_397952_);
   }

   public static void _disableDepthTest() {
      RenderSystem.assertOnRenderThread();
      DEPTH.mode.disable();
   }

   public static void _enableDepthTest() {
      RenderSystem.assertOnRenderThread();
      DEPTH.mode.enable();
   }

   public static void _depthFunc(int p_391448_) {
      RenderSystem.assertOnRenderThread();
      if (p_391448_ != DEPTH.func) {
         DEPTH.func = p_391448_;
         GL11.glDepthFunc(p_391448_);
      }
   }

   public static void _depthMask(boolean p_396968_) {
      RenderSystem.assertOnRenderThread();
      if (p_396968_ != DEPTH.mask) {
         DEPTH.mask = p_396968_;
         GL11.glDepthMask(p_396968_);
      }
   }

   public static void _disableBlend() {
      RenderSystem.assertOnRenderThread();
      BLEND.mode.disable();
   }

   public static void _enableBlend() {
      RenderSystem.assertOnRenderThread();
      BLEND.mode.enable();
   }

   public static void _blendFuncSeparate(int p_393389_, int p_391204_, int p_391240_, int p_394979_) {
      RenderSystem.assertOnRenderThread();
      if (p_393389_ != BLEND.srcRgb || p_391204_ != BLEND.dstRgb || p_391240_ != BLEND.srcAlpha || p_394979_ != BLEND.dstAlpha) {
         BLEND.srcRgb = p_393389_;
         BLEND.dstRgb = p_391204_;
         BLEND.srcAlpha = p_391240_;
         BLEND.dstAlpha = p_394979_;
         glBlendFuncSeparate(p_393389_, p_391204_, p_391240_, p_394979_);
      }
   }

   public static int glGetProgrami(int p_392680_, int p_394462_) {
      RenderSystem.assertOnRenderThread();
      return GL20.glGetProgrami(p_392680_, p_394462_);
   }

   public static void glAttachShader(int p_393760_, int p_392795_) {
      RenderSystem.assertOnRenderThread();
      GL20.glAttachShader(p_393760_, p_392795_);
   }

   public static void glDeleteShader(int p_398038_) {
      RenderSystem.assertOnRenderThread();
      GL20.glDeleteShader(p_398038_);
   }

   public static int glCreateShader(int p_396843_) {
      RenderSystem.assertOnRenderThread();
      return GL20.glCreateShader(p_396843_);
   }

   public static void glShaderSource(int p_393940_, String p_391652_) {
      RenderSystem.assertOnRenderThread();
      byte[] abyte = p_391652_.getBytes(StandardCharsets.UTF_8);
      ByteBuffer bytebuffer = MemoryUtil.memAlloc(abyte.length + 1);
      bytebuffer.put(abyte);
      bytebuffer.put((byte)0);
      bytebuffer.flip();

      try {
         MemoryStack memorystack = MemoryStack.stackPush();

         try {
            PointerBuffer pointerbuffer = memorystack.mallocPointer(1);
            pointerbuffer.put(bytebuffer);
            GL20C.nglShaderSource(p_393940_, 1, pointerbuffer.address0(), 0L);
         } catch (Throwable var12) {
            if (memorystack != null) {
               try {
                  memorystack.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (memorystack != null) {
            memorystack.close();
         }
      } finally {
         MemoryUtil.memFree(bytebuffer);
      }
   }

   public static void glCompileShader(int p_391844_) {
      RenderSystem.assertOnRenderThread();
      GL20.glCompileShader(p_391844_);
   }

   public static int glGetShaderi(int p_396236_, int p_396666_) {
      RenderSystem.assertOnRenderThread();
      return GL20.glGetShaderi(p_396236_, p_396666_);
   }

   public static void _glUseProgram(int p_392744_) {
      RenderSystem.assertOnRenderThread();
      GL20.glUseProgram(p_392744_);
   }

   public static int glCreateProgram() {
      RenderSystem.assertOnRenderThread();
      return GL20.glCreateProgram();
   }

   public static void glDeleteProgram(int p_397067_) {
      RenderSystem.assertOnRenderThread();
      GL20.glDeleteProgram(p_397067_);
   }

   public static void glLinkProgram(int p_394434_) {
      RenderSystem.assertOnRenderThread();
      GL20.glLinkProgram(p_394434_);
   }

   public static int _glGetUniformLocation(int p_393659_, CharSequence p_396010_) {
      RenderSystem.assertOnRenderThread();
      return GL20.glGetUniformLocation(p_393659_, p_396010_);
   }

   public static void _glUniform1i(int p_391519_, int p_394457_) {
      RenderSystem.assertOnRenderThread();
      GL20.glUniform1i(p_391519_, p_394457_);
   }

   public static void _glBindAttribLocation(int p_392637_, int p_392418_, CharSequence p_393241_) {
      RenderSystem.assertOnRenderThread();
      GL20.glBindAttribLocation(p_392637_, p_392418_, p_393241_);
   }

   public static void incrementTrackedBuffers() {
      numBuffers++;
      PLOT_BUFFERS.setValue(numBuffers);
   }

   public static int _glGenBuffers() {
      RenderSystem.assertOnRenderThread();
      incrementTrackedBuffers();
      return GL15.glGenBuffers();
   }

   public static int _glGenVertexArrays() {
      RenderSystem.assertOnRenderThread();
      return GL30.glGenVertexArrays();
   }

   public static void _glBindBuffer(int p_396014_, int p_395586_) {
      RenderSystem.assertOnRenderThread();
      GL15.glBindBuffer(p_396014_, p_395586_);
   }

   public static void _glBindVertexArray(int p_396222_) {
      RenderSystem.assertOnRenderThread();
      GL30.glBindVertexArray(p_396222_);
   }

   public static void _glBufferData(int p_395214_, ByteBuffer p_392254_, int p_398006_) {
      RenderSystem.assertOnRenderThread();
      GL15.glBufferData(p_395214_, p_392254_, p_398006_);
   }

   public static void _glBufferSubData(int p_393265_, long p_456397_, ByteBuffer p_393357_) {
      RenderSystem.assertOnRenderThread();
      GL15.glBufferSubData(p_393265_, p_456397_, p_393357_);
   }

   public static void _glBufferData(int p_396133_, long p_397153_, int p_396486_) {
      RenderSystem.assertOnRenderThread();
      GL15.glBufferData(p_396133_, p_397153_, p_396486_);
   }

   public static @Nullable ByteBuffer _glMapBufferRange(int p_396635_, long p_456616_, long p_452737_, int p_392421_) {
      RenderSystem.assertOnRenderThread();
      return GL30.glMapBufferRange(p_396635_, p_456616_, p_452737_, p_392421_);
   }

   public static void _glUnmapBuffer(int p_393698_) {
      RenderSystem.assertOnRenderThread();
      GL15.glUnmapBuffer(p_393698_);
   }

   public static void _glDeleteBuffers(int p_392748_) {
      RenderSystem.assertOnRenderThread();
      numBuffers--;
      PLOT_BUFFERS.setValue(numBuffers);
      GL15.glDeleteBuffers(p_392748_);
   }

   public static void _glBindFramebuffer(int p_395557_, int p_393310_) {
      if ((p_395557_ == 36008 || p_395557_ == 36160) && readFbo != p_393310_) {
         GL30.glBindFramebuffer(36008, p_393310_);
         readFbo = p_393310_;
      }

      if ((p_395557_ == 36009 || p_395557_ == 36160) && writeFbo != p_393310_) {
         GL30.glBindFramebuffer(36009, p_393310_);
         writeFbo = p_393310_;
      }
   }

   public static int getFrameBuffer(int p_393448_) {
      if (p_393448_ == 36008) {
         return readFbo;
      } else {
         return p_393448_ == 36009 ? writeFbo : 0;
      }
   }

   public static void _glBlitFrameBuffer(
      int p_393298_, int p_392926_, int p_392564_, int p_394340_, int p_395138_, int p_397917_, int p_391868_, int p_393543_, int p_396985_, int p_396115_
   ) {
      RenderSystem.assertOnRenderThread();
      GL30.glBlitFramebuffer(p_393298_, p_392926_, p_392564_, p_394340_, p_395138_, p_397917_, p_391868_, p_393543_, p_396985_, p_396115_);
   }

   public static void _glDeleteFramebuffers(int p_391497_) {
      RenderSystem.assertOnRenderThread();
      GL30.glDeleteFramebuffers(p_391497_);
      if (readFbo == p_391497_) {
         readFbo = 0;
      }

      if (writeFbo == p_391497_) {
         writeFbo = 0;
      }
   }

   public static int glGenFramebuffers() {
      RenderSystem.assertOnRenderThread();
      return GL30.glGenFramebuffers();
   }

   public static void _glFramebufferTexture2D(int p_392731_, int p_391934_, int p_392764_, int p_396348_, int p_391184_) {
      RenderSystem.assertOnRenderThread();
      GL30.glFramebufferTexture2D(p_392731_, p_391934_, p_392764_, p_396348_, p_391184_);
   }

   public static void glBlendFuncSeparate(int p_393432_, int p_392255_, int p_397678_, int p_397509_) {
      RenderSystem.assertOnRenderThread();
      GL14.glBlendFuncSeparate(p_393432_, p_392255_, p_397678_, p_397509_);
   }

   public static String glGetShaderInfoLog(int p_391967_, int p_392771_) {
      RenderSystem.assertOnRenderThread();
      return GL20.glGetShaderInfoLog(p_391967_, p_392771_);
   }

   public static String glGetProgramInfoLog(int p_394100_, int p_394227_) {
      RenderSystem.assertOnRenderThread();
      return GL20.glGetProgramInfoLog(p_394100_, p_394227_);
   }

   public static void _enableCull() {
      RenderSystem.assertOnRenderThread();
      CULL.enable.enable();
   }

   public static void _disableCull() {
      RenderSystem.assertOnRenderThread();
      CULL.enable.disable();
   }

   public static void _polygonMode(int p_392773_, int p_394202_) {
      RenderSystem.assertOnRenderThread();
      GL11.glPolygonMode(p_392773_, p_394202_);
   }

   public static void _enablePolygonOffset() {
      RenderSystem.assertOnRenderThread();
      POLY_OFFSET.fill.enable();
   }

   public static void _disablePolygonOffset() {
      RenderSystem.assertOnRenderThread();
      POLY_OFFSET.fill.disable();
   }

   public static void _polygonOffset(float p_391589_, float p_392606_) {
      RenderSystem.assertOnRenderThread();
      if (p_391589_ != POLY_OFFSET.factor || p_392606_ != POLY_OFFSET.units) {
         POLY_OFFSET.factor = p_391589_;
         POLY_OFFSET.units = p_392606_;
         GL11.glPolygonOffset(p_391589_, p_392606_);
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

   public static void _logicOp(int p_391507_) {
      RenderSystem.assertOnRenderThread();
      if (p_391507_ != COLOR_LOGIC.op) {
         COLOR_LOGIC.op = p_391507_;
         GL11.glLogicOp(p_391507_);
      }
   }

   public static void _activeTexture(int p_391310_) {
      RenderSystem.assertOnRenderThread();
      if (activeTexture != p_391310_ - 33984) {
         activeTexture = p_391310_ - 33984;
         GL13.glActiveTexture(p_391310_);
      }
   }

   public static void _texParameter(int p_393876_, int p_397998_, int p_396741_) {
      RenderSystem.assertOnRenderThread();
      GL11.glTexParameteri(p_393876_, p_397998_, p_396741_);
   }

   public static int _getTexLevelParameter(int p_393352_, int p_397204_, int p_396319_) {
      return GL11.glGetTexLevelParameteri(p_393352_, p_397204_, p_396319_);
   }

   public static int _genTexture() {
      RenderSystem.assertOnRenderThread();
      numTextures++;
      PLOT_TEXTURES.setValue(numTextures);
      return GL11.glGenTextures();
   }

   public static void _deleteTexture(int p_394568_) {
      RenderSystem.assertOnRenderThread();
      GL11.glDeleteTextures(p_394568_);

      for (GlStateManager.TextureState glstatemanager$texturestate : TEXTURES) {
         if (glstatemanager$texturestate.binding == p_394568_) {
            glstatemanager$texturestate.binding = -1;
         }
      }

      numTextures--;
      PLOT_TEXTURES.setValue(numTextures);
   }

   public static void _bindTexture(int p_394207_) {
      RenderSystem.assertOnRenderThread();
      if (p_394207_ != TEXTURES[activeTexture].binding) {
         TEXTURES[activeTexture].binding = p_394207_;
         GL11.glBindTexture(3553, p_394207_);
      }
   }

   public static void _texImage2D(
      int p_391628_, int p_392395_, int p_394214_, int p_396350_, int p_394928_, int p_396300_, int p_397202_, int p_397817_, @Nullable ByteBuffer p_426795_
   ) {
      RenderSystem.assertOnRenderThread();
      GL11.glTexImage2D(p_391628_, p_392395_, p_394214_, p_396350_, p_394928_, p_396300_, p_397202_, p_397817_, p_426795_);
   }

   public static void _texSubImage2D(
      int p_394880_, int p_397930_, int p_397479_, int p_393161_, int p_393411_, int p_397215_, int p_397149_, int p_393711_, long p_395529_
   ) {
      RenderSystem.assertOnRenderThread();
      GL11.glTexSubImage2D(p_394880_, p_397930_, p_397479_, p_393161_, p_393411_, p_397215_, p_397149_, p_393711_, p_395529_);
   }

   public static void _texSubImage2D(
      int p_394769_, int p_392303_, int p_393147_, int p_394524_, int p_396987_, int p_394589_, int p_397928_, int p_397699_, ByteBuffer p_430174_
   ) {
      RenderSystem.assertOnRenderThread();
      GL11.glTexSubImage2D(p_394769_, p_392303_, p_393147_, p_394524_, p_396987_, p_394589_, p_397928_, p_397699_, p_430174_);
   }

   public static void _viewport(int p_392872_, int p_397534_, int p_395252_, int p_393762_) {
      GL11.glViewport(p_392872_, p_397534_, p_395252_, p_393762_);
   }

   public static void _colorMask(boolean p_393218_, boolean p_396455_, boolean p_391166_, boolean p_395760_) {
      RenderSystem.assertOnRenderThread();
      if (p_393218_ != COLOR_MASK.red || p_396455_ != COLOR_MASK.green || p_391166_ != COLOR_MASK.blue || p_395760_ != COLOR_MASK.alpha) {
         COLOR_MASK.red = p_393218_;
         COLOR_MASK.green = p_396455_;
         COLOR_MASK.blue = p_391166_;
         COLOR_MASK.alpha = p_395760_;
         GL11.glColorMask(p_393218_, p_396455_, p_391166_, p_395760_);
      }
   }

   public static void _clear(int p_397125_) {
      RenderSystem.assertOnRenderThread();
      GL11.glClear(p_397125_);
      if (MacosUtil.IS_MACOS) {
         _getError();
      }
   }

   public static void _vertexAttribPointer(int p_396169_, int p_393187_, int p_394089_, boolean p_396314_, int p_393703_, long p_393673_) {
      RenderSystem.assertOnRenderThread();
      GL20.glVertexAttribPointer(p_396169_, p_393187_, p_394089_, p_396314_, p_393703_, p_393673_);
   }

   public static void _vertexAttribIPointer(int p_392930_, int p_396087_, int p_397639_, int p_397483_, long p_396751_) {
      RenderSystem.assertOnRenderThread();
      GL30.glVertexAttribIPointer(p_392930_, p_396087_, p_397639_, p_397483_, p_396751_);
   }

   public static void _enableVertexAttribArray(int p_396763_) {
      RenderSystem.assertOnRenderThread();
      GL20.glEnableVertexAttribArray(p_396763_);
   }

   public static void _drawElements(int p_394836_, int p_395652_, int p_393291_, long p_393540_) {
      RenderSystem.assertOnRenderThread();
      GL11.glDrawElements(p_394836_, p_395652_, p_393291_, p_393540_);
   }

   public static void _drawArrays(int p_393403_, int p_395562_, int p_396515_) {
      RenderSystem.assertOnRenderThread();
      GL11.glDrawArrays(p_393403_, p_395562_, p_396515_);
   }

   public static void _pixelStore(int p_391888_, int p_393795_) {
      RenderSystem.assertOnRenderThread();
      GL11.glPixelStorei(p_391888_, p_393795_);
   }

   public static void _readPixels(int p_396021_, int p_391924_, int p_393428_, int p_396602_, int p_397360_, int p_397798_, long p_391804_) {
      RenderSystem.assertOnRenderThread();
      GL11.glReadPixels(p_396021_, p_391924_, p_393428_, p_396602_, p_397360_, p_397798_, p_391804_);
   }

   public static int _getError() {
      RenderSystem.assertOnRenderThread();
      return GL11.glGetError();
   }

   public static void clearGlErrors() {
      RenderSystem.assertOnRenderThread();

      while (GL11.glGetError() != 0) {
      }
   }

   public static String _getString(int p_396593_) {
      RenderSystem.assertOnRenderThread();
      return GL11.glGetString(p_396593_);
   }

   public static int _getInteger(int p_395450_) {
      RenderSystem.assertOnRenderThread();
      return GL11.glGetInteger(p_395450_);
   }

   public static long _glFenceSync(int p_395964_, int p_397537_) {
      RenderSystem.assertOnRenderThread();
      return GL32.glFenceSync(p_395964_, p_397537_);
   }

   public static int _glClientWaitSync(long p_394010_, int p_395619_, long p_397494_) {
      RenderSystem.assertOnRenderThread();
      return GL32.glClientWaitSync(p_394010_, p_395619_, p_397494_);
   }

   public static void _glDeleteSync(long p_393074_) {
      RenderSystem.assertOnRenderThread();
      GL32.glDeleteSync(p_393074_);
   }

   @OnlyIn(Dist.CLIENT)
   static class BlendState {
      public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(3042);
      public int srcRgb = 1;
      public int dstRgb = 0;
      public int srcAlpha = 1;
      public int dstAlpha = 0;
   }

   @OnlyIn(Dist.CLIENT)
   static class BooleanState {
      private final int state;
      private boolean enabled;

      public BooleanState(int p_397528_) {
         this.state = p_397528_;
      }

      public void disable() {
         this.setEnabled(false);
      }

      public void enable() {
         this.setEnabled(true);
      }

      public void setEnabled(boolean p_397886_) {
         RenderSystem.assertOnRenderThread();
         if (p_397886_ != this.enabled) {
            this.enabled = p_397886_;
            if (p_397886_) {
               GL11.glEnable(this.state);
            } else {
               GL11.glDisable(this.state);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ColorLogicState {
      public final GlStateManager.BooleanState enable = new GlStateManager.BooleanState(3058);
      public int op = 5379;
   }

   @OnlyIn(Dist.CLIENT)
   static class ColorMask {
      public boolean red = true;
      public boolean green = true;
      public boolean blue = true;
      public boolean alpha = true;
   }

   @OnlyIn(Dist.CLIENT)
   static class CullState {
      public final GlStateManager.BooleanState enable = new GlStateManager.BooleanState(2884);
   }

   @OnlyIn(Dist.CLIENT)
   static class DepthState {
      public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(2929);
      public boolean mask = true;
      public int func = 513;
   }

   @OnlyIn(Dist.CLIENT)
   static class PolygonOffsetState {
      public final GlStateManager.BooleanState fill = new GlStateManager.BooleanState(32823);
      public float factor;
      public float units;
   }

   @OnlyIn(Dist.CLIENT)
   static class ScissorState {
      public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(3089);
   }

   @OnlyIn(Dist.CLIENT)
   static class TextureState {
      public int binding;
   }
}
