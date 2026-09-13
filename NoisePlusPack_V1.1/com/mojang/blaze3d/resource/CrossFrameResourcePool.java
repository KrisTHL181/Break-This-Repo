package com.mojang.blaze3d.resource;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CrossFrameResourcePool implements GraphicsResourceAllocator, AutoCloseable {
   private final int framesToKeepResource;
   private final Deque<CrossFrameResourcePool.ResourceEntry<?>> pool = new ArrayDeque<>();

   public CrossFrameResourcePool(int p_363418_) {
      this.framesToKeepResource = p_363418_;
   }

   public void endFrame() {
      Iterator<? extends CrossFrameResourcePool.ResourceEntry<?>> iterator = this.pool.iterator();

      while (iterator.hasNext()) {
         CrossFrameResourcePool.ResourceEntry<?> resourceentry = (CrossFrameResourcePool.ResourceEntry<?>)iterator.next();
         if (resourceentry.framesToLive-- == 0) {
            resourceentry.close();
            iterator.remove();
         }
      }
   }

   @Override
   public <T> T acquire(ResourceDescriptor<T> p_364134_) {
      T t = this.acquireWithoutPreparing(p_364134_);
      p_364134_.prepare(t);
      return t;
   }

   private <T> T acquireWithoutPreparing(ResourceDescriptor<T> p_391233_) {
      Iterator<? extends CrossFrameResourcePool.ResourceEntry<?>> iterator = this.pool.iterator();

      while (iterator.hasNext()) {
         CrossFrameResourcePool.ResourceEntry<?> resourceentry = (CrossFrameResourcePool.ResourceEntry<?>)iterator.next();
         if (p_391233_.canUsePhysicalResource(resourceentry.descriptor)) {
            iterator.remove();
            return (T)resourceentry.value;
         }
      }

      return p_391233_.allocate();
   }

   @Override
   public <T> void release(ResourceDescriptor<T> p_370226_, T p_369520_) {
      this.pool.addFirst(new CrossFrameResourcePool.ResourceEntry<>(p_370226_, p_369520_, this.framesToKeepResource));
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

   @OnlyIn(Dist.CLIENT)
   @VisibleForTesting
   protected static final class ResourceEntry<T> implements AutoCloseable {
      final ResourceDescriptor<T> descriptor;
      final T value;
      int framesToLive;

      ResourceEntry(ResourceDescriptor<T> p_361734_, T p_367770_, int p_363856_) {
         this.descriptor = p_361734_;
         this.value = p_367770_;
         this.framesToLive = p_363856_;
      }

      @Override
      public void close() {
         this.descriptor.free(this.value);
      }
   }
}
