package com.mojang.blaze3d.framegraph;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class FrameGraphBuilder {
   private final List<FrameGraphBuilder.InternalVirtualResource<?>> internalResources = new ArrayList<>();
   private final List<FrameGraphBuilder.ExternalResource<?>> externalResources = new ArrayList<>();
   private final List<FrameGraphBuilder.Pass> passes = new ArrayList<>();

   public FramePass addPass(String p_362791_) {
      FrameGraphBuilder.Pass framegraphbuilder$pass = new FrameGraphBuilder.Pass(this.passes.size(), p_362791_);
      this.passes.add(framegraphbuilder$pass);
      return framegraphbuilder$pass;
   }

   public <T> ResourceHandle<T> importExternal(String p_363754_, T p_369377_) {
      FrameGraphBuilder.ExternalResource<T> externalresource = new FrameGraphBuilder.ExternalResource<>(p_363754_, null, p_369377_);
      this.externalResources.add(externalresource);
      return externalresource.handle;
   }

   public <T> ResourceHandle<T> createInternal(String p_360750_, ResourceDescriptor<T> p_363637_) {
      return this.createInternalResource(p_360750_, p_363637_, null).handle;
   }

   <T> FrameGraphBuilder.InternalVirtualResource<T> createInternalResource(
      String p_368654_, ResourceDescriptor<T> p_360922_, FrameGraphBuilder.@Nullable Pass p_362192_
   ) {
      int i = this.internalResources.size();
      FrameGraphBuilder.InternalVirtualResource<T> internalvirtualresource = new FrameGraphBuilder.InternalVirtualResource<>(i, p_368654_, p_362192_, p_360922_);
      this.internalResources.add(internalvirtualresource);
      return internalvirtualresource;
   }

   public void execute(GraphicsResourceAllocator p_368856_) {
      this.execute(p_368856_, FrameGraphBuilder.Inspector.NONE);
   }

   public void execute(GraphicsResourceAllocator p_367017_, FrameGraphBuilder.Inspector p_366308_) {
      BitSet bitset = this.identifyPassesToKeep();
      List<FrameGraphBuilder.Pass> list = new ArrayList<>(bitset.cardinality());
      BitSet bitset1 = new BitSet(this.passes.size());

      for (FrameGraphBuilder.Pass framegraphbuilder$pass : this.passes) {
         this.resolvePassOrder(framegraphbuilder$pass, bitset, bitset1, list);
      }

      this.assignResourceLifetimes(list);

      for (FrameGraphBuilder.Pass framegraphbuilder$pass1 : list) {
         for (FrameGraphBuilder.InternalVirtualResource<?> internalvirtualresource : framegraphbuilder$pass1.resourcesToAcquire) {
            p_366308_.acquireResource(internalvirtualresource.name);
            internalvirtualresource.acquire(p_367017_);
         }

         p_366308_.beforeExecutePass(framegraphbuilder$pass1.name);
         framegraphbuilder$pass1.task.run();
         p_366308_.afterExecutePass(framegraphbuilder$pass1.name);

         for (int i = framegraphbuilder$pass1.resourcesToRelease.nextSetBit(0); i >= 0; i = framegraphbuilder$pass1.resourcesToRelease.nextSetBit(i + 1)) {
            FrameGraphBuilder.InternalVirtualResource<?> internalvirtualresource1 = this.internalResources.get(i);
            p_366308_.releaseResource(internalvirtualresource1.name);
            internalvirtualresource1.release(p_367017_);
         }
      }
   }

   private BitSet identifyPassesToKeep() {
      Deque<FrameGraphBuilder.Pass> deque = new ArrayDeque<>(this.passes.size());
      BitSet bitset = new BitSet(this.passes.size());

      for (FrameGraphBuilder.VirtualResource<?> virtualresource : this.externalResources) {
         FrameGraphBuilder.Pass framegraphbuilder$pass = virtualresource.handle.createdBy;
         if (framegraphbuilder$pass != null) {
            this.discoverAllRequiredPasses(framegraphbuilder$pass, bitset, deque);
         }
      }

      for (FrameGraphBuilder.Pass framegraphbuilder$pass1 : this.passes) {
         if (framegraphbuilder$pass1.disableCulling) {
            this.discoverAllRequiredPasses(framegraphbuilder$pass1, bitset, deque);
         }
      }

      return bitset;
   }

   private void discoverAllRequiredPasses(FrameGraphBuilder.Pass p_362979_, BitSet p_368956_, Deque<FrameGraphBuilder.Pass> p_369416_) {
      p_369416_.add(p_362979_);

      while (!p_369416_.isEmpty()) {
         FrameGraphBuilder.Pass framegraphbuilder$pass = p_369416_.poll();
         if (!p_368956_.get(framegraphbuilder$pass.id)) {
            p_368956_.set(framegraphbuilder$pass.id);

            for (int i = framegraphbuilder$pass.requiredPassIds.nextSetBit(0); i >= 0; i = framegraphbuilder$pass.requiredPassIds.nextSetBit(i + 1)) {
               p_369416_.add(this.passes.get(i));
            }
         }
      }
   }

   private void resolvePassOrder(FrameGraphBuilder.Pass p_364839_, BitSet p_370123_, BitSet p_365587_, List<FrameGraphBuilder.Pass> p_366834_) {
      if (p_365587_.get(p_364839_.id)) {
         String s = p_365587_.stream().mapToObj(p_368248_ -> this.passes.get(p_368248_).name).collect(Collectors.joining(", "));
         throw new IllegalStateException("Frame graph cycle detected between " + s);
      }

      if (p_370123_.get(p_364839_.id)) {
         p_365587_.set(p_364839_.id);
         p_370123_.clear(p_364839_.id);

         for (int i = p_364839_.requiredPassIds.nextSetBit(0); i >= 0; i = p_364839_.requiredPassIds.nextSetBit(i + 1)) {
            this.resolvePassOrder(this.passes.get(i), p_370123_, p_365587_, p_366834_);
         }

         for (FrameGraphBuilder.Handle<?> handle : p_364839_.writesFrom) {
            for (int j = handle.readBy.nextSetBit(0); j >= 0; j = handle.readBy.nextSetBit(j + 1)) {
               if (j != p_364839_.id) {
                  this.resolvePassOrder(this.passes.get(j), p_370123_, p_365587_, p_366834_);
               }
            }
         }

         p_366834_.add(p_364839_);
         p_365587_.clear(p_364839_.id);
      }
   }

   private void assignResourceLifetimes(Collection<FrameGraphBuilder.Pass> p_366752_) {
      FrameGraphBuilder.Pass[] aframegraphbuilder$pass = new FrameGraphBuilder.Pass[this.internalResources.size()];

      for (FrameGraphBuilder.Pass framegraphbuilder$pass : p_366752_) {
         for (int i = framegraphbuilder$pass.requiredResourceIds.nextSetBit(0); i >= 0; i = framegraphbuilder$pass.requiredResourceIds.nextSetBit(i + 1)) {
            FrameGraphBuilder.InternalVirtualResource<?> internalvirtualresource = this.internalResources.get(i);
            FrameGraphBuilder.Pass framegraphbuilder$pass1 = aframegraphbuilder$pass[i];
            aframegraphbuilder$pass[i] = framegraphbuilder$pass;
            if (framegraphbuilder$pass1 == null) {
               framegraphbuilder$pass.resourcesToAcquire.add(internalvirtualresource);
            } else {
               framegraphbuilder$pass1.resourcesToRelease.clear(i);
            }

            framegraphbuilder$pass.resourcesToRelease.set(i);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ExternalResource<T> extends FrameGraphBuilder.VirtualResource<T> {
      private final T resource;

      public ExternalResource(String p_361702_, FrameGraphBuilder.@Nullable Pass p_362591_, T p_364944_) {
         super(p_361702_, p_362591_);
         this.resource = p_364944_;
      }

      @Override
      public T get() {
         return this.resource;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Handle<T> implements ResourceHandle<T> {
      final FrameGraphBuilder.VirtualResource<T> holder;
      private final int version;
      final FrameGraphBuilder.@Nullable Pass createdBy;
      final BitSet readBy = new BitSet();
      private FrameGraphBuilder.@Nullable Handle<T> aliasedBy;

      Handle(FrameGraphBuilder.VirtualResource<T> p_368119_, int p_368761_, FrameGraphBuilder.@Nullable Pass p_368110_) {
         this.holder = p_368119_;
         this.version = p_368761_;
         this.createdBy = p_368110_;
      }

      @Override
      public T get() {
         return this.holder.get();
      }

      FrameGraphBuilder.Handle<T> writeAndAlias(FrameGraphBuilder.Pass p_368239_) {
         if (this.holder.handle != this) {
            throw new IllegalStateException("Handle " + this + " is no longer valid, as its contents were moved into " + this.aliasedBy);
         }

         FrameGraphBuilder.Handle<T> handle = new FrameGraphBuilder.Handle<>(this.holder, this.version + 1, p_368239_);
         this.holder.handle = handle;
         this.aliasedBy = handle;
         return handle;
      }

      @Override
      public String toString() {
         return this.createdBy != null ? this.holder + "#" + this.version + " (from " + this.createdBy + ")" : this.holder + "#" + this.version;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Inspector {
      FrameGraphBuilder.Inspector NONE = new FrameGraphBuilder.Inspector() {};

      default void acquireResource(String p_362031_) {
      }

      default void releaseResource(String p_370224_) {
      }

      default void beforeExecutePass(String p_363319_) {
      }

      default void afterExecutePass(String p_368799_) {
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class InternalVirtualResource<T> extends FrameGraphBuilder.VirtualResource<T> {
      final int id;
      private final ResourceDescriptor<T> descriptor;
      private @Nullable T physicalResource;

      public InternalVirtualResource(int p_369414_, String p_364574_, FrameGraphBuilder.@Nullable Pass p_361395_, ResourceDescriptor<T> p_364455_) {
         super(p_364574_, p_361395_);
         this.id = p_369414_;
         this.descriptor = p_364455_;
      }

      @Override
      public T get() {
         return Objects.requireNonNull(this.physicalResource, "Resource is not currently available");
      }

      public void acquire(GraphicsResourceAllocator p_367587_) {
         if (this.physicalResource != null) {
            throw new IllegalStateException("Tried to acquire physical resource, but it was already assigned");
         }

         this.physicalResource = p_367587_.acquire(this.descriptor);
      }

      public void release(GraphicsResourceAllocator p_363217_) {
         if (this.physicalResource == null) {
            throw new IllegalStateException("Tried to release physical resource that was not allocated");
         }

         p_363217_.release(this.descriptor, this.physicalResource);
         this.physicalResource = null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   class Pass implements FramePass {
      final int id;
      final String name;
      final List<FrameGraphBuilder.Handle<?>> writesFrom = new ArrayList<>();
      final BitSet requiredResourceIds = new BitSet();
      final BitSet requiredPassIds = new BitSet();
      Runnable task = () -> {};
      final List<FrameGraphBuilder.InternalVirtualResource<?>> resourcesToAcquire = new ArrayList<>();
      final BitSet resourcesToRelease = new BitSet();
      boolean disableCulling;

      public Pass(final int p_367383_, final String p_361566_) {
         this.id = p_367383_;
         this.name = p_361566_;
      }

      private <T> void markResourceRequired(FrameGraphBuilder.Handle<T> p_364152_) {
         if (p_364152_.holder instanceof FrameGraphBuilder.InternalVirtualResource<?> internalvirtualresource) {
            this.requiredResourceIds.set(internalvirtualresource.id);
         }
      }

      private void markPassRequired(FrameGraphBuilder.Pass p_365988_) {
         this.requiredPassIds.set(p_365988_.id);
      }

      @Override
      public <T> ResourceHandle<T> createsInternal(String p_362510_, ResourceDescriptor<T> p_365149_) {
         FrameGraphBuilder.InternalVirtualResource<T> internalvirtualresource = FrameGraphBuilder.this.createInternalResource(p_362510_, p_365149_, this);
         this.requiredResourceIds.set(internalvirtualresource.id);
         return internalvirtualresource.handle;
      }

      @Override
      public <T> void reads(ResourceHandle<T> p_366230_) {
         this._reads((FrameGraphBuilder.Handle<T>)p_366230_);
      }

      private <T> void _reads(FrameGraphBuilder.Handle<T> p_362181_) {
         this.markResourceRequired(p_362181_);
         if (p_362181_.createdBy != null) {
            this.markPassRequired(p_362181_.createdBy);
         }

         p_362181_.readBy.set(this.id);
      }

      @Override
      public <T> ResourceHandle<T> readsAndWrites(ResourceHandle<T> p_366708_) {
         return this._readsAndWrites((FrameGraphBuilder.Handle<T>)p_366708_);
      }

      @Override
      public void requires(FramePass p_367159_) {
         this.requiredPassIds.set(((FrameGraphBuilder.Pass)p_367159_).id);
      }

      @Override
      public void disableCulling() {
         this.disableCulling = true;
      }

      private <T> FrameGraphBuilder.Handle<T> _readsAndWrites(FrameGraphBuilder.Handle<T> p_361681_) {
         this.writesFrom.add(p_361681_);
         this._reads(p_361681_);
         return p_361681_.writeAndAlias(this);
      }

      @Override
      public void executes(Runnable p_366784_) {
         this.task = p_366784_;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   @OnlyIn(Dist.CLIENT)
   abstract static class VirtualResource<T> {
      public final String name;
      public FrameGraphBuilder.Handle<T> handle;

      public VirtualResource(String p_364878_, FrameGraphBuilder.@Nullable Pass p_363310_) {
         this.name = p_364878_;
         this.handle = new FrameGraphBuilder.Handle<>(this, 0, p_363310_);
      }

      public abstract T get();

      @Override
      public String toString() {
         return this.name;
      }
   }
}
