package net.minecraft.world.level.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * EntitySectionStorage — 实体区块节存储（MCRe NoiseFarlands 对象化版）
 * 原版以 long 打包键（SectionPos.asLong）+ LongAVLTreeSet 区间查询，
 * 本版以 SectionPos 对象为键，TreeSet 用 (x, z, y) 比较器模拟原打包排序（x 高位优先）。
 */
public class EntitySectionStorage<T extends EntityAccess> {
    public static final int CHONKY_ENTITY_SEARCH_GRACE = 2;
    public static final int MAX_NON_CHONKY_ENTITY_SIZE = 4;
    private final Class<T> entityClass;
    private final Function<ChunkPos, Visibility> intialSectionVisibility;
    private final Map<SectionPos, EntitySection<T>> sections = new HashMap<>();
    private final TreeSet<SectionPos> sectionIds = new TreeSet<>(EntitySectionStorage::compareSections);

    /** 与原版 SectionPos.asLong 打包排序一致：x（高位）→ z → y */
    private static int compareSections(final SectionPos a, final SectionPos b) {
        int cx = Integer.compare(a.x(), b.x());
        if (cx != 0) return cx;
        int cz = Integer.compare(a.z(), b.z());
        return cz != 0 ? cz : Integer.compare(a.y(), b.y());
    }

    public EntitySectionStorage(final Class<T> entityClass, final Function<ChunkPos, Visibility> intialSectionVisibility) {
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
            SectionPos lowestAbsoluteSectionKey = SectionPos.of(x, 0, 0);
            SectionPos highestAbsoluteSectionKey = SectionPos.of(x, Integer.MAX_VALUE, Integer.MAX_VALUE);
            for (SectionPos sectionKey : this.sectionIds.subSet(lowestAbsoluteSectionKey, true, highestAbsoluteSectionKey, true)) {
                int y = sectionKey.y();
                int z = sectionKey.z();
                if (y >= yMin && y <= yMax && z >= zMin && z <= zMax) {
                    EntitySection<T> entitySection = this.sections.get(sectionKey);
                    if (entitySection != null
                        && !entitySection.isEmpty()
                        && entitySection.getStatus().isAccessible()
                        && output.accept(entitySection).shouldAbort()) {
                        return;
                    }
                }
            }
        }
    }

    public Stream<SectionPos> getExistingSectionPositionsInChunk(final ChunkPos chunkKey) {
        var chunkSections = this.getChunkSections((int)chunkKey.x(), (int)chunkKey.z());
        if (chunkSections.isEmpty()) {
            return Stream.empty();
        }

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(chunkSections.iterator(), 1301), false);
    }

    private TreeSet<SectionPos> getChunkSections(final int x, final int z) {
        // 原版 subSet(asLong(x,0,z), asLong(x,-1,z)+1)：覆盖 (x, z, 任意 y)
        return (TreeSet<SectionPos>)this.sectionIds.subSet(SectionPos.of(x, 0, z), SectionPos.of(x, 0, z + 1));
    }

    public Stream<EntitySection<T>> getExistingSectionsInChunk(final ChunkPos chunkKey) {
        return this.getExistingSectionPositionsInChunk(chunkKey).map(this.sections::get).filter(Objects::nonNull);
    }

    private static ChunkPos getChunkKeyFromSectionKey(final SectionPos sectionPos) {
        return new ChunkPos(sectionPos.x(), sectionPos.z());
    }

    public EntitySection<T> getOrCreateSection(final SectionPos key) {
        return this.sections.computeIfAbsent(key, this::createSection);
    }

    public @Nullable EntitySection<T> getSection(final SectionPos key) {
        return this.sections.get(key);
    }

    private EntitySection<T> createSection(final SectionPos sectionPos) {
        ChunkPos chunkPos = getChunkKeyFromSectionKey(sectionPos);
        Visibility chunkStatus = this.intialSectionVisibility.apply(chunkPos);
        this.sectionIds.add(sectionPos);
        return new EntitySection<>(this.entityClass, chunkStatus);
    }

    public java.util.Set<ChunkPos> getAllChunksWithExistingSections() {
        java.util.HashSet<ChunkPos> chunks = new java.util.HashSet<>();
        this.sections.keySet().forEach(sectionKey -> chunks.add(getChunkKeyFromSectionKey(sectionKey)));
        return chunks;
    }

    public void getEntities(final AABB bb, final AbortableIterationConsumer<T> output) {
        this.forEachAccessibleNonEmptySection(bb, section -> section.getEntities(bb, output));
    }

    public <U extends T> void getEntities(final EntityTypeTest<T, U> type, final AABB bb, final AbortableIterationConsumer<U> consumer) {
        this.forEachAccessibleNonEmptySection(bb, section -> section.getEntities(type, bb, consumer));
    }

    public void remove(final SectionPos sectionKey) {
        this.sections.remove(sectionKey);
        this.sectionIds.remove(sectionKey);
    }

    @VisibleForDebug
    public int count() {
        return this.sectionIds.size();
    }
}
