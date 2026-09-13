package com.mojang.blaze3d.systems;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.OptionalDouble;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SamplerCache {
   private final GpuSampler[] samplers = new GpuSampler[32];

   public void initialize() {
      GpuDevice gpudevice = RenderSystem.getDevice();
      if (AddressMode.values().length == 2 && FilterMode.values().length == 2) {
         for (AddressMode addressmode : AddressMode.values()) {
            for (AddressMode addressmode1 : AddressMode.values()) {
               for (FilterMode filtermode : FilterMode.values()) {
                  for (FilterMode filtermode1 : FilterMode.values()) {
                     for (boolean flag : new boolean[]{true, false}) {
                        this.samplers[encode(addressmode, addressmode1, filtermode, filtermode1, flag)] = gpudevice.createSampler(
                           addressmode, addressmode1, filtermode, filtermode1, 1, flag ? OptionalDouble.empty() : OptionalDouble.of(0.0)
                        );
                     }
                  }
               }
            }
         }
      } else {
         throw new IllegalStateException("AddressMode and FilterMode enum sizes must be 2 - if you expanded them, please update SamplerCache");
      }
   }

   public GpuSampler getSampler(AddressMode p_454290_, AddressMode p_458452_, FilterMode p_460336_, FilterMode p_460079_, boolean p_455450_) {
      return this.samplers[encode(p_454290_, p_458452_, p_460336_, p_460079_, p_455450_)];
   }

   public GpuSampler getClampToEdge(FilterMode p_454087_) {
      return this.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, p_454087_, p_454087_, false);
   }

   public GpuSampler getClampToEdge(FilterMode p_455950_, boolean p_451306_) {
      return this.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, p_455950_, p_455950_, p_451306_);
   }

   public GpuSampler getRepeat(FilterMode p_456853_) {
      return this.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, p_456853_, p_456853_, false);
   }

   public GpuSampler getRepeat(FilterMode p_452824_, boolean p_460908_) {
      return this.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, p_452824_, p_452824_, p_460908_);
   }

   public void close() {
      for (GpuSampler gpusampler : this.samplers) {
         gpusampler.close();
      }
   }

   @VisibleForTesting
   static int encode(AddressMode p_453366_, AddressMode p_454583_, FilterMode p_451436_, FilterMode p_455123_, boolean p_451956_) {
      int i = 0;
      i |= p_453366_.ordinal() & 1;
      i |= (p_454583_.ordinal() & 1) << 1;
      i |= (p_451436_.ordinal() & 1) << 2;
      i |= (p_455123_.ordinal() & 1) << 3;
      if (p_451956_) {
         i |= 16;
      }

      return i;
   }
}
