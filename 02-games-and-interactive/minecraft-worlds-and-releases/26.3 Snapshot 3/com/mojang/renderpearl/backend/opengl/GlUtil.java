package com.mojang.renderpearl.backend.opengl;

import com.mojang.renderpearl.api.buffers.GpuBuffer;

public class GlUtil {
   public static int selectBufferBindTarget(final @GpuBuffer.Usage int usage) {
      if ((usage & 32) != 0) {
         return 34962;
      } else if ((usage & 64) != 0) {
         return 34963;
      } else {
         return (usage & 128) != 0 ? 35345 : 36663;
      }
   }
}
