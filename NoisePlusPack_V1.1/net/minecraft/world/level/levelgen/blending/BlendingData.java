package net.minecraft.world.level.levelgen.blending;

import com.google.common.primitives.Doubles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction8;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

public class BlendingData {
   private static final double BLENDING_DENSITY_FACTOR = 0.1;
   protected static final int CELL_WIDTH = 4;
   protected static final int CELL_HEIGHT = 8;
   protected static final int CELL_RATIO = 2;
   private static final double SOLID_DENSITY = 1.0;
   private static final double AIR_DENSITY = -1.0;
   private static final int CELLS_PER_SECTION_Y = 2;
   private static final int QUARTS_PER_SECTION = QuartPos.fromBlock(16);
   private static final int CELL_HORIZONTAL_MAX_INDEX_INSIDE = QUARTS_PER_SECTION - 1;
   private static final int CELL_HORIZONTAL_MAX_INDEX_OUTSIDE = QUARTS_PER_SECTION;
   private static final int CELL_COLUMN_INSIDE_COUNT = 2 * CELL_HORIZONTAL_MAX_INDEX_INSIDE + 1;
   private static final int CELL_COLUMN_OUTSIDE_COUNT = 2 * CELL_HORIZONTAL_MAX_INDEX_OUTSIDE + 1;
   static final int CELL_COLUMN_COUNT = CELL_COLUMN_INSIDE_COUNT + CELL_COLUMN_OUTSIDE_COUNT;
   private final LevelHeightAccessor areaWithOldGeneration;
   private static final List<Block> SURFACE_BLOCKS = List.of(
      Blocks.PODZOL,
      Blocks.GRAVEL,
      Blocks.GRASS_BLOCK,
      Blocks.STONE,
      Blocks.COARSE_DIRT,
      Blocks.SAND,
      Blocks.RED_SAND,
      Blocks.MYCELIUM,
      Blocks.SNOW_BLOCK,
      Blocks.TERRACOTTA,
      Blocks.DIRT
   );
   protected static final double NO_VALUE = Double.MAX_VALUE;
   private boolean hasCalculatedData;
   private final double[] heights;
   private final List<@Nullable List<@Nullable Holder<Biome>>> biomes;
   private final transient double[][] densities;

   private BlendingData(int p_224740_, int p_224741_, Optional<double[]> p_224742_) {
      this.heights = p_224742_.orElseGet(() -> Util.make(new double[CELL_COLUMN_COUNT], p_224756_ -> Arrays.fill(p_224756_, Double.MAX_VALUE)));
      this.densities = new double[CELL_COLUMN_COUNT][];
      ObjectArrayList<List<Holder<Biome>>> objectarraylist = new ObjectArrayList(CELL_COLUMN_COUNT);
      objectarraylist.size(CELL_COLUMN_COUNT);
      this.biomes = objectarraylist;
      int i = SectionPos.sectionToBlockCoord(p_224740_);
      int j = SectionPos.sectionToBlockCoord(p_224741_) - i;
      this.areaWithOldGeneration = LevelHeightAccessor.create(i, j);
   }

   public static @Nullable BlendingData unpack(BlendingData.@Nullable Packed p_364541_) {
      return p_364541_ == null ? null : new BlendingData(p_364541_.minSection(), p_364541_.maxSection(), p_364541_.heights());
   }

   public BlendingData.Packed pack() {
      boolean flag = false;

      for (double d0 : this.heights) {
         if (d0 != Double.MAX_VALUE) {
            flag = true;
            break;
         }
      }

      return new BlendingData.Packed(
         this.areaWithOldGeneration.getMinSectionY(),
         this.areaWithOldGeneration.getMaxSectionY() + 1,
         flag ? Optional.of(DoubleArrays.copy(this.heights)) : Optional.empty()
      );
   }

   public static @Nullable BlendingData getOrUpdateBlendingData(WorldGenRegion p_190305_, int p_190306_, int p_190307_) {
      ChunkAccess chunkaccess = p_190305_.getChunk(p_190306_, p_190307_);
      BlendingData blendingdata = chunkaccess.getBlendingData();
      if (blendingdata != null && !chunkaccess.getHighestGeneratedStatus().isBefore(ChunkStatus.BIOMES)) {
         blendingdata.calculateData(chunkaccess, sideByGenerationAge(p_190305_, p_190306_, p_190307_, false));
         return blendingdata;
      } else {
         return null;
      }
   }

