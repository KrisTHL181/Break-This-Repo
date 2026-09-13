package com.mojang.blaze3d.resource;

public interface GraphicsResourceAllocator {
   GraphicsResourceAllocator UNPOOLED = new GraphicsResourceAllocator() {
      @Override
      public <T> T acquire(final ResourceDescriptor<T> descriptor) {
         T resource = descriptor.allocate();
         descriptor.prepare(resource);
         return resource;
      }

      @Override
      public <T> void release(final ResourceDescriptor<T> descriptor, final T resource) {
         descriptor.free(resource);
      }
   };

   <T> T acquire(ResourceDescriptor<T> descriptor);

   <T> void release(ResourceDescriptor<T> descriptor, T resource);
}
