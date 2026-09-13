package net.minecraft.world.level.entity;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public interface LevelEntityGetter<T extends EntityAccess> {
   @Nullable T get(final int id);

   @Nullable T get(final UUID id);

   Iterable<T> getAll();

   <U extends T> void get(final EntityTypeTest<T, U> type, final AbortableIterationConsumer<U> consumer);

   void get(final AABB bb, final Consumer<T> output);

   <U extends T> void get(final EntityTypeTest<T, U> type, final AABB bb, final AbortableIterationConsumer<U> consumer);
}