   public static Set<Direction8> sideByGenerationAge(WorldGenLevel p_197066_, int p_197067_, int p_197068_, boolean p_197069_) {
      Set<Direction8> set = EnumSet.noneOf(Direction8.class);

      for (Direction8 direction8 : Direction8.values()) {
         int i = p_197067_ + direction8.getStepX();
         int j = p_197068_ + direction8.getStepZ();
         if (p_197066_.getChunk(i, j).isOldNoiseGeneration() == p_197069_) {
            set.add(direction8);
         }
      }

      return set;
   }

   private void calculateData(ChunkAccess p_190318_, Set<Direction8> p_190319_) {
      if (!this.hasCalculatedData) {
         if (p_190319_.contains(Direction8.NORTH) || p_190319_.contains(Direction8.WEST) || p_190319_.contains(Direction8.NORTH_WEST)) {
            this.addValuesForColumn(getInsideIndex(0, 0), p_190318_, 0, 0);
         }

         if (p_190319_.contains(Direction8.NORTH)) {
            for (int i = 1; i < QUARTS_PER_SECTION; i++) {
               this.addValuesForColumn(getInsideIndex(i, 0), p_190318_, 4 * i, 0);
            }
         }

         if (p_190319_.contains(Direction8.WEST)) {
            for (int j = 1; j < QUARTS_PER_SECTION; j++) {
               this.addValuesForColumn(getInsideIndex(0, j), p_190318_, 0, 4 * j);
            }
         }

         if (p_190319_.contains(Direction8.EAST)) {
            for (int k = 1; k < QUARTS_PER_SECTION; k++) {
               this.addValuesForColumn(getOutsideIndex(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE, k), p_190318_, 15, 4 * k);
            }
         }

         if (p_190319_.contains(Direction8.SOUTH)) {
            for (int l = 0; l < QUARTS_PER_SECTION; l++) {
               this.addValuesForColumn(getOutsideIndex(l, CELL_HORIZONTAL_MAX_INDEX_OUTSIDE), p_190318_, 4 * l, 15);
            }
         }

         if (p_190319_.contains(Direction8.EAST) && p_190319_.contains(Direction8.NORTH_EAST)) {
            this.addValuesForColumn(getOutsideIndex(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE, 0), p_190318_, 15, 0);
         }

         if (p_190319_.contains(Direction8.EAST) && p_190319_.contains(Direction8.SOUTH) && p_190319_.contains(Direction8.SOUTH_EAST)) {
            this.addValuesForColumn(getOutsideIndex(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE, CELL_HORIZONTAL_MAX_INDEX_OUTSIDE), p_190318_, 15, 15);
         }

         this.hasCalculatedData = true;
      }
   }

   private void addValuesForColumn(int p_190300_, ChunkAccess p_190301_, int p_190302_, int p_190303_) {
      if (this.heights[p_190300_] == Double.MAX_VALUE) {
         this.heights[p_190300_] = this.getHeightAtXZ(p_190301_, p_190302_, p_190303_);
      }

      this.densities[p_190300_] = this.getDensityColumn(p_190301_, p_190302_, p_190303_, Mth.floor(this.heights[p_190300_]));
      this.biomes.set(p_190300_, this.getBiomeColumn(p_190301_, p_190302_, p_190303_));
   }

   private int getHeightAtXZ(ChunkAccess p_190311_, int p_190312_, int p_190313_) {
      int i;
      if (p_190311_.hasPrimedHeightmap(Heightmap.Types.WORLD_SURFACE_WG)) {
         i = Math.min(p_190311_.getHeight(Heightmap.Types.WORLD_SURFACE_WG, p_190312_, p_190313_), this.areaWithOldGeneration.getMaxY());
      } else {
         i = this.areaWithOldGeneration.getMaxY();
      }

      int j = this.areaWithOldGeneration.getMinY();
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(p_190312_, i, p_190313_);

      while (blockpos$mutableblockpos.getY() > j) {
         if (SURFACE_BLOCKS.contains(p_190311_.getBlockState(blockpos$mutableblockpos).getBlock())) {
            return blockpos$mutableblockpos.getY();
         }

         blockpos$mutableblockpos.move(Direction.DOWN);
      }

      return j;
   }

   private static double read1(ChunkAccess p_198298_, BlockPos.MutableBlockPos p_198299_) {
      return isGround(p_198298_, p_198299_.move(Direction.DOWN)) ? 1.0 : -1.0;
   }

   private static double read7(ChunkAccess p_198301_, BlockPos.MutableBlockPos p_198302_) {
      double d0 = 0.0;

      for (int i = 0; i < 7; i++) {
         d0 += read1(p_198301_, p_198302_);
      }

      return d0;
   }

