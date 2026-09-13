package com.mojang.blaze3d.resource;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

public class CrossFrameResourcePool implements GraphicsResourceAllocator, AutoCloseable {
   private final int framesToKeepResource;
   private final Deque<CrossFrameResourcePool.ResourceEntry<?>> pool = new ArrayDeque<>();

   public CrossFrameResourcePool(final int framesToKeepResource) {
      this.framesToKeepResource = framesToKeepResource;
   }

   public void endFrame() {
      Iterator<? extends CrossFrameResourcePool.ResourceEntry<?>> iterator = this.pool.iterator();

      while (iterator.hasNext()) {
         CrossFrameResourcePool.ResourceEntry<?> entry = (CrossFrameResourcePool.ResourceEntry<?>)iterator.next();
         if (entry.framesToLive-- == 0) {
            entry.close();
            iterator.remove();
         }
      }
   }

   @Override
   public <T> T acquire(final ResourceDescriptor<T> descriptor) {
      T resource = this.acquireWithoutPreparing(descriptor);
      descriptor.prepare(resource);
      return resource;
   }

   private <T> T acquireWithoutPreparing(final ResourceDescriptor<T> descriptor) {
      Iterator<? extends CrossFrameResourcePool.ResourceEntry<?>> iterator = this.pool.iterator();

      while (iterator.hasNext()) {
         CrossFrameResourcePool.ResourceEntry<?> entry = (CrossFrameResourcePool.ResourceEntry<?>)iterator.next();
         if (descriptor.canUsePhysicalResource(entry.descriptor)) {
            iterator.remove();
            return (T)entry.value;
         }
      }

      return descriptor.allocate();
   }

   @Override
   public <T> void release(final ResourceDescriptor<T> descriptor, final T resource) {
      this.pool.addFirst(new CrossFrameResourcePool.ResourceEntry<>(descriptor, resource, this.framesToKeepResource));
   }

   public void clear() {
      this.pool.forEach(CrossFrameResourcePool.ResourceEntry::close);
      this.pool.clear();
   }

   @Override
   public void close() {
      this.clear();
   }

   @VisibleForTesting
   protected Collection<CrossFrameResourcePool.ResourceEntry<?>> entries() {
      return this.pool;
   }

   @VisibleForTesting
   protected static final class ResourceEntry<T> implements AutoCloseable {
      private final ResourceDescriptor<T> descriptor;
      private final T value;
      private int framesToLive;

      private ResourceEntry(final ResourceDescriptor<T> descriptor, final T value, final int framesToLive) {
         this.descriptor = descriptor;
         this.value = value;
         this.framesToLive = framesToLive;
      }

      @Override
      public void close() {
         this.descriptor.free(this.value);
      }
   }
}
