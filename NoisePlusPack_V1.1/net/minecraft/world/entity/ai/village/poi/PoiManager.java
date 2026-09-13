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
      RegionStorageInfo p_332478_,
      Path p_217869_,
      DataFixer p_217870_,
      boolean p_217871_,
      RegistryAccess p_217872_,
      ChunkIOErrorReporter p_343386_,
      LevelHeightAccessor p_217873_
   ) {
      super(
         new SimpleRegionStorage(p_332478_, p_217869_, p_217870_, p_217871_, DataFixTypes.POI_CHUNK),
         PoiSection.Packed.CODEC,
         PoiSection::pack,
         PoiSection.Packed::unpack,
         PoiSection::new,
         p_217872_,
         p_343386_,
         p_217873_
      );
      this.distanceTracker = new PoiManager.DistanceTracker();
   }

   public @Nullable PoiRecord add(BlockPos p_217920_, Holder<PoiType> p_217921_) {
      return this.getOrCreate(SectionPos.asLong(p_217920_)).add(p_217920_, p_217921_);
   }

   public void remove(BlockPos p_27080_) {
      this.getOrLoad(SectionPos.asLong(p_27080_)).ifPresent(p_148657_ -> p_148657_.remove(p_27080_));
   }

   public long getCountInRange(Predicate<Holder<PoiType>> p_27122_, BlockPos p_27123_, int p_27124_, PoiManager.Occupancy p_27125_) {
      return this.getInRange(p_27122_, p_27123_, p_27124_, p_27125_).count();
   }

   public boolean existsAtPosition(ResourceKey<PoiType> p_217875_, BlockPos p_217876_) {
      return this.exists(p_217876_, p_217879_ -> p_217879_.is(p_217875_));
   }

   public Stream<PoiRecord> getInSquare(Predicate<Holder<PoiType>> p_27167_, BlockPos p_27168_, int p_27169_, PoiManager.Occupancy p_27170_) {
      int i = Math.floorDiv(p_27169_, 16) + 1;
      return ChunkPos.rangeClosed(new ChunkPos(p_27168_), i).flatMap(p_217938_ -> this.getInChunk(p_27167_, p_217938_, p_27170_)).filter(p_217971_ -> {
         BlockPos blockpos = p_217971_.getPos();
         return Math.abs(blockpos.getX() - p_27168_.getX()) <= p_27169_ && Math.abs(blockpos.getZ() - p_27168_.getZ()) <= p_27169_;
      });
   }

   public Stream<PoiRecord> getInRange(Predicate<Holder<PoiType>> p_27182_, BlockPos p_27183_, int p_27184_, PoiManager.Occupancy p_27185_) {
      int i = p_27184_ * p_27184_;
      return this.getInSquare(p_27182_, p_27183_, p_27184_, p_27185_).filter(p_217906_ -> p_217906_.getPos().distSqr(p_27183_) <= i);
   }

   @VisibleForDebug
   public Stream<PoiRecord> getInChunk(Predicate<Holder<PoiType>> p_27118_, ChunkPos p_27119_, PoiManager.Occupancy p_27120_) {
      return IntStream.rangeClosed(this.levelHeightAccessor.getMinSectionY(), this.levelHeightAccessor.getMaxSectionY())
         .boxed()
         .map(p_217886_ -> this.getOrLoad(SectionPos.of(p_27119_, p_217886_).asLong()))
         .filter(Optional::isPresent)
         .flatMap(p_217942_ -> p_217942_.get().getRecords(p_27118_, p_27120_));
   }

   public Stream<BlockPos> findAll(
      Predicate<Holder<PoiType>> p_27139_, Predicate<BlockPos> p_27140_, BlockPos p_27141_, int p_27142_, PoiManager.Occupancy p_27143_
   ) {
      return this.getInRange(p_27139_, p_27141_, p_27142_, p_27143_).map(PoiRecord::getPos).filter(p_27140_);
   }

   public Stream<Pair<Holder<PoiType>, BlockPos>> findAllWithType(
      Predicate<Holder<PoiType>> p_217984_, Predicate<BlockPos> p_217985_, BlockPos p_217986_, int p_217987_, PoiManager.Occupancy p_217988_
   ) {
      return this.getInRange(p_217984_, p_217986_, p_217987_, p_217988_)
         .filter(p_217982_ -> p_217985_.test(p_217982_.getPos()))
         .map(p_217990_ -> Pair.of(p_217990_.getPoiType(), p_217990_.getPos()));
   }

   public Stream<Pair<Holder<PoiType>, BlockPos>> findAllClosestFirstWithType(
      Predicate<Holder<PoiType>> p_217995_, Predicate<BlockPos> p_217996_, BlockPos p_217997_, int p_217998_, PoiManager.Occupancy p_217999_
   ) {
      return this.findAllWithType(p_217995_, p_217996_, p_217997_, p_217998_, p_217999_)
         .sorted(Comparator.comparingDouble(p_217915_ -> ((BlockPos)p_217915_.getSecond()).distSqr(p_217997_)));
   }

   public Optional<BlockPos> find(
      Predicate<Holder<PoiType>> p_27187_, Predicate<BlockPos> p_27188_, BlockPos p_27189_, int p_27190_, PoiManager.Occupancy p_27191_
   ) {
      return this.findAll(p_27187_, p_27188_, p_27189_, p_27190_, p_27191_).findFirst();
   }

   public Optional<BlockPos> findClosest(Predicate<Holder<PoiType>> p_27193_, BlockPos p_27194_, int p_27195_, PoiManager.Occupancy p_27196_) {
      return this.getInRange(p_27193_, p_27194_, p_27195_, p_27196_)
         .map(PoiRecord::getPos)
         .min(Comparator.comparingDouble(p_217977_ -> p_217977_.distSqr(p_27194_)));
   }

   public Optional<Pair<Holder<PoiType>, BlockPos>> findClosestWithType(
      Predicate<Holder<PoiType>> p_218003_, BlockPos p_218004_, int p_218005_, PoiManager.Occupancy p_218006_
   ) {
      return this.getInRange(p_218003_, p_218004_, p_218005_, p_218006_)
         .min(Comparator.comparingDouble(p_217909_ -> p_217909_.getPos().distSqr(p_218004_)))
         .map(p_217959_ -> Pair.of(p_217959_.getPoiType(), p_217959_.getPos()));
   }

   public Optional<BlockPos> findClosest(
      Predicate<Holder<PoiType>> p_148659_, Predicate<BlockPos> p_148660_, BlockPos p_148661_, int p_148662_, PoiManager.Occupancy p_148663_
   ) {
      return this.getInRange(p_148659_, p_148661_, p_148662_, p_148663_)
         .map(PoiRecord::getPos)
         .filter(p_148660_)
         .min(Comparator.comparingDouble(p_217918_ -> p_217918_.distSqr(p_148661_)));
   }

   public Optional<BlockPos> take(Predicate<Holder<PoiType>> p_217947_, BiPredicate<Holder<PoiType>, BlockPos> p_217948_, BlockPos p_217949_, int p_217950_) {
      return this.getInRange(p_217947_, p_217949_, p_217950_, PoiManager.Occupancy.HAS_SPACE)
         .filter(p_217934_ -> p_217948_.test(p_217934_.getPoiType(), p_217934_.getPos()))
         .findFirst()
         .map(p_217881_ -> {
            p_217881_.acquireTicket();
            return p_217881_.getPos();
         });
   }

   public Optional<BlockPos> getRandom(
      Predicate<Holder<PoiType>> p_217952_,
      Predicate<BlockPos> p_217953_,
      PoiManager.Occupancy p_217954_,
      BlockPos p_217955_,
      int p_217956_,
      RandomSource p_217957_
   ) {
      List<PoiRecord> list = Util.toShuffledList(this.getInRange(p_217952_, p_217955_, p_217956_, p_217954_), p_217957_);
      return list.stream().filter(p_217945_ -> p_217953_.test(p_217945_.getPos())).findFirst().map(PoiRecord::getPos);
   }

   public boolean release(BlockPos p_27155_) {
      return this.getOrLoad(SectionPos.asLong(p_27155_))
         .map(p_217993_ -> p_217993_.release(p_27155_))
         .orElseThrow(() -> Util.pauseInIde(new IllegalStateException("POI never registered at " + p_27155_)));
   }

   public boolean exists(BlockPos p_27092_, Predicate<Holder<PoiType>> p_27093_) {
      return this.getOrLoad(SectionPos.asLong(p_27092_)).map(p_217925_ -> p_217925_.exists(p_27092_, p_27093_)).orElse(false);
   }

   public Optional<Holder<PoiType>> getType(BlockPos p_27178_) {
      return this.getOrLoad(SectionPos.asLong(p_27178_)).flatMap(p_217974_ -> p_217974_.getType(p_27178_));
   }

   @VisibleForDebug
   public @Nullable DebugPoiInfo getDebugPoiInfo(BlockPos p_429522_) {
      return this.getOrLoad(SectionPos.asLong(p_429522_)).flatMap(p_421796_ -> p_421796_.getDebugPoiInfo(p_429522_)).orElse(null);
   }

   public int sectionsToVillage(SectionPos p_27099_) {
      this.distanceTracker.runAllUpdates();
      return this.distanceTracker.getLevel(p_27099_.asLong());
   }

   boolean isVillageCenter(long p_27198_) {
      Optional<PoiSection> optional = this.get(p_27198_);
      return optional == null
         ? false
         : optional.<Boolean>map(
               p_217883_ -> p_217883_.getRecords(p_217927_ -> p_217927_.is(PoiTypeTags.VILLAGE), PoiManager.Occupancy.IS_OCCUPIED).findAny().isPresent()
            )
            .orElse(false);
   }

   @Override
   public void tick(BooleanSupplier p_27105_) {
      super.tick(p_27105_);
      this.distanceTracker.runAllUpdates();
   }

   @Override
   protected void setDirty(long p_27036_) {
      super.setDirty(p_27036_);
      this.distanceTracker.update(p_27036_, this.distanceTracker.getLevelFromSource(p_27036_), false);
   }

   @Override
   protected void onSectionLoad(long p_27145_) {
      this.distanceTracker.update(p_27145_, this.distanceTracker.getLevelFromSource(p_27145_), false);
   }

   public void checkConsistencyWithBlocks(SectionPos p_281731_, LevelChunkSection p_281893_) {
      Util.ifElse(this.getOrLoad(p_281731_.asLong()), p_217898_ -> p_217898_.refresh(p_217967_ -> {
         if (mayHavePoi(p_281893_)) {
            this.updateFromSection(p_281893_, p_281731_, p_217967_);
         }
      }), () -> {
         if (mayHavePoi(p_281893_)) {
            PoiSection poisection = this.getOrCreate(p_281731_.asLong());
            this.updateFromSection(p_281893_, p_281731_, poisection::add);
         }
      });
   }

   private static boolean mayHavePoi(LevelChunkSection p_27061_) {
      return p_27061_.maybeHas(PoiTypes::hasPoi);
   }

   private void updateFromSection(LevelChunkSection p_27070_, SectionPos p_27071_, BiConsumer<BlockPos, Holder<PoiType>> p_27072_) {
      p_27071_.blocksInside()
         .forEach(
            p_217902_ -> {
               BlockState blockstate = p_27070_.getBlockState(
                  SectionPos.sectionRelative(p_217902_.getX()), SectionPos.sectionRelative(p_217902_.getY()), SectionPos.sectionRelative(p_217902_.getZ())
               );
               PoiTypes.forState(blockstate).ifPresent(p_217931_ -> p_27072_.accept(p_217902_, (Holder<PoiType>)p_217931_));
            }
         );
   }

   public void ensureLoadedAndValid(LevelReader p_27057_, BlockPos p_27058_, int p_27059_) {
      SectionPos.aroundChunk(
            new ChunkPos(p_27058_), Math.floorDiv(p_27059_, 16), this.levelHeightAccessor.getMinSectionY(), this.levelHeightAccessor.getMaxSectionY()
         )
         .map(p_217979_ -> Pair.of(p_217979_, this.getOrLoad(p_217979_.asLong())))
         .filter(p_217963_ -> !((Optional)p_217963_.getSecond()).map(PoiSection::isValid).orElse(false))
         .map(p_217891_ -> ((SectionPos)p_217891_.getFirst()).chunk())
         .filter(p_217961_ -> this.loadedChunks.add(p_217961_.toLong()))
         .forEach(p_326965_ -> p_27057_.getChunk(p_326965_.x, p_326965_.z, ChunkStatus.EMPTY));
   }

   final class DistanceTracker extends SectionTracker {
      private final Long2ByteMap levels = new Long2ByteOpenHashMap();

      protected DistanceTracker() {
         super(7, 16, 256);
         this.levels.defaultReturnValue((byte)7);
      }

      @Override
      protected int getLevelFromSource(long p_27208_) {
         return PoiManager.this.isVillageCenter(p_27208_) ? 0 : 7;
      }

      @Override
      protected int getLevel(long p_27210_) {
         return this.levels.get(p_27210_);
      }

      @Override
      protected void setLevel(long p_27205_, int p_27206_) {
         if (p_27206_ > 6) {
            this.levels.remove(p_27205_);
         } else {
            this.levels.put(p_27205_, (byte)p_27206_);
         }
      }

      public void runAllUpdates() {
         super.runUpdates(Integer.MAX_VALUE);
      }
   }

   public enum Occupancy {
      HAS_SPACE(PoiRecord::hasSpace),
      IS_OCCUPIED(PoiRecord::isOccupied),
      ANY(p_27223_ -> true);

      private final Predicate<? super PoiRecord> test;

      Occupancy(final Predicate<? super PoiRecord> p_27220_) {
         this.test = p_27220_;
      }

      public Predicate<? super PoiRecord> getTest() {
         return this.test;
      }
   }
}
