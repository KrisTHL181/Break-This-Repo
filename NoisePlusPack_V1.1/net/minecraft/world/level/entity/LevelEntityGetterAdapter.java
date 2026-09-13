package net.minecraft.world.level.entity;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class LevelEntityGetterAdapter<T extends EntityAccess> implements LevelEntityGetter<T> {
   private final EntityLookup<T> visibleEntities;
   private final EntitySectionStorage<T> sectionStorage;

   public LevelEntityGetterAdapter(EntityLookup<T> p_156943_, EntitySectionStorage<T> p_156944_) {
      this.visibleEntities = p_156943_;
      this.sectionStorage = p_156944_;
   }

   @Override
   public @Nullable T get(int p_156947_) {
      return this.visibleEntities.getEntity(p_156947_);
   }

   @Override
   public @Nullable T get(UUID p_156959_) {
      return this.visibleEntities.getEntity(p_156959_);
   }

   @Override
   public Iterable<T> getAll() {
      return this.visibleEntities.getAllEntities();
   }

   @Override
   public <U extends T> void get(EntityTypeTest<T, U> p_261718_, AbortableIterationConsumer<U> p_262009_) {
      this.visibleEntities.getEntities(p_261718_, p_262009_);
   }

   @Override
   public void get(AABB p_156956_, Consumer<T> p_156957_) {
      this.sectionStorage.getEntities(p_156956_, AbortableIterationConsumer.forConsumer(p_156957_));
   }

   @Override
   public <U extends T> void get(EntityTypeTest<T, U> p_261696_, AABB p_261693_, AbortableIterationConsumer<U> p_261719_) {
      this.sectionStorage.getEntities(p_261696_, p_261693_, p_261719_);
   }
}
