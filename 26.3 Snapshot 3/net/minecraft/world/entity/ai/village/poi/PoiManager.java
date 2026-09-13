package net.minecraft.world.entity.ai.village.poi;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.SectionTracker;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.debug.DebugPoiInfo;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkIOErrorReporter;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import org.jspecify.annotations.Nullable;

public class PoiManager extends SectionStorage<PoiSection, PoiSection.Packed> {
   public static final int MAX_VILLAGE_DISTANCE = 6;
   public static final int VILLAGE_SECTION_SIZE = 1;
   private final PoiManager.DistanceTracker distanceTracker;
   private final LongSet loadedChunks = new LongOpenHashSet();

   public PoiManager(
      final RegionStorageInfo info,
      final Path folder,
      final DataFixer fixerUpper,
      final boolean sync,
      final RegistryAccess registryAccess,
      final ChunkIOErrorReporter errorReporter,
      final LevelHeightAccessor levelHeightAccessor
   ) {
      super(
         new SimpleRegionStorage(info, folder, fixerUpper, sync, DataFixTypes.POI_CHUNK),
         PoiSection.Packed.CODEC,
         PoiSection::pack,
         PoiSection.Packed::unpack,
         PoiSection::new,
         registryAccess,
         errorReporter,
         levelHeightAccessor
      );
      this.distanceTracker = new PoiManager.DistanceTracker();
   }

   public @Nullable PoiRecord add(final BlockPos pos, final Holder<PoiType> type) {
      return this.getOrCreate(SectionPos.asLong(pos)).add(pos, type);
   }

   public void remove(final BlockPos pos) {
      this.getOrLoad(SectionPos.asLong(pos)).ifPresent(poiSection -> poiSection.remove(pos));
   }

   public long getCountInRange(final Predicate<Holder<PoiType>> predicate, final BlockPos center, final int radius, final PoiManager.Occupancy occupancy) {
      return this.getInRange(predicate, center, radius, occupancy).count();
   }

   public boolean existsAtPosition(final ResourceKey<PoiType> poiType, final BlockPos blockPos) {
      return this.exists(blockPos, p -> p.is(poiType));
   }

   public Stream<PoiRecord> getInSquare(
      final Predicate<Holder<PoiType>> predicate, final BlockPos center, final int radius, final PoiManager.Occupancy occupancy
   ) {
      int chunkRadius = Math.floorDiv(radius, 16) + 1;
      return ChunkPos.rangeClosed(ChunkPos.containing(center), chunkRadius).flatMap(pos -> this.getInChunk(predicate, pos, occupancy)).filter(record -> {
         BlockPos pos = record.getPos();
         return Math.abs(pos.getX() - center.getX()) <= radius && Math.abs(pos.getZ() - center.getZ()) <= radius;
      });
   }

   public Stream<PoiRecord> getInRange(
      final Predicate<Holder<PoiType>> predicate, final BlockPos center, final int radius, final PoiManager.Occupancy occupancy
   ) {
      int radiusSqr = radius * radius;
      return this.getInSquare(predicate, center, radius, occupancy).filter(r -> r.getPos().distSqr(center) <= radiusSqr);
   }

   @VisibleForDebug
   public Stream<PoiRecord> getInChunk(final Predicate<Holder<PoiType>> predicate, final ChunkPos chunkPos, final PoiManager.Occupancy occupancy) {
      return IntStream.rangeClosed(this.levelHeightAccessor.getMinSectionY(), this.levelHeightAccessor.getMaxSectionY())
         .boxed()
         .map(sectionY -> this.getOrLoad(SectionPos.of(chunkPos, sectionY).asLong()))
         .filter(Optional::isPresent)
         .flatMap(poiSection -> poiSection.get().getRecords(predicate, occupancy));
   }

   public Stream<BlockPos> findAll(
      final Predicate<Holder<PoiType>> predicate,
      final Predicate<BlockPos> filter,
      final BlockPos center,
      final int radius,
      final PoiManager.Occupancy occupancy
   ) {
      return this.getInRange(predicate, center, radius, occupancy).map(PoiRecord::getPos).filter(filter);
   }

   public Stream<Pair<Holder<PoiType>, BlockPos>> findAllWithType(
      final Predicate<Holder<PoiType>> predicate,
      final Predicate<BlockPos> filter,
      final BlockPos center,
      final int radius,
      final PoiManager.Occupancy occupancy
   ) {
      return this.getInRange(predicate, center, radius, occupancy).filter(p -> filter.test(p.getPos())).map(p -> Pair.of(p.getPoiType(), p.getPos()));
   }

