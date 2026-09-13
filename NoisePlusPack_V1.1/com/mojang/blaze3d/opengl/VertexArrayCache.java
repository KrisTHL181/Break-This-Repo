package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBVertexAttribBinding;
import org.lwjgl.opengl.GLCapabilities;

@OnlyIn(Dist.CLIENT)
public abstract class VertexArrayCache {
   public static VertexArrayCache create(GLCapabilities p_396371_, GlDebugLabel p_392063_, Set<String> p_395833_) {
      if (p_396371_.GL_ARB_vertex_attrib_binding && GlDevice.USE_GL_ARB_vertex_attrib_binding) {
         p_395833_.add("GL_ARB_vertex_attrib_binding");
         return new VertexArrayCache.Separate(p_392063_);
      } else {
         return new VertexArrayCache.Emulated(p_392063_);
      }
   }

   public abstract void bindVertexArray(VertexFormat var1, @Nullable GlBuffer var2);

   @OnlyIn(Dist.CLIENT)
   static class Emulated extends VertexArrayCache {
      private final Map<VertexFormat, VertexArrayCache.VertexArray> cache = new HashMap<>();
      private final GlDebugLabel debugLabels;

      public Emulated(GlDebugLabel p_394706_) {
         this.debugLabels = p_394706_;
      }

      @Override
      public void bindVertexArray(VertexFormat p_392095_, @Nullable GlBuffer p_394959_) {
         VertexArrayCache.VertexArray vertexarraycache$vertexarray = this.cache.get(p_392095_);
         if (vertexarraycache$vertexarray == null) {
            int i = GlStateManager._glGenVertexArrays();
            GlStateManager._glBindVertexArray(i);
            if (p_394959_ != null) {
               GlStateManager._glBindBuffer(34962, p_394959_.handle);
               setupCombinedAttributes(p_392095_, true);
            }

            VertexArrayCache.VertexArray vertexarraycache$vertexarray1 = new VertexArrayCache.VertexArray(i, p_392095_, p_394959_);
            this.debugLabels.applyLabel(vertexarraycache$vertexarray1);
            this.cache.put(p_392095_, vertexarraycache$vertexarray1);
         } else {
            GlStateManager._glBindVertexArray(vertexarraycache$vertexarray.id);
            if (p_394959_ != null && vertexarraycache$vertexarray.lastVertexBuffer != p_394959_) {
               GlStateManager._glBindBuffer(34962, p_394959_.handle);
               vertexarraycache$vertexarray.lastVertexBuffer = p_394959_;
               setupCombinedAttributes(p_392095_, false);
            }
         }
      }

      private static void setupCombinedAttributes(VertexFormat p_396813_, boolean p_397111_) {
         int i = p_396813_.getVertexSize();
         List<VertexFormatElement> list = p_396813_.getElements();

         for (int j = 0; j < list.size(); j++) {
            VertexFormatElement vertexformatelement = list.get(j);
            if (p_397111_) {
               GlStateManager._enableVertexAttribArray(j);
            }

            switch (vertexformatelement.usage()) {
               case POSITION:
               case GENERIC:
               case UV:
                  if (vertexformatelement.type() == VertexFormatElement.Type.FLOAT) {
                     GlStateManager._vertexAttribPointer(
                        j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), false, i, p_396813_.getOffset(vertexformatelement)
                     );
                  } else {
                     GlStateManager._vertexAttribIPointer(
                        j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), i, p_396813_.getOffset(vertexformatelement)
                     );
                  }
                  break;
               case NORMAL:
               case COLOR:
                  GlStateManager._vertexAttribPointer(
                     j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), true, i, p_396813_.getOffset(vertexformatelement)
                  );
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Separate extends VertexArrayCache {
      private final Map<VertexFormat, VertexArrayCache.VertexArray> cache = new HashMap<>();
      private final GlDebugLabel debugLabels;
      private final boolean needsMesaWorkaround;

      public Separate(GlDebugLabel p_393226_) {
         this.debugLabels = p_393226_;
         if ("Mesa".equals(GlStateManager._getString(7936))) {
            String s = GlStateManager._getString(7938);
            this.needsMesaWorkaround = s.contains("25.0.0") || s.contains("25.0.1") || s.contains("25.0.2");
         } else {
            this.needsMesaWorkaround = false;
         }
      }

      @Override
      public void bindVertexArray(VertexFormat p_391319_, @Nullable GlBuffer p_391840_) {
         VertexArrayCache.VertexArray vertexarraycache$vertexarray = this.cache.get(p_391319_);
         if (vertexarraycache$vertexarray != null) {
            GlStateManager._glBindVertexArray(vertexarraycache$vertexarray.id);
            if (p_391840_ != null && vertexarraycache$vertexarray.lastVertexBuffer != p_391840_) {
               if (this.needsMesaWorkaround
                  && vertexarraycache$vertexarray.lastVertexBuffer != null
                  && vertexarraycache$vertexarray.lastVertexBuffer.handle == p_391840_.handle) {
                  ARBVertexAttribBinding.glBindVertexBuffer(0, 0, 0L, 0);
               }

               ARBVertexAttribBinding.glBindVertexBuffer(0, p_391840_.handle, 0L, p_391319_.getVertexSize());
               vertexarraycache$vertexarray.lastVertexBuffer = p_391840_;
            }
         } else {
            int i = GlStateManager._glGenVertexArrays();
            GlStateManager._glBindVertexArray(i);
            if (p_391840_ != null) {
               List<VertexFormatElement> list = p_391319_.getElements();

               for (int j = 0; j < list.size(); j++) {
                  VertexFormatElement vertexformatelement = list.get(j);
                  GlStateManager._enableVertexAttribArray(j);
                  switch (vertexformatelement.usage()) {
                     case POSITION:
                     case GENERIC:
                     case UV:
                        if (vertexformatelement.type() == VertexFormatElement.Type.FLOAT) {
                           ARBVertexAttribBinding.glVertexAttribFormat(
                              j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), false, p_391319_.getOffset(vertexformatelement)
                           );
                        } else {
                           ARBVertexAttribBinding.glVertexAttribIFormat(
                              j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), p_391319_.getOffset(vertexformatelement)
                           );
                        }
                        break;
                     case NORMAL:
                     case COLOR:
                        ARBVertexAttribBinding.glVertexAttribFormat(
                           j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), true, p_391319_.getOffset(vertexformatelement)
                        );
                  }

                  ARBVertexAttribBinding.glVertexAttribBinding(j, 0);
               }
            }

            if (p_391840_ != null) {
               ARBVertexAttribBinding.glBindVertexBuffer(0, p_391840_.handle, 0L, p_391319_.getVertexSize());
            }

            VertexArrayCache.VertexArray vertexarraycache$vertexarray1 = new VertexArrayCache.VertexArray(i, p_391319_, p_391840_);
            this.debugLabels.applyLabel(vertexarraycache$vertexarray1);
            this.cache.put(p_391319_, vertexarraycache$vertexarray1);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class VertexArray {
      final int id;
      final VertexFormat format;
      @Nullable GlBuffer lastVertexBuffer;

      VertexArray(int p_397876_, VertexFormat p_397604_, @Nullable GlBuffer p_391760_) {
         this.id = p_397876_;
         this.format = p_397604_;
         this.lastVertexBuffer = p_391760_;
      }
   }
}
