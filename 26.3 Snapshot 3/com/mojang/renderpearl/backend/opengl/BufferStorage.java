package com.mojang.renderpearl.backend.opengl;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import java.util.Set;
import org.lwjgl.opengl.GLCapabilities;

public abstract class BufferStorage {
   public static BufferStorage create(final GLCapabilities capabilities, final Set<String> enabledExtensions) {
      if (capabilities.GL_ARB_buffer_storage && GlDevice.USE_GL_ARB_buffer_storage) {
         enabledExtensions.add("GL_ARB_buffer_storage");
         return new BufferStorage.Immutable();
      } else {
         return new BufferStorage.Mutable();
      }
   }

   public abstract GlBuffer createBuffer(DirectStateAccess dsa, @GpuBuffer.Usage int usage, long size);

   public abstract GlBuffer createBuffer(DirectStateAccess dsa, @GpuBuffer.Usage int usage, ByteBuffer data);

   private static class Immutable extends BufferStorage {
      @Override
      public GlBuffer createBuffer(final DirectStateAccess dsa, final @GpuBuffer.Usage int usage, final long size) {
         int buffer = dsa.createBuffer();
         dsa.bufferStorage(buffer, size, usage);
         return new GlBuffer.Direct(dsa, usage, size, buffer, true);
      }

      @Override
      public GlBuffer createBuffer(final DirectStateAccess dsa, final @GpuBuffer.Usage int usage, final ByteBuffer data) {
         int buffer = dsa.createBuffer();
         int size = data.remaining();
         dsa.bufferStorage(buffer, data, usage);
         return new GlBuffer.Direct(dsa, usage, size, buffer, true);
      }
   }

   private static class Mutable extends BufferStorage {
      @Override
      public GlBuffer createBuffer(final DirectStateAccess dsa, final @GpuBuffer.Usage int usage, final long size) {
         int buffer = dsa.createBuffer();
         dsa.bufferData(buffer, size, usage);
         return new GlBuffer.Direct(dsa, usage, size, buffer, false);
      }

      @Override
      public GlBuffer createBuffer(final DirectStateAccess dsa, final @GpuBuffer.Usage int usage, final ByteBuffer data) {
         int buffer = dsa.createBuffer();
         int size = data.remaining();
         dsa.bufferData(buffer, data, usage);
         return new GlBuffer.Direct(dsa, usage, size, buffer, false);
      }
   }
}
