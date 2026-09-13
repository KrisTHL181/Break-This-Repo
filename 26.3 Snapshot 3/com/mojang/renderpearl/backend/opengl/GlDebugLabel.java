package com.mojang.renderpearl.backend.opengl;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.util.StringUtil;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.EXTDebugLabel;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

public abstract class GlDebugLabel {
   private static final Logger LOGGER = LogUtils.getLogger();

   public void applyLabel(final GlBuffer buffer, final @Nullable Supplier<String> label) {
   }

   public void applyLabel(final GlTexture texture) {
   }

   public void applyLabel(final GlShaderModule shaderModule) {
   }

   public void applyLabel(final GlProgram program) {
   }

   public void applyLabel(final VertexArrayCache.VertexArray vertexArray) {
   }

   public void pushDebugGroup(final Supplier<String> label) {
   }

   public void popDebugGroup() {
   }

   public static GlDebugLabel create(final GLCapabilities caps, final boolean wantsLabels, final Set<String> enabledExtensions) {
      if (wantsLabels) {
         if (caps.GL_KHR_debug && GlDevice.USE_GL_KHR_debug) {
            enabledExtensions.add("GL_KHR_debug");
            return new GlDebugLabel.Core();
         }

         if (caps.GL_EXT_debug_label && GlDevice.USE_GL_EXT_debug_label) {
            enabledExtensions.add("GL_EXT_debug_label");
            return new GlDebugLabel.Ext();
         }

         LOGGER.warn("Debug labels unavailable: neither KHR_debug nor EXT_debug_label are supported");
      }

      return new GlDebugLabel.Empty();
   }

   public boolean exists() {
      return false;
   }

   private static class Core extends GlDebugLabel {
      private final int maxLabelLength = GL33C.glGetInteger(33512);

      @Override
      public void applyLabel(final GlBuffer buffer, final @Nullable Supplier<String> label) {
         if (label != null) {
            KHRDebug.glObjectLabel(33504, buffer.handle(), StringUtil.truncateStringIfNecessary(label.get(), this.maxLabelLength, true));
         }
      }

      @Override
      public void applyLabel(final GlTexture texture) {
         KHRDebug.glObjectLabel(5890, texture.id, StringUtil.truncateStringIfNecessary(texture.getLabel(), this.maxLabelLength, true));
      }

      @Override
      public void applyLabel(final GlShaderModule shaderModule) {
         KHRDebug.glObjectLabel(
            33505, shaderModule.getShaderId(), StringUtil.truncateStringIfNecessary(shaderModule.getDebugLabel(), this.maxLabelLength, true)
         );
      }

      @Override
      public void applyLabel(final GlProgram program) {
         KHRDebug.glObjectLabel(33506, program.getProgramId(), StringUtil.truncateStringIfNecessary(program.getDebugLabel(), this.maxLabelLength, true));
      }

      @Override
      public void applyLabel(final VertexArrayCache.VertexArray vertexArray) {
         KHRDebug.glObjectLabel(32884, vertexArray.id, StringUtil.truncateStringIfNecessary(vertexArray.formatName, this.maxLabelLength, true));
      }

      @Override
      public void pushDebugGroup(final Supplier<String> label) {
         KHRDebug.glPushDebugGroup(33354, 0, label.get());
      }

      @Override
      public void popDebugGroup() {
         KHRDebug.glPopDebugGroup();
      }

      @Override
      public boolean exists() {
         return true;
      }
   }

   private static class Empty extends GlDebugLabel {
   }

   private static class Ext extends GlDebugLabel {
      @Override
      public void applyLabel(final GlBuffer buffer, final @Nullable Supplier<String> label) {
         if (label != null) {
            EXTDebugLabel.glLabelObjectEXT(37201, buffer.handle(), StringUtil.truncateStringIfNecessary(label.get(), 256, true));
         }
      }

      @Override
      public void applyLabel(final GlTexture texture) {
         EXTDebugLabel.glLabelObjectEXT(5890, texture.id, StringUtil.truncateStringIfNecessary(texture.getLabel(), 256, true));
      }

      @Override
      public void applyLabel(final GlShaderModule shaderModule) {
         EXTDebugLabel.glLabelObjectEXT(35656, shaderModule.getShaderId(), StringUtil.truncateStringIfNecessary(shaderModule.getDebugLabel(), 256, true));
      }

      @Override
      public void applyLabel(final GlProgram program) {
         EXTDebugLabel.glLabelObjectEXT(35648, program.getProgramId(), StringUtil.truncateStringIfNecessary(program.getDebugLabel(), 256, true));
      }

      @Override
      public void applyLabel(final VertexArrayCache.VertexArray vertexArray) {
         EXTDebugLabel.glLabelObjectEXT(32884, vertexArray.id, StringUtil.truncateStringIfNecessary(vertexArray.formatName, 256, true));
      }

      @Override
      public boolean exists() {
         return true;
      }
   }
}
