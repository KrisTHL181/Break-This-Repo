package net.minecraft.world.level.entity;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.stream.Stream;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.util.Continuation;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public class EntitySection<T extends EntityAccess> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ClassInstanceMultiMap<T> storage;
   private Visibility chunkStatus;

   public EntitySection(final Class<T> entityClass, final Visibility chunkStatus) {
      this.chunkStatus = chunkStatus;
      this.storage = new ClassInstanceMultiMap<>(entityClass);
   }

   public void add(final T entity) {
      this.storage.add(entity);
   }

   public boolean remove(final T entity) {
      return this.storage.remove(entity);
   }

   public Continuation getEntities(final AABB bb, final AbortableIterationConsumer<T> entities) {
      for (T entity : this.storage) {
         if (entity.getBoundingBox().intersects(bb) && entities.accept(entity).shouldAbort()) {
            return Continuation.ABORT;
         }
      }

      return Continuation.CONTINUE;
   }

   public <U extends T> Continuation getEntities(final EntityTypeTest<T, U> type, final AABB bb, final AbortableIterationConsumer<? super U> consumer) {
      Collection<? extends T> foundEntities = this.storage.find(type.getBaseClass());
      if (foundEntities.isEmpty()) {
         return Continuation.CONTINUE;
      }

      for (T entity : foundEntities) {
         U maybeEntity = (U)type.tryCast(entity);
         if (maybeEntity != null && entity.getBoundingBox().intersects(bb) && consumer.accept(maybeEntity).shouldAbort()) {
            return Continuation.ABORT;
         }
      }

      return Continuation.CONTINUE;
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

   public Visibility updateChunkStatus(final Visibility chunkStatus) {
      Visibility prev = this.chunkStatus;
      this.chunkStatus = chunkStatus;
      return prev;
   }

   @VisibleForDebug
   public int size() {
      return this.storage.size();
   }
}
