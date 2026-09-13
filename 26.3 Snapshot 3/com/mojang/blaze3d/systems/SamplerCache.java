package com.mojang.blaze3d.systems;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import java.util.OptionalDouble;

public class SamplerCache {
   private final GpuSampler[] samplers = new GpuSampler[32];

   public void initialize() {
      GpuDevice device = RenderSystem.getDevice();
      if (AddressMode.values().length == 2 && FilterMode.values().length == 2) {
         for (AddressMode addressModeU : AddressMode.values()) {
            for (AddressMode addressModeV : AddressMode.values()) {
               for (FilterMode minFilter : FilterMode.values()) {
                  for (FilterMode magFilter : FilterMode.values()) {
                     for (boolean useMipmaps : new boolean[]{true, false}) {
                        this.samplers[encode(addressModeU, addressModeV, minFilter, magFilter, useMipmaps)] = device.createSampler(
                           addressModeU, addressModeV, minFilter, magFilter, 1, useMipmaps ? OptionalDouble.empty() : OptionalDouble.of(0.0)
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

   public GpuSampler getSampler(
      final AddressMode addressModeU, final AddressMode addressModeV, final FilterMode minFilter, final FilterMode magFilter, final boolean useMipmaps
   ) {
      return this.samplers[encode(addressModeU, addressModeV, minFilter, magFilter, useMipmaps)];
   }

   public GpuSampler getClampToEdge(final FilterMode minMag) {
      return this.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, minMag, minMag, false);
   }

   public GpuSampler getClampToEdge(final FilterMode minMag, final boolean mipmaps) {
      return this.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, minMag, minMag, mipmaps);
   }

   public GpuSampler getRepeat(final FilterMode minMag) {
      return this.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, minMag, minMag, false);
   }

   public GpuSampler getRepeat(final FilterMode minMag, final boolean mipmaps) {
      return this.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, minMag, minMag, mipmaps);
   }

   public void close() {
      for (GpuSampler sampler : this.samplers) {
         sampler.close();
      }
   }

   @VisibleForTesting
   static int encode(
      final AddressMode addressModeU, final AddressMode addressModeV, final FilterMode minFilter, final FilterMode magFilter, final boolean useMipmaps
   ) {
      int result = 0;
      result |= addressModeU.ordinal() & 1;
      result |= (addressModeV.ordinal() & 1) << 1;
      result |= (minFilter.ordinal() & 1) << 2;
      result |= (magFilter.ordinal() & 1) << 3;
      if (useMipmaps) {
         result |= 16;
      }

      return result;
   }
}