   public Stream<Pair<Holder<PoiType>, BlockPos>> findAllClosestFirstWithType(
      final Predicate<Holder<PoiType>> predicate,
      final Predicate<BlockPos> filter,
      final BlockPos center,
      final int radius,
      final PoiManager.Occupancy occupancy
   ) {
      return this.findAllWithType(predicate, filter, center, radius, occupancy)
         .sorted(Comparator.comparingDouble(p -> ((BlockPos)p.getSecond()).distSqr(center)));
   }

   public Optional<BlockPos> find(
      final Predicate<Holder<PoiType>> predicate,
      final Predicate<BlockPos> filter,
      final BlockPos center,
      final int radius,
      final PoiManager.Occupancy occupancy
   ) {
      return this.findAll(predicate, filter, center, radius, occupancy).findFirst();
   }

   public Optional<BlockPos> findClosest(
      final Predicate<Holder<PoiType>> predicate, final BlockPos center, final int radius, final PoiManager.Occupancy occupancy
   ) {
      return this.getInRange(predicate, center, radius, occupancy).map(PoiRecord::getPos).min(Comparator.comparingDouble(pos -> pos.distSqr(center)));
   }

   public Optional<Pair<Holder<PoiType>, BlockPos>> findClosestWithType(
      final Predicate<Holder<PoiType>> predicate, final BlockPos center, final int radius, final PoiManager.Occupancy occupancy
   ) {
      return this.getInRange(predicate, center, radius, occupancy)
         .min(Comparator.comparingDouble(r -> r.getPos().distSqr(center)))
         .map(p -> Pair.of(p.getPoiType(), p.getPos()));
   }

   public Optional<BlockPos> findClosest(
      final Predicate<Holder<PoiType>> predicate,
      final Predicate<BlockPos> filter,
      final BlockPos center,
      final int radius,
      final PoiManager.Occupancy occupancy
   ) {
      return this.getInRange(predicate, center, radius, occupancy)
         .map(PoiRecord::getPos)
         .filter(filter)
         .min(Comparator.comparingDouble(pos -> pos.distSqr(center)));
   }

   public Optional<BlockPos> take(
      final Predicate<Holder<PoiType>> predicate, final BiPredicate<Holder<PoiType>, BlockPos> filter, final BlockPos center, final int radius
   ) {
      return this.getInRange(predicate, center, radius, PoiManager.Occupancy.HAS_SPACE)
         .filter(poi -> filter.test(poi.getPoiType(), poi.getPos()))
         .findFirst()
         .map(r -> {
            r.acquireTicket();
            return r.getPos();
         });
   }

   public Optional<BlockPos> getRandom(
      final Predicate<Holder<PoiType>> predicate,
      final Predicate<BlockPos> filter,
      final PoiManager.Occupancy occupancy,
      final BlockPos center,
      final int radius,
      final RandomSource random
   ) {
      List<PoiRecord> collect = Util.toShuffledList(this.getInRange(predicate, center, radius, occupancy), random);
      return collect.stream().filter(poi -> filter.test(poi.getPos())).findFirst().map(PoiRecord::getPos);
   }

   public boolean release(final BlockPos pos) {
      return this.getOrLoad(SectionPos.asLong(pos))
         .map(section -> section.release(pos))
         .orElseThrow(() -> Util.pauseInIde(new IllegalStateException("POI never registered at " + pos)));
   }

   public boolean exists(final BlockPos pos, final Predicate<Holder<PoiType>> predicate) {
      return this.getOrLoad(SectionPos.asLong(pos)).map(s -> s.exists(pos, predicate)).orElse(false);
   }

   public Optional<Holder<PoiType>> getType(final BlockPos pos) {
      return this.getOrLoad(SectionPos.asLong(pos)).flatMap(section -> section.getType(pos));
   }

   @VisibleForDebug
   public @Nullable DebugPoiInfo getDebugPoiInfo(final BlockPos pos) {
      return this.getOrLoad(SectionPos.asLong(pos)).flatMap(section -> section.getDebugPoiInfo(pos)).orElse(null);
   }

