package net.minecraft.world.level.levelgen.blending;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction8;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.data.worldgen.NoiseData;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.FluidState;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class Blender {
   private static final Blender EMPTY = new Blender(new Long2ObjectOpenHashMap(), new Long2ObjectOpenHashMap()) {
      @Override
      public Blender.BlendingOutput blendOffsetAndFactor(int p_209724_, int p_209725_) {
         return new Blender.BlendingOutput(1.0, 0.0);
      }

      @Override
      public double blendDensity(DensityFunction.FunctionContext p_209727_, double p_209728_) {
         return p_209728_;
      }

      @Override
      public BiomeResolver getBiomeResolver(BiomeResolver p_190232_) {
         return p_190232_;
      }
   };
   private static final NormalNoise SHIFT_NOISE = NormalNoise.create(new XoroshiroRandomSource(42L), NoiseData.DEFAULT_SHIFT);
   private static final int HEIGHT_BLENDING_RANGE_CELLS = QuartPos.fromSection(7) - 1;
   private static final int HEIGHT_BLENDING_RANGE_CHUNKS = QuartPos.toSection(HEIGHT_BLENDING_RANGE_CELLS + 3);
   private static final int DENSITY_BLENDING_RANGE_CELLS = 2;
   private static final int DENSITY_BLENDING_RANGE_CHUNKS = QuartPos.toSection(5);
   private static final double OLD_CHUNK_XZ_RADIUS = 8.0;
   private final Long2ObjectOpenHashMap<BlendingData> heightAndBiomeBlendingData;
   private final Long2ObjectOpenHashMap<BlendingData> densityBlendingData;

   public static Blender empty() {
      return EMPTY;
   }

   public static Blender of(@Nullable WorldGenRegion p_190203_) {
      if (!SharedConstants.DEBUG_DISABLE_BLENDING && p_190203_ != null) {
         ChunkPos chunkpos = p_190203_.getCenter();
         if (!p_190203_.isOldChunkAround(chunkpos, HEIGHT_BLENDING_RANGE_CHUNKS)) {
            return EMPTY;
         }

         Long2ObjectOpenHashMap<BlendingData> long2objectopenhashmap = new Long2ObjectOpenHashMap();
         Long2ObjectOpenHashMap<BlendingData> long2objectopenhashmap1 = new Long2ObjectOpenHashMap();
         int i = Mth.square(HEIGHT_BLENDING_RANGE_CHUNKS + 1);

         for (int j = -HEIGHT_BLENDING_RANGE_CHUNKS; j <= HEIGHT_BLENDING_RANGE_CHUNKS; j++) {
            for (int k = -HEIGHT_BLENDING_RANGE_CHUNKS; k <= HEIGHT_BLENDING_RANGE_CHUNKS; k++) {
               if (j * j + k * k <= i) {
                  int l = chunkpos.x + j;
                  int i1 = chunkpos.z + k;
                  BlendingData blendingdata = BlendingData.getOrUpdateBlendingData(p_190203_, l, i1);
                  if (blendingdata != null) {
                     long2objectopenhashmap.put(ChunkPos.asLong(l, i1), blendingdata);
                     if (j >= -DENSITY_BLENDING_RANGE_CHUNKS
                        && j <= DENSITY_BLENDING_RANGE_CHUNKS
                        && k >= -DENSITY_BLENDING_RANGE_CHUNKS
                        && k <= DENSITY_BLENDING_RANGE_CHUNKS) {
                        long2objectopenhashmap1.put(ChunkPos.asLong(l, i1), blendingdata);
                     }
                  }
               }
            }
         }

         return long2objectopenhashmap.isEmpty() && long2objectopenhashmap1.isEmpty() ? EMPTY : new Blender(long2objectopenhashmap, long2objectopenhashmap1);
      } else {
         return EMPTY;
      }
   }

   Blender(Long2ObjectOpenHashMap<BlendingData> p_202197_, Long2ObjectOpenHashMap<BlendingData> p_202198_) {
      this.heightAndBiomeBlendingData = p_202197_;
      this.densityBlendingData = p_202198_;
   }

   public boolean isEmpty() {
      return this.heightAndBiomeBlendingData.isEmpty() && this.densityBlendingData.isEmpty();
   }

   public Blender.BlendingOutput blendOffsetAndFactor(int p_209719_, int p_209720_) {
      int i = QuartPos.fromBlock(p_209719_);
      int j = QuartPos.fromBlock(p_209720_);
      double d0 = this.getBlendingDataValue(i, 0, j, BlendingData::getHeight);
      if (d0 != Double.MAX_VALUE) {
         return new Blender.BlendingOutput(0.0, heightToOffset(d0));
      }

      MutableDouble mutabledouble = new MutableDouble(0.0);
      MutableDouble mutabledouble1 = new MutableDouble(0.0);
      MutableDouble mutabledouble2 = new MutableDouble(Double.POSITIVE_INFINITY);
      this.heightAndBiomeBlendingData
         .forEach(
            (p_449990_, p_449991_) -> p_449991_.iterateHeights(
               QuartPos.fromSection(ChunkPos.getX(p_449990_)), QuartPos.fromSection(ChunkPos.getZ(p_449990_)), (p_190199_, p_190200_, p_190201_) -> {
                  double d3 = Mth.length(i - p_190199_, j - p_190200_);
                  if (!(d3 > HEIGHT_BLENDING_RANGE_CELLS)) {
                     if (d3 < mutabledouble2.doubleValue()) {
                        mutabledouble2.setValue(d3);
                     }

                     double d4 = 1.0 / (d3 * d3 * d3 * d3);
                     mutabledouble1.add(p_190201_ * d4);
                     mutabledouble.add(d4);
                  }
               }
            )
         );
      if (mutabledouble2.doubleValue() == Double.POSITIVE_INFINITY) {
         return new Blender.BlendingOutput(1.0, 0.0);
      }

      double d1 = mutabledouble1.doubleValue() / mutabledouble.doubleValue();
      double d2 = Mth.clamp(mutabledouble2.doubleValue() / (HEIGHT_BLENDING_RANGE_CELLS + 1), 0.0, 1.0);
      d2 = 3.0 * d2 * d2 - 2.0 * d2 * d2 * d2;
      return new Blender.BlendingOutput(d2, heightToOffset(d1));
   }

   private static double heightToOffset(double p_190155_) {
      double d0 = 1.0;
      double d1 = p_190155_ + 0.5;
      double d2 = Mth.positiveModulo(d1, 8.0);
      return 1.0 * (32.0 * (d1 - 128.0) - 3.0 * (d1 - 120.0) * d2 + 3.0 * d2 * d2) / (128.0 * (32.0 - 3.0 * d2));
   }

   public double blendDensity(DensityFunction.FunctionContext p_209721_, double p_209722_) {
      int i = QuartPos.fromBlock(p_209721_.blockX());
      int j = p_209721_.blockY() / 8;
      int k = QuartPos.fromBlock(p_209721_.blockZ());
      double d0 = this.getBlendingDataValue(i, j, k, BlendingData::getDensity);
      if (d0 != Double.MAX_VALUE) {
         return d0;
      }

      MutableDouble mutabledouble = new MutableDouble(0.0);
      MutableDouble mutabledouble1 = new MutableDouble(0.0);
      MutableDouble mutabledouble2 = new MutableDouble(Double.POSITIVE_INFINITY);
      this.densityBlendingData
         .forEach(
            (p_449983_, p_449984_) -> p_449984_.iterateDensities(
               QuartPos.fromSection(ChunkPos.getX(p_449983_)),
               QuartPos.fromSection(ChunkPos.getZ(p_449983_)),
               j - 1,
               j + 1,
               (p_202230_, p_202231_, p_202232_, p_202233_) -> {
                  double d3 = Mth.length(i - p_202230_, (j - p_202231_) * 2, k - p_202232_);
                  if (!(d3 > 2.0)) {
                     if (d3 < mutabledouble2.doubleValue()) {
                        mutabledouble2.setValue(d3);
                     }

                     double d4 = 1.0 / (d3 * d3 * d3 * d3);
                     mutabledouble1.add(p_202233_ * d4);
                     mutabledouble.add(d4);
                  }
               }
            )
         );
      if (mutabledouble2.doubleValue() == Double.POSITIVE_INFINITY) {
         return p_209722_;
      }

      double d1 = mutabledouble1.doubleValue() / mutabledouble.doubleValue();
      double d2 = Mth.clamp(mutabledouble2.doubleValue() / 3.0, 0.0, 1.0);
      return Mth.lerp(d2, d1, p_209722_);
   }

   private double getBlendingDataValue(int p_190175_, int p_190176_, int p_190177_, Blender.CellValueGetter p_190178_) {
      int i = QuartPos.toSection(p_190175_);
      int j = QuartPos.toSection(p_190177_);
      boolean flag = (p_190175_ & 3) == 0;
      boolean flag1 = (p_190177_ & 3) == 0;
      double d0 = this.getBlendingDataValue(p_190178_, i, j, p_190175_, p_190176_, p_190177_);
      if (d0 == Double.MAX_VALUE) {
         if (flag && flag1) {
            d0 = this.getBlendingDataValue(p_190178_, i - 1, j - 1, p_190175_, p_190176_, p_190177_);
         }

         if (d0 == Double.MAX_VALUE) {
            if (flag) {
               d0 = this.getBlendingDataValue(p_190178_, i - 1, j, p_190175_, p_190176_, p_190177_);
            }

            if (d0 == Double.MAX_VALUE && flag1) {
               d0 = this.getBlendingDataValue(p_190178_, i, j - 1, p_190175_, p_190176_, p_190177_);
            }
         }
      }

      return d0;
   }

   private double getBlendingDataValue(Blender.CellValueGetter p_190212_, int p_190213_, int p_190214_, int p_190215_, int p_190216_, int p_190217_) {
      BlendingData blendingdata = (BlendingData)this.heightAndBiomeBlendingData.get(ChunkPos.asLong(p_190213_, p_190214_));
      return blendingdata != null
         ? p_190212_.get(blendingdata, p_190215_ - QuartPos.fromSection(p_190213_), p_190216_, p_190217_ - QuartPos.fromSection(p_190214_))
         : Double.MAX_VALUE;
   }

   public BiomeResolver getBiomeResolver(BiomeResolver p_190204_) {
      return (p_204669_, p_204670_, p_204671_, p_204672_) -> {
         Holder<Biome> holder = this.blendBiome(p_204669_, p_204670_, p_204671_);
         return holder == null ? p_190204_.getNoiseBiome(p_204669_, p_204670_, p_204671_, p_204672_) : holder;
      };
   }

   private Holder<Biome> blendBiome(int p_224707_, int p_224708_, int p_224709_) {
      MutableDouble mutabledouble = new MutableDouble(Double.POSITIVE_INFINITY);
      MutableObject<Holder<Biome>> mutableobject = new MutableObject();
      this.heightAndBiomeBlendingData
         .forEach(
            (p_449997_, p_449998_) -> p_449998_.iterateBiomes(
               QuartPos.fromSection(ChunkPos.getX(p_449997_)),
               p_224708_,
               QuartPos.fromSection(ChunkPos.getZ(p_449997_)),
               (p_360591_, p_360592_, p_360593_) -> {
                  double d2 = Mth.length(p_224707_ - p_360591_, p_224709_ - p_360592_);
                  if (!(d2 > HEIGHT_BLENDING_RANGE_CELLS) && d2 < mutabledouble.doubleValue()) {
                     mutableobject.setValue(p_360593_);
                     mutabledouble.setValue(d2);
                  }
               }
            )
         );
      if (mutabledouble.doubleValue() == Double.POSITIVE_INFINITY) {
         return null;
      }

      double d0 = SHIFT_NOISE.getValue(p_224707_, 0.0, p_224709_) * 12.0;
      double d1 = Mth.clamp((mutabledouble.doubleValue() + d0) / (HEIGHT_BLENDING_RANGE_CELLS + 1), 0.0, 1.0);
      return d1 > 0.5 ? null : (Holder)mutableobject.get();
   }

   public static void generateBorderTicks(WorldGenRegion p_197032_, ChunkAccess p_197033_) {
      if (!SharedConstants.DEBUG_DISABLE_BLENDING) {
         ChunkPos chunkpos = p_197033_.getPos();
         boolean flag = p_197033_.isOldNoiseGeneration();
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
         BlockPos blockpos = new BlockPos(chunkpos.getMinBlockX(), 0, chunkpos.getMinBlockZ());
         BlendingData blendingdata = p_197033_.getBlendingData();
         if (blendingdata != null) {
            int i = blendingdata.getAreaWithOldGeneration().getMinY();
            int j = blendingdata.getAreaWithOldGeneration().getMaxY();
            if (flag) {
               for (int k = 0; k < 16; k++) {
                  for (int l = 0; l < 16; l++) {
                     generateBorderTick(p_197033_, blockpos$mutableblockpos.setWithOffset(blockpos, k, i - 1, l));
                     generateBorderTick(p_197033_, blockpos$mutableblockpos.setWithOffset(blockpos, k, i, l));
                     generateBorderTick(p_197033_, blockpos$mutableblockpos.setWithOffset(blockpos, k, j, l));
                     generateBorderTick(p_197033_, blockpos$mutableblockpos.setWithOffset(blockpos, k, j + 1, l));
                  }
               }
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
               if (p_197032_.getChunk(chunkpos.x + direction.getStepX(), chunkpos.z + direction.getStepZ()).isOldNoiseGeneration() != flag) {
                  int i1 = direction == Direction.EAST ? 15 : 0;
                  int j1 = direction == Direction.WEST ? 0 : 15;
                  int k1 = direction == Direction.SOUTH ? 15 : 0;
                  int l1 = direction == Direction.NORTH ? 0 : 15;

                  for (int i2 = i1; i2 <= j1; i2++) {
                     for (int j2 = k1; j2 <= l1; j2++) {
                        int k2 = Math.min(j, p_197033_.getHeight(Heightmap.Types.MOTION_BLOCKING, i2, j2)) + 1;

                        for (int l2 = i; l2 < k2; l2++) {
                           generateBorderTick(p_197033_, blockpos$mutableblockpos.setWithOffset(blockpos, i2, l2, j2));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void generateBorderTick(ChunkAccess p_197041_, BlockPos p_197042_) {
      BlockState blockstate = p_197041_.getBlockState(p_197042_);
      if (blockstate.is(BlockTags.LEAVES)) {
         p_197041_.markPosForPostprocessing(p_197042_);
      }

      FluidState fluidstate = p_197041_.getFluidState(p_197042_);
      if (!fluidstate.isEmpty()) {
         p_197041_.markPosForPostprocessing(p_197042_);
      }
   }

   public static void addAroundOldChunksCarvingMaskFilter(WorldGenLevel p_197035_, ProtoChunk p_197036_) {
      if (!SharedConstants.DEBUG_DISABLE_BLENDING) {
         ChunkPos chunkpos = p_197036_.getPos();
         Builder<Direction8, BlendingData> builder = ImmutableMap.builder();

         for (Direction8 direction8 : Direction8.values()) {
            int i = chunkpos.x + direction8.getStepX();
            int j = chunkpos.z + direction8.getStepZ();
            BlendingData blendingdata = p_197035_.getChunk(i, j).getBlendingData();
            if (blendingdata != null) {
               builder.put(direction8, blendingdata);
            }
         }

         ImmutableMap<Direction8, BlendingData> immutablemap = builder.build();
         if (p_197036_.isOldNoiseGeneration() || !immutablemap.isEmpty()) {
            Blender.DistanceGetter blender$distancegetter = makeOldChunkDistanceGetter(p_197036_.getBlendingData(), immutablemap);
            CarvingMask.Mask carvingmask$mask = (p_202262_, p_202263_, p_202264_) -> {
               double d0 = p_202262_ + 0.5 + SHIFT_NOISE.getValue(p_202262_, p_202263_, p_202264_) * 4.0;
               double d1 = p_202263_ + 0.5 + SHIFT_NOISE.getValue(p_202263_, p_202264_, p_202262_) * 4.0;
               double d2 = p_202264_ + 0.5 + SHIFT_NOISE.getValue(p_202264_, p_202262_, p_202263_) * 4.0;
               return blender$distancegetter.getDistance(d0, d1, d2) < 4.0;
            };
            p_197036_.getOrCreateCarvingMask().setAdditionalMask(carvingmask$mask);
         }
      }
   }

   public static Blender.DistanceGetter makeOldChunkDistanceGetter(@Nullable BlendingData p_224727_, Map<Direction8, BlendingData> p_224728_) {
      List<Blender.DistanceGetter> list = Lists.newArrayList();
      if (p_224727_ != null) {
         list.add(makeOffsetOldChunkDistanceGetter(null, p_224727_));
      }

      p_224728_.forEach((p_224734_, p_224735_) -> list.add(makeOffsetOldChunkDistanceGetter(p_224734_, p_224735_)));
      return (p_202267_, p_202268_, p_202269_) -> {
         double d0 = Double.POSITIVE_INFINITY;

         for (Blender.DistanceGetter blender$distancegetter : list) {
            double d1 = blender$distancegetter.getDistance(p_202267_, p_202268_, p_202269_);
            if (d1 < d0) {
               d0 = d1;
            }
         }

         return d0;
      };
   }

   private static Blender.DistanceGetter makeOffsetOldChunkDistanceGetter(@Nullable Direction8 p_224730_, BlendingData p_224731_) {
      double d0 = 0.0;
      double d1 = 0.0;
      if (p_224730_ != null) {
         for (Direction direction : p_224730_.getDirections()) {
            d0 += direction.getStepX() * 16;
            d1 += direction.getStepZ() * 16;
         }
      }

      double d5 = d0;
      double d2 = d1;
      double d3 = p_224731_.getAreaWithOldGeneration().getHeight() / 2.0;
      double d4 = p_224731_.getAreaWithOldGeneration().getMinY() + d3;
      return (p_224703_, p_224704_, p_224705_) -> distanceToCube(p_224703_ - 8.0 - d5, p_224704_ - d4, p_224705_ - 8.0 - d2, 8.0, d3, 8.0);
   }

   private static double distanceToCube(double p_197025_, double p_197026_, double p_197027_, double p_197028_, double p_197029_, double p_197030_) {
      double d0 = Math.abs(p_197025_) - p_197028_;
      double d1 = Math.abs(p_197026_) - p_197029_;
      double d2 = Math.abs(p_197027_) - p_197030_;
      return Mth.length(Math.max(0.0, d0), Math.max(0.0, d1), Math.max(0.0, d2));
   }

   public record BlendingOutput(double alpha, double blendingOffset) {
   }

   interface CellValueGetter {
      double get(BlendingData var1, int var2, int var3, int var4);
   }

   public interface DistanceGetter {
      double getDistance(double var1, double var3, double var5);
   }
}
