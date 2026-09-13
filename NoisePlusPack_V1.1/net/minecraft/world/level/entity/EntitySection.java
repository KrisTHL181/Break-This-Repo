package net.minecraft.world.level.entity;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.stream.Stream;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public class EntitySection<T extends EntityAccess> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ClassInstanceMultiMap<T> storage;
   private Visibility chunkStatus;

   public EntitySection(Class<T> p_156831_, Visibility p_156832_) {
      this.chunkStatus = p_156832_;
      this.storage = new ClassInstanceMultiMap<>(p_156831_);
   }

   public void add(T p_188347_) {
      this.storage.add(p_188347_);
   }

   public boolean remove(T p_188356_) {
      return this.storage.remove(p_188356_);
   }

   public AbortableIterationConsumer.Continuation getEntities(AABB p_262016_, AbortableIterationConsumer<T> p_261863_) {
      for (T t : this.storage) {
         if (t.getBoundingBox().intersects(p_262016_) && p_261863_.accept(t).shouldAbort()) {
            return AbortableIterationConsumer.Continuation.ABORT;
         }
      }

      return AbortableIterationConsumer.Continuation.CONTINUE;
   }

   public <U extends T> AbortableIterationConsumer.Continuation getEntities(
      EntityTypeTest<T, U> p_188349_, AABB p_188350_, AbortableIterationConsumer<? super U> p_261535_
   ) {
      Collection<? extends T> collection = this.storage.find(p_188349_.getBaseClass());
      if (collection.isEmpty()) {
         return AbortableIterationConsumer.Continuation.CONTINUE;
      }

      for (T t : collection) {
         U u = (U)p_188349_.tryCast(t);
         if (u != null && t.getBoundingBox().intersects(p_188350_) && p_261535_.accept(u).shouldAbort()) {
            return AbortableIterationConsumer.Continuation.ABORT;
         }
      }

      return AbortableIterationConsumer.Continuation.CONTINUE;
   }

   public boolean isEmpty() {
      return this.storage.isEmpty();
   }

   public Stream<T> getEntities() {
      return this.storage.stream();
   }

   public Visibility getStatus() {
      return this.chunkStatus;
   }

   public Visibility updateChunkStatus(Visibility p_156839_) {
      Visibility visibility = this.chunkStatus;
      this.chunkStatus = p_156839_;
      return visibility;
   }

   @VisibleForDebug
   public int size() {
      return this.storage.size();
   }
}