   private double[] getDensityColumn(ChunkAccess p_198293_, int p_198294_, int p_198295_, int p_198296_) {
      double[] adouble = new double[this.cellCountPerColumn()];
      Arrays.fill(adouble, -1.0);
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(p_198294_, this.areaWithOldGeneration.getMaxY() + 1, p_198295_);
      double d0 = read7(p_198293_, blockpos$mutableblockpos);

      for (int i = adouble.length - 2; i >= 0; i--) {
         double d1 = read1(p_198293_, blockpos$mutableblockpos);
         double d2 = read7(p_198293_, blockpos$mutableblockpos);
         adouble[i] = (d0 + d1 + d2) / 15.0;
         d0 = d2;
      }

      int j = this.getCellYIndex(Mth.floorDiv(p_198296_, 8));
      if (j >= 0 && j < adouble.length - 1) {
         double d4 = (p_198296_ + 0.5) % 8.0 / 8.0;
         double d5 = (1.0 - d4) / d4;
         double d3 = Math.max(d5, 1.0) * 0.25;
         adouble[j + 1] = -d5 / d3;
         adouble[j] = 1.0 / d3;
      }

      return adouble;
   }

   private List<Holder<Biome>> getBiomeColumn(ChunkAccess p_224758_, int p_224759_, int p_224760_) {
      ObjectArrayList<Holder<Biome>> objectarraylist = new ObjectArrayList(this.quartCountPerColumn());
      objectarraylist.size(this.quartCountPerColumn());

      for (int i = 0; i < objectarraylist.size(); i++) {
         int j = i + QuartPos.fromBlock(this.areaWithOldGeneration.getMinY());
         objectarraylist.set(i, p_224758_.getNoiseBiome(QuartPos.fromBlock(p_224759_), j, QuartPos.fromBlock(p_224760_)));
      }

      return objectarraylist;
   }

   private static boolean isGround(ChunkAccess p_190315_, BlockPos p_190316_) {
      BlockState blockstate = p_190315_.getBlockState(p_190316_);
      if (blockstate.isAir()) {
         return false;
      } else if (blockstate.is(BlockTags.LEAVES)) {
         return false;
      } else if (blockstate.is(BlockTags.LOGS)) {
         return false;
      } else {
         return !blockstate.is(Blocks.BROWN_MUSHROOM_BLOCK) && !blockstate.is(Blocks.RED_MUSHROOM_BLOCK)
            ? !blockstate.getCollisionShape(p_190315_, p_190316_).isEmpty()
            : false;
      }
   }

   protected double getHeight(int p_190286_, int p_190287_, int p_190288_) {
      if (p_190286_ != CELL_HORIZONTAL_MAX_INDEX_OUTSIDE && p_190288_ != CELL_HORIZONTAL_MAX_INDEX_OUTSIDE) {
         return p_190286_ != 0 && p_190288_ != 0 ? Double.MAX_VALUE : this.heights[getInsideIndex(p_190286_, p_190288_)];
      } else {
         return this.heights[getOutsideIndex(p_190286_, p_190288_)];
      }
   }

   private double getDensity(double @Nullable [] p_190325_, int p_190326_) {
      if (p_190325_ == null) {
         return Double.MAX_VALUE;
      }

      int i = this.getCellYIndex(p_190326_);
      return i >= 0 && i < p_190325_.length ? p_190325_[i] * 0.1 : Double.MAX_VALUE;
   }

   protected double getDensity(int p_190334_, int p_190335_, int p_190336_) {
      if (p_190335_ == this.getMinY()) {
         return 0.1;
      } else if (p_190334_ != CELL_HORIZONTAL_MAX_INDEX_OUTSIDE && p_190336_ != CELL_HORIZONTAL_MAX_INDEX_OUTSIDE) {
         return p_190334_ != 0 && p_190336_ != 0 ? Double.MAX_VALUE : this.getDensity(this.densities[getInsideIndex(p_190334_, p_190336_)], p_190335_);
      } else {
         return this.getDensity(this.densities[getOutsideIndex(p_190334_, p_190336_)], p_190335_);
      }
   }

   protected void iterateBiomes(int p_224749_, int p_224750_, int p_224751_, BlendingData.BiomeConsumer p_224752_) {
      if (p_224750_ >= QuartPos.fromBlock(this.areaWithOldGeneration.getMinY()) && p_224750_ <= QuartPos.fromBlock(this.areaWithOldGeneration.getMaxY())) {
         int i = p_224750_ - QuartPos.fromBlock(this.areaWithOldGeneration.getMinY());

         for (int j = 0; j < this.biomes.size(); j++) {
            List<Holder<Biome>> list = this.biomes.get(j);
            if (list != null) {
               Holder<Biome> holder = list.get(i);
               if (holder != null) {
                  p_224752_.consume(p_224749_ + getX(j), p_224751_ + getZ(j), holder);
               }
            }
         }
      }
   }

