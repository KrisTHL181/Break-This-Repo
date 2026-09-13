package com.mojang.jtracy;

public class MemoryPool {
   static final MemoryPool UNAVAILABLE = new MemoryPool(0L);
   private final long id;

   MemoryPool(long id) {
      this.id = id;
   }

   public void malloc(long pointer, int size) {
      if (this != UNAVAILABLE) {
         TracyBindings.mallocNamed(this.id, pointer, size);
      }
   }

   public void free(long pointer) {
      if (this != UNAVAILABLE) {
         TracyBindings.freeNamed(this.id, pointer);
      }
   }
}
