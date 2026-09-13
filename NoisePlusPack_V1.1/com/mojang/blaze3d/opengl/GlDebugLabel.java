package com.mojang.blaze3d.opengl;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.EXTDebugLabel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class GlDebugLabel {
   private static final Logger LOGGER = LogUtils.getLogger();

   public void applyLabel(GlBuffer p_392931_) {
   }

   public void applyLabel(GlTexture p_392658_) {
   }

   public void applyLabel(GlShaderModule p_394260_) {
   }

   public void applyLabel(GlProgram p_394477_) {
   }

   public void applyLabel(VertexArrayCache.VertexArray p_391224_) {
   }

   public void pushDebugGroup(Supplier<String> p_409626_) {
   }

   public void popDebugGroup() {
   }

   public static GlDebugLabel create(GLCapabilities p_396084_, boolean p_394830_, Set<String> p_393423_) {
      if (p_394830_) {
         if (p_396084_.GL_KHR_debug && GlDevice.USE_GL_KHR_debug) {
            p_393423_.add("GL_KHR_debug");
            return new GlDebugLabel.Core();
         }

         if (p_396084_.GL_EXT_debug_label && GlDevice.USE_GL_EXT_debug_label) {
            p_393423_.add("GL_EXT_debug_label");
            return new GlDebugLabel.Ext();
         }

         LOGGER.warn("Debug labels unavailable: neither KHR_debug nor EXT_debug_label are supported");
      }

      return new GlDebugLabel.Empty();
   }

   public boolean exists() {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   static class Core extends GlDebugLabel {
      private final int maxLabelLength = GL11.glGetInteger(33512);

      @Override
      public void applyLabel(GlBuffer p_395944_) {
         Supplier<String> supplier = p_395944_.label;
         if (supplier != null) {
            KHRDebug.glObjectLabel(33504, p_395944_.handle, StringUtil.truncateStringIfNecessary(supplier.get(), this.maxLabelLength, true));
         }
      }

      @Override
      public void applyLabel(GlTexture p_397392_) {
         KHRDebug.glObjectLabel(5890, p_397392_.id, StringUtil.truncateStringIfNecessary(p_397392_.getLabel(), this.maxLabelLength, true));
      }

      @Override
      public void applyLabel(GlShaderModule p_397238_) {
         KHRDebug.glObjectLabel(33505, p_397238_.getShaderId(), StringUtil.truncateStringIfNecessary(p_397238_.getDebugLabel(), this.maxLabelLength, true));
      }

      @Override
      public void applyLabel(GlProgram p_394299_) {
         KHRDebug.glObjectLabel(33506, p_394299_.getProgramId(), StringUtil.truncateStringIfNecessary(p_394299_.getDebugLabel(), this.maxLabelLength, true));
      }

      @Override
      public void applyLabel(VertexArrayCache.VertexArray p_392345_) {
         KHRDebug.glObjectLabel(32884, p_392345_.id, StringUtil.truncateStringIfNecessary(p_392345_.format.toString(), this.maxLabelLength, true));
      }

      @Override
      public void pushDebugGroup(Supplier<String> p_409965_) {
         KHRDebug.glPushDebugGroup(33354, 0, p_409965_.get());
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

   @OnlyIn(Dist.CLIENT)
   static class Empty extends GlDebugLabel {
   }

   @OnlyIn(Dist.CLIENT)
   static class Ext extends GlDebugLabel {
      @Override
      public void applyLabel(GlBuffer p_391884_) {
         Supplier<String> supplier = p_391884_.label;
         if (supplier != null) {
            EXTDebugLabel.glLabelObjectEXT(37201, p_391884_.handle, StringUtil.truncateStringIfNecessary(supplier.get(), 256, true));
         }
      }

      @Override
      public void applyLabel(GlTexture p_397714_) {
         EXTDebugLabel.glLabelObjectEXT(5890, p_397714_.id, StringUtil.truncateStringIfNecessary(p_397714_.getLabel(), 256, true));
      }

      @Override
      public void applyLabel(GlShaderModule p_392069_) {
         EXTDebugLabel.glLabelObjectEXT(35656, p_392069_.getShaderId(), StringUtil.truncateStringIfNecessary(p_392069_.getDebugLabel(), 256, true));
      }

      @Override
      public void applyLabel(GlProgram p_394908_) {
         EXTDebugLabel.glLabelObjectEXT(35648, p_394908_.getProgramId(), StringUtil.truncateStringIfNecessary(p_394908_.getDebugLabel(), 256, true));
      }

      @Override
      public void applyLabel(VertexArrayCache.VertexArray p_396080_) {
         EXTDebugLabel.glLabelObjectEXT(32884, p_396080_.id, StringUtil.truncateStringIfNecessary(p_396080_.format.toString(), 256, true));
      }

      @Override
      public boolean exists() {
         return true;
      }
   }
}