   protected void iterateHeights(int p_190296_, int p_190297_, BlendingData.HeightConsumer p_190298_) {
      for (int i = 0; i < this.heights.length; i++) {
         double d0 = this.heights[i];
         if (d0 != Double.MAX_VALUE) {
            p_190298_.consume(p_190296_ + getX(i), p_190297_ + getZ(i), d0);
         }
      }
   }

   protected void iterateDensities(int p_190290_, int p_190291_, int p_190292_, int p_190293_, BlendingData.DensityConsumer p_190294_) {
      int i = this.getColumnMinY();
      int j = Math.max(0, p_190292_ - i);
      int k = Math.min(this.cellCountPerColumn(), p_190293_ - i);

      for (int l = 0; l < this.densities.length; l++) {
         double[] adouble = this.densities[l];
         if (adouble != null) {
            int i1 = p_190290_ + getX(l);
            int j1 = p_190291_ + getZ(l);

            for (int k1 = j; k1 < k; k1++) {
               p_190294_.consume(i1, k1 + i, j1, adouble[k1] * 0.1);
            }
         }
      }
   }

   private int cellCountPerColumn() {
      return this.areaWithOldGeneration.getSectionsCount() * 2;
   }

   private int quartCountPerColumn() {
      return QuartPos.fromSection(this.areaWithOldGeneration.getSectionsCount());
   }

   private int getColumnMinY() {
      return this.getMinY() + 1;
   }

   private int getMinY() {
      return this.areaWithOldGeneration.getMinSectionY() * 2;
   }

   private int getCellYIndex(int p_224747_) {
      return p_224747_ - this.getColumnMinY();
   }

   private static int getInsideIndex(int p_190331_, int p_190332_) {
      return CELL_HORIZONTAL_MAX_INDEX_INSIDE - p_190331_ + p_190332_;
   }

   private static int getOutsideIndex(int p_190351_, int p_190352_) {
      return CELL_COLUMN_INSIDE_COUNT + p_190351_ + CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - p_190352_;
   }

   private static int getX(int p_190349_) {
      if (p_190349_ < CELL_COLUMN_INSIDE_COUNT) {
         return zeroIfNegative(CELL_HORIZONTAL_MAX_INDEX_INSIDE - p_190349_);
      }

      int i = p_190349_ - CELL_COLUMN_INSIDE_COUNT;
      return CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - zeroIfNegative(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - i);
   }

   private static int getZ(int p_190355_) {
      if (p_190355_ < CELL_COLUMN_INSIDE_COUNT) {
         return zeroIfNegative(p_190355_ - CELL_HORIZONTAL_MAX_INDEX_INSIDE);
      }

      int i = p_190355_ - CELL_COLUMN_INSIDE_COUNT;
      return CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - zeroIfNegative(i - CELL_HORIZONTAL_MAX_INDEX_OUTSIDE);
   }

   private static int zeroIfNegative(int p_190357_) {
      return p_190357_ & ~(p_190357_ >> 31);
   }

   public LevelHeightAccessor getAreaWithOldGeneration() {
      return this.areaWithOldGeneration;
   }

   protected interface BiomeConsumer {
      void consume(int var1, int var2, Holder<Biome> var3);
   }

   protected interface DensityConsumer {
      void consume(int var1, int var2, int var3, double var4);
   }

   protected interface HeightConsumer {
      void consume(int var1, int var2, double var3);
   }

   public record Packed(int minSection, int maxSection, Optional<double[]> heights) {
      private static final Codec<double[]> DOUBLE_ARRAY_CODEC = Codec.DOUBLE.listOf().xmap(Doubles::toArray, Doubles::asList);
      public static final Codec<BlendingData.Packed> CODEC = RecordCodecBuilder.create(
            p_363506_ -> p_363506_.group(
                  Codec.INT.fieldOf("min_section").forGetter(BlendingData.Packed::minSection),
                  Codec.INT.fieldOf("max_section").forGetter(BlendingData.Packed::maxSection),
                  DOUBLE_ARRAY_CODEC.lenientOptionalFieldOf("heights").forGetter(BlendingData.Packed::heights)
               )
               .apply(p_363506_, BlendingData.Packed::new)
         )
         .validate(BlendingData.Packed::validateArraySize);

      private static DataResult<BlendingData.Packed> validateArraySize(BlendingData.Packed p_368802_) {
         return p_368802_.heights.isPresent() && ((double[])p_368802_.heights.get()).length != BlendingData.CELL_COLUMN_COUNT
            ? DataResult.error(() -> "heights has to be of length " + BlendingData.CELL_COLUMN_COUNT)
            : DataResult.success(p_368802_);
      }
   }
}