   public int sectionsToVillage(final SectionPos sectionPos) {
      this.distanceTracker.runAllUpdates();
      return this.distanceTracker.getLevel(sectionPos.asLong());
   }

   private boolean isVillageCenter(final long sectionPos) {
      Optional<PoiSection> section = this.get(sectionPos);
      return section == null
         ? false
         : section.<Boolean>map(s -> s.getRecords(e -> e.is(PoiTypeTags.VILLAGE), PoiManager.Occupancy.IS_OCCUPIED).findAny().isPresent()).orElse(false);
   }

   @Override
   public void tick(final BooleanSupplier haveTime) {
      super.tick(haveTime);
      this.distanceTracker.runAllUpdates();
   }

   @Override
   protected void setDirty(final long sectionPos) {
      super.setDirty(sectionPos);
      this.distanceTracker.update(sectionPos, this.distanceTracker.getLevelFromSource(sectionPos), false);
   }

   @Override
   protected void onSectionLoad(final long sectionPos) {
      this.distanceTracker.update(sectionPos, this.distanceTracker.getLevelFromSource(sectionPos), false);
   }

   public void checkConsistencyWithBlocks(final SectionPos sectionPos, final LevelChunkSection blockSection) {
      Util.ifElse(this.getOrLoad(sectionPos.asLong()), section -> section.refresh(output -> {
         if (mayHavePoi(blockSection)) {
            this.updateFromSection(blockSection, sectionPos, output);
         }
      }), () -> {
         if (mayHavePoi(blockSection)) {
            PoiSection newSection = this.getOrCreate(sectionPos.asLong());
            this.updateFromSection(blockSection, sectionPos, newSection::add);
         }
      });
   }

   private static boolean mayHavePoi(final LevelChunkSection blockSection) {
      return blockSection.maybeHas(PoiTypes::hasPoi);
   }

   private void updateFromSection(final LevelChunkSection blockSection, final SectionPos pos, final BiConsumer<BlockPos, Holder<PoiType>> output) {
      pos.blocksInside()
         .forEach(
            blockPos -> {
               BlockState state = blockSection.getBlockState(
                  SectionPos.sectionRelative(blockPos.getX()), SectionPos.sectionRelative(blockPos.getY()), SectionPos.sectionRelative(blockPos.getZ())
               );
               PoiTypes.forState(state).ifPresent(type -> output.accept(blockPos, (Holder<PoiType>)type));
            }
         );
   }

   public void ensureLoadedAndValid(final LevelReader reader, final BlockPos center, final int radius) {
      SectionPos.aroundChunk(
            ChunkPos.containing(center), Math.floorDiv(radius, 16), this.levelHeightAccessor.getMinSectionY(), this.levelHeightAccessor.getMaxSectionY()
         )
         .map(pos -> Pair.of(pos, this.getOrLoad(pos.asLong())))
         .filter(poiSection -> !((Optional)poiSection.getSecond()).map(PoiSection::isValid).orElse(false))
         .map(p -> ((SectionPos)p.getFirst()).chunk())
         .filter(pos -> this.loadedChunks.add(pos.pack()))
         .forEach(pos -> reader.getChunk(pos.x(), pos.z(), ChunkStatus.EMPTY));
   }

   private final class DistanceTracker extends SectionTracker {
      private final Long2ByteMap levels = new Long2ByteOpenHashMap();

      DistanceTracker() {
         super(7, 16, 256);
         this.levels.defaultReturnValue((byte)7);
      }

      @Override
      protected int getLevelFromSource(final long to) {
         return PoiManager.this.isVillageCenter(to) ? 0 : 7;
      }

      @Override
      protected int getLevel(final long node) {
         return this.levels.get(node);
      }

      @Override
      protected void setLevel(final long node, final int level) {
         if (level > 6) {
            this.levels.remove(node);
         } else {
            this.levels.put(node, (byte)level);
         }
      }

      public void runAllUpdates() {
         super.runUpdates(Integer.MAX_VALUE);
      }
   }

   public enum Occupancy {
      HAS_SPACE(PoiRecord::hasSpace),
      IS_OCCUPIED(PoiRecord::isOccupied),
      ANY(poiRecord -> true);

      private final Predicate<? super PoiRecord> test;

      Occupancy(final Predicate<? super PoiRecord> test) {
         this.test = test;
      }

      public Predicate<? super PoiRecord> getTest() {
         return this.test;
      }
   }
}
