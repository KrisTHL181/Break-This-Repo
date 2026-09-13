package net.minecraft.world.level.entity;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class LevelEntityGetterAdapter<T extends EntityAccess> implements LevelEntityGetter<T> {
   private final EntityLookup<T> visibleEntities;
   private final EntitySectionStorage<T> sectionStorage;

   public LevelEntityGetterAdapter(final EntityLookup<T> visibleEntities, final EntitySectionStorage<T> sectionStorage) {
      this.visibleEntities = visibleEntities;
      this.sectionStorage = sectionStorage;
   }

   @Override
   public @Nullable T get(final int id) {
      return this.visibleEntities.getEntity(id);
   }

   @Override
   public @Nullable T get(final UUID id) {
      return this.visibleEntities.getEntity(id);
   }

   @Override
   public Iterable<T> getAll() {
      return this.visibleEntities.getAllEntities();
   }

   @Override
   public <U extends T> void get(final EntityTypeTest<T, U> type, final AbortableIterationConsumer<U> consumer) {
      this.visibleEntities.getEntities(type, consumer);
   }

   @Override
   public void get(final AABB bb, final Consumer<T> output) {
      this.sectionStorage.getEntities(bb, AbortableIterationConsumer.forConsumer(output));
   }

   @Override
   public <U extends T> void get(final EntityTypeTest<T, U> type, final AABB bb, final AbortableIterationConsumer<U> consumer) {
      this.sectionStorage.getEntities(type, bb, consumer);
   }
}
