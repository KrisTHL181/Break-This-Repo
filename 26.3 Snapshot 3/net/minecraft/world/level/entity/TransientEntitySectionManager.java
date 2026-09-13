package net.minecraft.world.level.entity;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class TransientEntitySectionManager<T extends EntityAccess> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final LevelCallback<T> callbacks;
   private final EntityLookup<T> entityStorage;
   private final EntitySectionStorage<T> sectionStorage;
   private final LongSet tickingChunks = new LongOpenHashSet();
   private final LevelEntityGetter<T> entityGetter;

   public TransientEntitySectionManager(final Class<T> entityClass, final LevelCallback<T> callbacks) {
      this.entityStorage = new EntityLookup<>();
      this.sectionStorage = new EntitySectionStorage<>(entityClass, key -> this.tickingChunks.contains(key) ? Visibility.TICKING : Visibility.TRACKED);
      this.callbacks = callbacks;
      this.entityGetter = new LevelEntityGetterAdapter<>(this.entityStorage, this.sectionStorage);
   }

   public void startTicking(final ChunkPos pos) {
      long chunkKey = pos.pack();
      this.tickingChunks.add(chunkKey);
      this.sectionStorage.getExistingSectionsInChunk(chunkKey).forEach(section -> {
         Visibility previousStatus = section.updateChunkStatus(Visibility.TICKING);
         if (!previousStatus.isTicking()) {
            section.getEntities().filter(e -> !e.isAlwaysTicking()).forEach(this.callbacks::onTickingStart);
         }
      });
   }

   public void stopTicking(final ChunkPos pos) {
      long chunkKey = pos.pack();
      this.tickingChunks.remove(chunkKey);
      this.sectionStorage.getExistingSectionsInChunk(chunkKey).forEach(section -> {
         Visibility previousStatus = section.updateChunkStatus(Visibility.TRACKED);
         if (previousStatus.isTicking()) {
            section.getEntities().filter(e -> !e.isAlwaysTicking()).forEach(this.callbacks::onTickingEnd);
         }
      });
   }

   public LevelEntityGetter<T> getEntityGetter() {
      return this.entityGetter;
   }

   public void addEntity(final T entity) {
      this.entityStorage.add(entity);
      long sectionKey = SectionPos.asLong(entity.blockPosition());
      EntitySection<T> entitySection = this.sectionStorage.getOrCreateSection(sectionKey);
      entitySection.add(entity);
      entity.setLevelCallback(new TransientEntitySectionManager.Callback(entity, sectionKey, entitySection));
      this.callbacks.onCreated(entity);
      this.callbacks.onTrackingStart(entity);
      if (entity.isAlwaysTicking() || entitySection.getStatus().isTicking()) {
         this.callbacks.onTickingStart(entity);
      }
   }

   @VisibleForDebug
   public int count() {
      return this.entityStorage.count();
   }

   private void removeSectionIfEmpty(final long sectionPos, final EntitySection<T> section) {
      if (section.isEmpty()) {
         this.sectionStorage.remove(sectionPos);
      }
   }

   @VisibleForDebug
   public String gatherStats() {
      return this.entityStorage.count() + "," + this.sectionStorage.count() + "," + this.tickingChunks.size();
   }

   private class Callback implements EntityInLevelCallback {
      private final T entity;
      private long currentSectionKey;
      private EntitySection<T> currentSection;

      private Callback(final T entity, final long currentSectionKey, final EntitySection<T> currentSection) {
         this.entity = entity;
         this.currentSectionKey = currentSectionKey;
         this.currentSection = currentSection;
      }

      @Override
      public void onMove() {
         BlockPos pos = this.entity.blockPosition();
         long newSectionPos = SectionPos.asLong(pos);
         if (newSectionPos != this.currentSectionKey) {
            Visibility previousStatus = this.currentSection.getStatus();
            if (!this.currentSection.remove(this.entity)) {
               TransientEntitySectionManager.LOGGER
                  .warn("Entity {} wasn't found in section {} (moving to {})", new Object[]{this.entity, SectionPos.of(this.currentSectionKey), newSectionPos});
            }

            TransientEntitySectionManager.this.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
            EntitySection<T> newSection = TransientEntitySectionManager.this.sectionStorage.getOrCreateSection(newSectionPos);
            newSection.add(this.entity);
            this.currentSection = newSection;
            this.currentSectionKey = newSectionPos;
            TransientEntitySectionManager.this.callbacks.onSectionChange(this.entity);
            if (!this.entity.isAlwaysTicking()) {
               boolean wasTicking = previousStatus.isTicking();
               boolean isTicking = newSection.getStatus().isTicking();
               if (wasTicking && !isTicking) {
                  TransientEntitySectionManager.this.callbacks.onTickingEnd(this.entity);
               } else if (!wasTicking && isTicking) {
                  TransientEntitySectionManager.this.callbacks.onTickingStart(this.entity);
               }
            }
         }
      }

      @Override
      public void onRemove(final Entity.RemovalReason reason) {
         if (!this.currentSection.remove(this.entity)) {
            TransientEntitySectionManager.LOGGER
               .warn("Entity {} wasn't found in section {} (destroying due to {})", new Object[]{this.entity, SectionPos.of(this.currentSectionKey), reason});
         }

         Visibility status = this.currentSection.getStatus();
         if (status.isTicking() || this.entity.isAlwaysTicking()) {
            TransientEntitySectionManager.this.callbacks.onTickingEnd(this.entity);
         }

         TransientEntitySectionManager.this.callbacks.onTrackingEnd(this.entity);
         TransientEntitySectionManager.this.callbacks.onDestroyed(this.entity);
         TransientEntitySectionManager.this.entityStorage.remove(this.entity);
         this.entity.setLevelCallback(NULL);
         TransientEntitySectionManager.this.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
      }
   }
}
