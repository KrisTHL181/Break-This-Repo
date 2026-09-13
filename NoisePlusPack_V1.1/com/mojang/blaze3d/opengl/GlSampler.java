package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.OptionalDouble;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL33C;

@OnlyIn(Dist.CLIENT)
public class GlSampler extends GpuSampler {
   private final int id;
   private final AddressMode addressModeU;
   private final AddressMode addressModeV;
   private final FilterMode minFilter;
   private final FilterMode magFilter;
   private final int maxAnisotropy;
   private final OptionalDouble maxLod;
   private boolean closed;

   public GlSampler(AddressMode p_452363_, AddressMode p_455375_, FilterMode p_458020_, FilterMode p_453175_, int p_451635_, OptionalDouble p_458619_) {
      this.addressModeU = p_452363_;
      this.addressModeV = p_455375_;
      this.minFilter = p_458020_;
      this.magFilter = p_453175_;
      this.maxAnisotropy = p_451635_;
      this.maxLod = p_458619_;
      this.id = GL33C.glGenSamplers();
      GL33C.glSamplerParameteri(this.id, 10242, GlConst.toGl(p_452363_));
      GL33C.glSamplerParameteri(this.id, 10243, GlConst.toGl(p_455375_));
      if (p_451635_ > 1) {
         GL33C.glSamplerParameterf(this.id, 34046, p_451635_);
      }

      switch (p_458020_) {
         case NEAREST:
            GL33C.glSamplerParameteri(this.id, 10241, 9986);
            break;
         case LINEAR:
            GL33C.glSamplerParameteri(this.id, 10241, 9987);
      }

      switch (p_453175_) {
         case NEAREST:
            GL33C.glSamplerParameteri(this.id, 10240, 9728);
            break;
         case LINEAR:
            GL33C.glSamplerParameteri(this.id, 10240, 9729);
      }

      if (p_458619_.isPresent()) {
         GL33C.glSamplerParameterf(this.id, 33083, (float)p_458619_.getAsDouble());
      }
   }

   public int getId() {
      return this.id;
   }

   @Override
   public AddressMode getAddressModeU() {
      return this.addressModeU;
   }

   @Override
   public AddressMode getAddressModeV() {
      return this.addressModeV;
   }

   @Override
   public FilterMode getMinFilter() {
      return this.minFilter;
   }

   @Override
   public FilterMode getMagFilter() {
      return this.magFilter;
   }

   @Override
   public int getMaxAnisotropy() {
      return this.maxAnisotropy;
   }

   @Override
   public OptionalDouble getMaxLod() {
      return this.maxLod;
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         GL33C.glDeleteSamplers(this.id);
      }
   }

   public boolean isClosed() {
      return this.closed;
   }
}
