package net.minecraft.world.level.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import java.util.Objects;
import java.util.Spliterators;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class EntitySectionStorage<T extends EntityAccess> {
   public static final int CHONKY_ENTITY_SEARCH_GRACE = 2;
   public static final int MAX_NON_CHONKY_ENTITY_SIZE = 4;
   private final Class<T> entityClass;
   private final Long2ObjectFunction<Visibility> intialSectionVisibility;
   private final Long2ObjectMap<EntitySection<T>> sections = new Long2ObjectOpenHashMap();
   private final LongSortedSet sectionIds = new LongAVLTreeSet();

   public EntitySectionStorage(final Class<T> entityClass, final Long2ObjectFunction<Visibility> intialSectionVisibility) {
      this.entityClass = entityClass;
      this.intialSectionVisibility = intialSectionVisibility;
   }

   public void forEachAccessibleNonEmptySection(final AABB bb, final AbortableIterationConsumer<EntitySection<T>> output) {
      int xMin = SectionPos.posToSectionCoord(bb.minX - 2.0);
      int yMin = SectionPos.posToSectionCoord(bb.minY - 4.0);
      int zMin = SectionPos.posToSectionCoord(bb.minZ - 2.0);
      int xMax = SectionPos.posToSectionCoord(bb.maxX + 2.0);
      int yMax = SectionPos.posToSectionCoord(bb.maxY + 0.0);
      int zMax = SectionPos.posToSectionCoord(bb.maxZ + 2.0);

      for (int x = xMin; x <= xMax; x++) {
         long lowestAbsoluteSectionKey = SectionPos.asLong(x, 0, 0);
         long highestAbsoluteSectionKey = SectionPos.asLong(x, -1, -1);
         LongIterator it = this.sectionIds.subSet(lowestAbsoluteSectionKey, highestAbsoluteSectionKey + 1L).iterator();

         while (it.hasNext()) {
            long sectionKey = it.nextLong();
            int y = SectionPos.y(sectionKey);
            int z = SectionPos.z(sectionKey);
            if (y >= yMin && y <= yMax && z >= zMin && z <= zMax) {
               EntitySection<T> entitySection = (EntitySection<T>)this.sections.get(sectionKey);
               if (entitySection != null && !entitySection.isEmpty() && entitySection.getStatus().isAccessible() && output.accept(entitySection).shouldAbort()) {
                  return;
               }
            }
         }
      }
   }

   public LongStream getExistingSectionPositionsInChunk(final long chunkKey) {
      int x = ChunkPos.getX(chunkKey);
      int z = ChunkPos.getZ(chunkKey);
      LongSortedSet chunkSections = this.getChunkSections(x, z);
      if (chunkSections.isEmpty()) {
         return LongStream.empty();
      }

      OfLong iterator = chunkSections.iterator();
      return StreamSupport.longStream(Spliterators.spliteratorUnknownSize(iterator, 1301), false);
   }

   private LongSortedSet getChunkSections(final int x, final int z) {
      long lowestAbsoluteSectionKey = SectionPos.asLong(x, 0, z);
      long highestAbsoluteSectionKey = SectionPos.asLong(x, -1, z);
      return this.sectionIds.subSet(lowestAbsoluteSectionKey, highestAbsoluteSectionKey + 1L);
   }

   public Stream<EntitySection<T>> getExistingSectionsInChunk(final long chunkKey) {
      return this.getExistingSectionPositionsInChunk(chunkKey).<EntitySection<T>>mapToObj(this.sections::get).filter(Objects::nonNull);
   }

   private static long getChunkKeyFromSectionKey(final long sectionPos) {
      return ChunkPos.pack(SectionPos.x(sectionPos), SectionPos.z(sectionPos));
   }

   public EntitySection<T> getOrCreateSection(final long key) {
      return (EntitySection<T>)this.sections.computeIfAbsent(key, this::createSection);
   }

   public @Nullable EntitySection<T> getSection(final long key) {
      return (EntitySection<T>)this.sections.get(key);
   }

   private EntitySection<T> createSection(final long sectionPos) {
      long chunkPos = getChunkKeyFromSectionKey(sectionPos);
      Visibility chunkStatus = (Visibility)this.intialSectionVisibility.get(chunkPos);
      this.sectionIds.add(sectionPos);
      return new EntitySection<>(this.entityClass, chunkStatus);
   }

   public LongSet getAllChunksWithExistingSections() {
      LongSet chunks = new LongOpenHashSet();
      this.sections.keySet().forEach(sectionKey -> chunks.add(getChunkKeyFromSectionKey(sectionKey)));
      return chunks;
   }

   public void getEntities(final AABB bb, final AbortableIterationConsumer<T> output) {
      this.forEachAccessibleNonEmptySection(bb, section -> section.getEntities(bb, output));
   }

   public <U extends T> void getEntities(final EntityTypeTest<T, U> type, final AABB bb, final AbortableIterationConsumer<U> consumer) {
      this.forEachAccessibleNonEmptySection(bb, section -> section.getEntities(type, bb, consumer));
   }

   public void remove(final long sectionKey) {
      this.sections.remove(sectionKey);
      this.sectionIds.remove(sectionKey);
   }

   @VisibleForDebug
   public int count() {
      return this.sectionIds.size();
   }
}
