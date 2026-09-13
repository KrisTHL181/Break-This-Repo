package net.minecraft.world.level.levelgen;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jspecify.annotations.Nullable;

public class SurfaceRules {
   public static final SurfaceRules.ConditionSource ON_FLOOR = stoneDepthCheck(0, false, CaveSurface.FLOOR);
   public static final SurfaceRules.ConditionSource UNDER_FLOOR = stoneDepthCheck(0, true, CaveSurface.FLOOR);
   public static final SurfaceRules.ConditionSource DEEP_UNDER_FLOOR = stoneDepthCheck(0, true, 6, CaveSurface.FLOOR);
   public static final SurfaceRules.ConditionSource VERY_DEEP_UNDER_FLOOR = stoneDepthCheck(0, true, 30, CaveSurface.FLOOR);
   public static final SurfaceRules.ConditionSource ON_CEILING = stoneDepthCheck(0, false, CaveSurface.CEILING);
   public static final SurfaceRules.ConditionSource UNDER_CEILING = stoneDepthCheck(0, true, CaveSurface.CEILING);

   public static SurfaceRules.ConditionSource stoneDepthCheck(int p_202177_, boolean p_202178_, CaveSurface p_202179_) {
      return new SurfaceRules.StoneDepthCheck(p_202177_, p_202178_, 0, p_202179_);
   }

   public static SurfaceRules.ConditionSource stoneDepthCheck(int p_202172_, boolean p_202173_, int p_202174_, CaveSurface p_202175_) {
      return new SurfaceRules.StoneDepthCheck(p_202172_, p_202173_, p_202174_, p_202175_);
   }

   public static SurfaceRules.ConditionSource not(SurfaceRules.ConditionSource p_189393_) {
      return new SurfaceRules.NotConditionSource(p_189393_);
   }

   public static SurfaceRules.ConditionSource yBlockCheck(VerticalAnchor p_189401_, int p_189402_) {
      return new SurfaceRules.YConditionSource(p_189401_, p_189402_, false);
   }

   public static SurfaceRules.ConditionSource yStartCheck(VerticalAnchor p_189423_, int p_189424_) {
      return new SurfaceRules.YConditionSource(p_189423_, p_189424_, true);
   }

   public static SurfaceRules.ConditionSource waterBlockCheck(int p_189383_, int p_189384_) {
      return new SurfaceRules.WaterConditionSource(p_189383_, p_189384_, false);
   }

   public static SurfaceRules.ConditionSource waterStartCheck(int p_189420_, int p_189421_) {
      return new SurfaceRules.WaterConditionSource(p_189420_, p_189421_, true);
   }

   @SafeVarargs
   public static SurfaceRules.ConditionSource isBiome(ResourceKey<Biome>... p_189417_) {
      return isBiome(List.of(p_189417_));
   }

   private static SurfaceRules.BiomeConditionSource isBiome(List<ResourceKey<Biome>> p_189408_) {
      return new SurfaceRules.BiomeConditionSource(p_189408_);
   }

   public static SurfaceRules.ConditionSource noiseCondition(ResourceKey<NormalNoise.NoiseParameters> p_189410_, double p_189411_) {
      return noiseCondition(p_189410_, p_189411_, Double.MAX_VALUE);
   }

   public static SurfaceRules.ConditionSource noiseCondition(ResourceKey<NormalNoise.NoiseParameters> p_189413_, double p_189414_, double p_189415_) {
      return new SurfaceRules.NoiseThresholdConditionSource(p_189413_, p_189414_, p_189415_);
   }

   public static SurfaceRules.ConditionSource verticalGradient(String p_189404_, VerticalAnchor p_189405_, VerticalAnchor p_189406_) {
      return new SurfaceRules.VerticalGradientConditionSource(Identifier.parse(p_189404_), p_189405_, p_189406_);
   }

   public static SurfaceRules.ConditionSource steep() {
      return SurfaceRules.Steep.INSTANCE;
   }

   public static SurfaceRules.ConditionSource hole() {
      return SurfaceRules.Hole.INSTANCE;
   }

   public static SurfaceRules.ConditionSource abovePreliminarySurface() {
      return SurfaceRules.AbovePreliminarySurface.INSTANCE;
   }

   public static SurfaceRules.ConditionSource temperature() {
      return SurfaceRules.Temperature.INSTANCE;
   }

   public static SurfaceRules.RuleSource ifTrue(SurfaceRules.ConditionSource p_189395_, SurfaceRules.RuleSource p_189396_) {
      return new SurfaceRules.TestRuleSource(p_189395_, p_189396_);
   }

   public static SurfaceRules.RuleSource sequence(SurfaceRules.RuleSource... p_198273_) {
      if (p_198273_.length == 0) {
         throw new IllegalArgumentException("Need at least 1 rule for a sequence");
      } else {
         return new SurfaceRules.SequenceRuleSource(Arrays.asList(p_198273_));
      }
   }

   public static SurfaceRules.RuleSource state(BlockState p_189391_) {
      return new SurfaceRules.BlockRuleSource(p_189391_);
   }

   public static SurfaceRules.RuleSource bandlands() {
      return SurfaceRules.Bandlands.INSTANCE;
   }

   static <A> MapCodec<? extends A> register(Registry<MapCodec<? extends A>> p_224604_, String p_224605_, KeyDispatchDataCodec<? extends A> p_224606_) {
      return Registry.register(p_224604_, p_224605_, p_224606_.codec());
   }

   enum AbovePreliminarySurface implements SurfaceRules.ConditionSource {
      INSTANCE;

      static final KeyDispatchDataCodec<SurfaceRules.AbovePreliminarySurface> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(SurfaceRules.Context p_189437_) {
         return p_189437_.abovePreliminarySurface;
      }
   }

   enum Bandlands implements SurfaceRules.RuleSource {
      INSTANCE;

      static final KeyDispatchDataCodec<SurfaceRules.Bandlands> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
         return CODEC;
      }

      public SurfaceRules.SurfaceRule apply(SurfaceRules.Context p_189482_) {
         return p_189482_.system::getBand;
      }
   }

   static final class BiomeConditionSource implements SurfaceRules.ConditionSource {
      static final KeyDispatchDataCodec<SurfaceRules.BiomeConditionSource> CODEC = KeyDispatchDataCodec.of(
         ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biome_is").xmap(SurfaceRules::isBiome, p_204620_ -> p_204620_.biomes)
      );
      private final List<ResourceKey<Biome>> biomes;
      final Predicate<ResourceKey<Biome>> biomeNameTest;

      BiomeConditionSource(List<ResourceKey<Biome>> p_189493_) {
         this.biomes = p_189493_;
         this.biomeNameTest = Set.copyOf(p_189493_)::contains;
      }

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(final SurfaceRules.Context p_189496_) {
         class BiomeCondition extends SurfaceRules.LazyYCondition {
            BiomeCondition() {
               super(p_189496_);
            }

            @Override
            protected boolean compute() {
               return this.context.biome.get().is(BiomeConditionSource.this.biomeNameTest);
            }
         }

         return new BiomeCondition();
      }

      @Override
      public boolean equals(Object p_209694_) {
         if (this == p_209694_) {
            return true;
         } else {
            return p_209694_ instanceof SurfaceRules.BiomeConditionSource surfacerules$biomeconditionsource
               ? this.biomes.equals(surfacerules$biomeconditionsource.biomes)
               : false;
         }
      }

      @Override
      public int hashCode() {
         return this.biomes.hashCode();
      }

      @Override
      public String toString() {
         return "BiomeConditionSource[biomes=" + this.biomes + "]";
      }
   }

   record BlockRuleSource(BlockState resultState, SurfaceRules.StateRule rule) implements SurfaceRules.RuleSource {
      static final KeyDispatchDataCodec<SurfaceRules.BlockRuleSource> CODEC = KeyDispatchDataCodec.of(
         BlockState.CODEC.xmap(SurfaceRules.BlockRuleSource::new, SurfaceRules.BlockRuleSource::resultState).fieldOf("result_state")
      );

      BlockRuleSource(BlockState p_189517_) {
         this(p_189517_, new SurfaceRules.StateRule(p_189517_));
      }

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
         return CODEC;
      }

      public SurfaceRules.SurfaceRule apply(SurfaceRules.Context p_189523_) {
         return this.rule;
      }
   }

   interface Condition {
      boolean test();
   }

   public interface ConditionSource extends Function<SurfaceRules.Context, SurfaceRules.Condition> {
      Codec<SurfaceRules.ConditionSource> CODEC = BuiltInRegistries.MATERIAL_CONDITION
         .byNameCodec()
         .dispatch(p_327454_ -> p_327454_.codec().codec(), Function.identity());

      static MapCodec<? extends SurfaceRules.ConditionSource> bootstrap(Registry<MapCodec<? extends SurfaceRules.ConditionSource>> p_204625_) {
         SurfaceRules.register(p_204625_, "biome", SurfaceRules.BiomeConditionSource.CODEC);
         SurfaceRules.register(p_204625_, "noise_threshold", SurfaceRules.NoiseThresholdConditionSource.CODEC);
         SurfaceRules.register(p_204625_, "vertical_gradient", SurfaceRules.VerticalGradientConditionSource.CODEC);
         SurfaceRules.register(p_204625_, "y_above", SurfaceRules.YConditionSource.CODEC);
         SurfaceRules.register(p_204625_, "water", SurfaceRules.WaterConditionSource.CODEC);
         SurfaceRules.register(p_204625_, "temperature", SurfaceRules.Temperature.CODEC);
         SurfaceRules.register(p_204625_, "steep", SurfaceRules.Steep.CODEC);
         SurfaceRules.register(p_204625_, "not", SurfaceRules.NotConditionSource.CODEC);
         SurfaceRules.register(p_204625_, "hole", SurfaceRules.Hole.CODEC);
         SurfaceRules.register(p_204625_, "above_preliminary_surface", SurfaceRules.AbovePreliminarySurface.CODEC);
         return SurfaceRules.register(p_204625_, "stone_depth", SurfaceRules.StoneDepthCheck.CODEC);
      }

      KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec();
   }

   protected static final class Context {
      private static final int HOW_FAR_BELOW_PRELIMINARY_SURFACE_LEVEL_TO_BUILD_SURFACE = 8;
      private static final int SURFACE_CELL_BITS = 4;
      private static final int SURFACE_CELL_SIZE = 16;
      private static final int SURFACE_CELL_MASK = 15;
      final SurfaceSystem system;
      final SurfaceRules.Condition temperature = new SurfaceRules.Context.TemperatureHelperCondition(this);
      final SurfaceRules.Condition steep = new SurfaceRules.Context.SteepMaterialCondition(this);
      final SurfaceRules.Condition hole = new SurfaceRules.Context.HoleCondition(this);
      final SurfaceRules.Condition abovePreliminarySurface = new SurfaceRules.Context.AbovePreliminarySurfaceCondition();
      final RandomState randomState;
      final ChunkAccess chunk;
      private final NoiseChunk noiseChunk;
      private final Function<BlockPos, Holder<Biome>> biomeGetter;
      final WorldGenerationContext context;
      private long lastPreliminarySurfaceCellOrigin = Long.MAX_VALUE;
      private final int[] preliminarySurfaceCache = new int[4];
      long lastUpdateXZ = -9223372036854775807L;
      int blockX;
      int blockZ;
      int surfaceDepth;
      private long lastSurfaceDepth2Update = this.lastUpdateXZ - 1L;
      private double surfaceSecondary;
      private long lastMinSurfaceLevelUpdate = this.lastUpdateXZ - 1L;
      private int minSurfaceLevel;
      long lastUpdateY = -9223372036854775807L;
      final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      Supplier<Holder<Biome>> biome;
      int blockY;
      int waterHeight;
      int stoneDepthBelow;
      int stoneDepthAbove;

      protected Context(
         SurfaceSystem p_224616_,
         RandomState p_224617_,
         ChunkAccess p_224618_,
         NoiseChunk p_224619_,
         Function<BlockPos, Holder<Biome>> p_224620_,
         Registry<Biome> p_224621_,
         WorldGenerationContext p_224622_
      ) {
         this.system = p_224616_;
         this.randomState = p_224617_;
         this.chunk = p_224618_;
         this.noiseChunk = p_224619_;
         this.biomeGetter = p_224620_;
         this.context = p_224622_;
      }

      protected void updateXZ(int p_189570_, int p_189571_) {
         this.lastUpdateXZ++;
         this.lastUpdateY++;
         this.blockX = p_189570_;
         this.blockZ = p_189571_;
         this.surfaceDepth = this.system.getSurfaceDepth(p_189570_, p_189571_);
      }

      protected void updateY(int p_189577_, int p_189578_, int p_189579_, int p_189580_, int p_189581_, int p_189582_) {
         this.lastUpdateY++;
         this.biome = Suppliers.memoize(() -> this.biomeGetter.apply(this.pos.set(p_189580_, p_189581_, p_189582_)));
         this.blockY = p_189581_;
         this.waterHeight = p_189579_;
         this.stoneDepthBelow = p_189578_;
         this.stoneDepthAbove = p_189577_;
      }

      protected double getSurfaceSecondary() {
         if (this.lastSurfaceDepth2Update != this.lastUpdateXZ) {
            this.lastSurfaceDepth2Update = this.lastUpdateXZ;
            this.surfaceSecondary = this.system.getSurfaceSecondary(this.blockX, this.blockZ);
         }

         return this.surfaceSecondary;
      }

      public int getSeaLevel() {
         return this.system.getSeaLevel();
      }

      private static int blockCoordToSurfaceCell(int p_198281_) {
         return p_198281_ >> 4;
      }

      private static int surfaceCellToBlockCoord(int p_198283_) {
         return p_198283_ << 4;
      }

      protected int getMinSurfaceLevel() {
         if (this.lastMinSurfaceLevelUpdate != this.lastUpdateXZ) {
            this.lastMinSurfaceLevelUpdate = this.lastUpdateXZ;
            int i = blockCoordToSurfaceCell(this.blockX);
            int j = blockCoordToSurfaceCell(this.blockZ);
            long k = ChunkPos.asLong(i, j);
            if (this.lastPreliminarySurfaceCellOrigin != k) {
               this.lastPreliminarySurfaceCellOrigin = k;
               this.preliminarySurfaceCache[0] = this.noiseChunk.preliminarySurfaceLevel(surfaceCellToBlockCoord(i), surfaceCellToBlockCoord(j));
               this.preliminarySurfaceCache[1] = this.noiseChunk.preliminarySurfaceLevel(surfaceCellToBlockCoord(i + 1), surfaceCellToBlockCoord(j));
               this.preliminarySurfaceCache[2] = this.noiseChunk.preliminarySurfaceLevel(surfaceCellToBlockCoord(i), surfaceCellToBlockCoord(j + 1));
               this.preliminarySurfaceCache[3] = this.noiseChunk.preliminarySurfaceLevel(surfaceCellToBlockCoord(i + 1), surfaceCellToBlockCoord(j + 1));
            }

            int l = Mth.floor(
               Mth.lerp2(
                  (this.blockX & 15) / 16.0F,
                  (this.blockZ & 15) / 16.0F,
                  this.preliminarySurfaceCache[0],
                  this.preliminarySurfaceCache[1],
                  this.preliminarySurfaceCache[2],
                  this.preliminarySurfaceCache[3]
               )
            );
            this.minSurfaceLevel = l + this.surfaceDepth - 8;
         }

         return this.minSurfaceLevel;
      }

      final class AbovePreliminarySurfaceCondition implements SurfaceRules.Condition {
         @Override
         public boolean test() {
            return Context.this.blockY >= Context.this.getMinSurfaceLevel();
         }
      }

      static final class HoleCondition extends SurfaceRules.LazyXZCondition {
         HoleCondition(SurfaceRules.Context p_189591_) {
            super(p_189591_);
         }

         @Override
         protected boolean compute() {
            return this.context.surfaceDepth <= 0;
         }
      }

      static class SteepMaterialCondition extends SurfaceRules.LazyXZCondition {
         SteepMaterialCondition(SurfaceRules.Context p_189594_) {
            super(p_189594_);
         }

         @Override
         protected boolean compute() {
            int i = this.context.blockX & 15;
            int j = this.context.blockZ & 15;
            int k = Math.max(j - 1, 0);
            int l = Math.min(j + 1, 15);
            ChunkAccess chunkaccess = this.context.chunk;
            int i1 = chunkaccess.getHeight(Heightmap.Types.WORLD_SURFACE_WG, i, k);
            int j1 = chunkaccess.getHeight(Heightmap.Types.WORLD_SURFACE_WG, i, l);
            if (j1 >= i1 + 4) {
               return true;
            }

            int k1 = Math.max(i - 1, 0);
            int l1 = Math.min(i + 1, 15);
            int i2 = chunkaccess.getHeight(Heightmap.Types.WORLD_SURFACE_WG, k1, j);
            int j2 = chunkaccess.getHeight(Heightmap.Types.WORLD_SURFACE_WG, l1, j);
            return i2 >= j2 + 4;
         }
      }

      static class TemperatureHelperCondition extends SurfaceRules.LazyYCondition {
         TemperatureHelperCondition(SurfaceRules.Context p_189597_) {
            super(p_189597_);
         }

         @Override
         protected boolean compute() {
            return this.context
               .biome
               .get()
               .value()
               .coldEnoughToSnow(this.context.pos.set(this.context.blockX, this.context.blockY, this.context.blockZ), this.context.getSeaLevel());
         }
      }
   }

   enum Hole implements SurfaceRules.ConditionSource {
      INSTANCE;

      static final KeyDispatchDataCodec<SurfaceRules.Hole> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(SurfaceRules.Context p_189608_) {
         return p_189608_.hole;
      }
   }

   abstract static class LazyCondition implements SurfaceRules.Condition {
      protected final SurfaceRules.Context context;
      private long lastUpdate;
      @Nullable Boolean result;

      protected LazyCondition(SurfaceRules.Context p_189619_) {
         this.context = p_189619_;
         this.lastUpdate = this.getContextLastUpdate() - 1L;
      }

      @Override
      public boolean test() {
         long i = this.getContextLastUpdate();
         if (i == this.lastUpdate) {
            if (this.result == null) {
               throw new IllegalStateException("Update triggered but the result is null");
            } else {
               return this.result;
            }
         } else {
            this.lastUpdate = i;
            this.result = this.compute();
            return this.result;
         }
      }

      protected abstract long getContextLastUpdate();

      protected abstract boolean compute();
   }

   abstract static class LazyXZCondition extends SurfaceRules.LazyCondition {
      protected LazyXZCondition(SurfaceRules.Context p_189622_) {
         super(p_189622_);
      }

      @Override
      protected long getContextLastUpdate() {
         return this.context.lastUpdateXZ;
      }
   }

   abstract static class LazyYCondition extends SurfaceRules.LazyCondition {
      protected LazyYCondition(SurfaceRules.Context p_189625_) {
         super(p_189625_);
      }

      @Override
      protected long getContextLastUpdate() {
         return this.context.lastUpdateY;
      }
   }

   record NoiseThresholdConditionSource(ResourceKey<NormalNoise.NoiseParameters> noise, double minThreshold, double maxThreshold)
      implements SurfaceRules.ConditionSource {
      static final KeyDispatchDataCodec<SurfaceRules.NoiseThresholdConditionSource> CODEC = KeyDispatchDataCodec.of(
         RecordCodecBuilder.mapCodec(
            p_258995_ -> p_258995_.group(
                  ResourceKey.codec(Registries.NOISE).fieldOf("noise").forGetter(SurfaceRules.NoiseThresholdConditionSource::noise),
                  Codec.DOUBLE.fieldOf("min_threshold").forGetter(SurfaceRules.NoiseThresholdConditionSource::minThreshold),
                  Codec.DOUBLE.fieldOf("max_threshold").forGetter(SurfaceRules.NoiseThresholdConditionSource::maxThreshold)
               )
               .apply(p_258995_, SurfaceRules.NoiseThresholdConditionSource::new)
         )
      );

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(final SurfaceRules.Context p_189640_) {
         final NormalNoise normalnoise = p_189640_.randomState.getOrCreateNoise(this.noise);

         class NoiseThresholdCondition extends SurfaceRules.LazyXZCondition {
            NoiseThresholdCondition() {
               super(p_189640_);
            }

            @Override
            protected boolean compute() {
               double d0 = normalnoise.getValue(this.context.blockX, 0.0, this.context.blockZ);
               return d0 >= NoiseThresholdConditionSource.this.minThreshold && d0 <= NoiseThresholdConditionSource.this.maxThreshold;
            }
         }

         return new NoiseThresholdCondition();
      }
   }

   record NotCondition(SurfaceRules.Condition target) implements SurfaceRules.Condition {
      @Override
      public boolean test() {
         return !this.target.test();
      }
   }

   record NotConditionSource(SurfaceRules.ConditionSource target) implements SurfaceRules.ConditionSource {
      static final KeyDispatchDataCodec<SurfaceRules.NotConditionSource> CODEC = KeyDispatchDataCodec.of(
         SurfaceRules.ConditionSource.CODEC.xmap(SurfaceRules.NotConditionSource::new, SurfaceRules.NotConditionSource::target).fieldOf("invert")
      );

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(SurfaceRules.Context p_189674_) {
         return new SurfaceRules.NotCondition(this.target.apply(p_189674_));
      }
   }

   public interface RuleSource extends Function<SurfaceRules.Context, SurfaceRules.SurfaceRule> {
      Codec<SurfaceRules.RuleSource> CODEC = BuiltInRegistries.MATERIAL_RULE
         .byNameCodec()
         .dispatch(p_327455_ -> p_327455_.codec().codec(), Function.identity());

      static MapCodec<? extends SurfaceRules.RuleSource> bootstrap(Registry<MapCodec<? extends SurfaceRules.RuleSource>> p_204631_) {
         SurfaceRules.register(p_204631_, "bandlands", SurfaceRules.Bandlands.CODEC);
         SurfaceRules.register(p_204631_, "block", SurfaceRules.BlockRuleSource.CODEC);
         SurfaceRules.register(p_204631_, "sequence", SurfaceRules.SequenceRuleSource.CODEC);
         return SurfaceRules.register(p_204631_, "condition", SurfaceRules.TestRuleSource.CODEC);
      }

      KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec();
   }

   record SequenceRule(List<SurfaceRules.SurfaceRule> rules) implements SurfaceRules.SurfaceRule {
      @Override
      public @Nullable BlockState tryApply(int p_189694_, int p_189695_, int p_189696_) {
         for (SurfaceRules.SurfaceRule surfacerules$surfacerule : this.rules) {
            BlockState blockstate = surfacerules$surfacerule.tryApply(p_189694_, p_189695_, p_189696_);
            if (blockstate != null) {
               return blockstate;
            }
         }

         return null;
      }
   }

   record SequenceRuleSource(List<SurfaceRules.RuleSource> sequence) implements SurfaceRules.RuleSource {
      static final KeyDispatchDataCodec<SurfaceRules.SequenceRuleSource> CODEC = KeyDispatchDataCodec.of(
         SurfaceRules.RuleSource.CODEC.listOf().xmap(SurfaceRules.SequenceRuleSource::new, SurfaceRules.SequenceRuleSource::sequence).fieldOf("sequence")
      );

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
         return CODEC;
      }

      public SurfaceRules.SurfaceRule apply(SurfaceRules.Context p_189704_) {
         if (this.sequence.size() == 1) {
            return this.sequence.get(0).apply(p_189704_);
         }

         Builder<SurfaceRules.SurfaceRule> builder = ImmutableList.builder();

         for (SurfaceRules.RuleSource surfacerules$rulesource : this.sequence) {
            builder.add(surfacerules$rulesource.apply(p_189704_));
         }

         return new SurfaceRules.SequenceRule(builder.build());
      }
   }

   record StateRule(BlockState state) implements SurfaceRules.SurfaceRule {
      @Override
      public BlockState tryApply(int p_189721_, int p_189722_, int p_189723_) {
         return this.state;
      }
   }

   enum Steep implements SurfaceRules.ConditionSource {
      INSTANCE;

      static final KeyDispatchDataCodec<SurfaceRules.Steep> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(SurfaceRules.Context p_189733_) {
         return p_189733_.steep;
      }
   }

   record StoneDepthCheck(int offset, boolean addSurfaceDepth, int secondaryDepthRange, CaveSurface surfaceType) implements SurfaceRules.ConditionSource {
      static final KeyDispatchDataCodec<SurfaceRules.StoneDepthCheck> CODEC = KeyDispatchDataCodec.of(
         RecordCodecBuilder.mapCodec(
            p_189753_ -> p_189753_.group(
                  Codec.INT.fieldOf("offset").forGetter(SurfaceRules.StoneDepthCheck::offset),
                  Codec.BOOL.fieldOf("add_surface_depth").forGetter(SurfaceRules.StoneDepthCheck::addSurfaceDepth),
                  Codec.INT.fieldOf("secondary_depth_range").forGetter(SurfaceRules.StoneDepthCheck::secondaryDepthRange),
                  CaveSurface.CODEC.fieldOf("surface_type").forGetter(SurfaceRules.StoneDepthCheck::surfaceType)
               )
               .apply(p_189753_, SurfaceRules.StoneDepthCheck::new)
         )
      );

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(final SurfaceRules.Context p_189755_) {
         final boolean flag = this.surfaceType == CaveSurface.CEILING;

         class StoneDepthCondition extends SurfaceRules.LazyYCondition {
            StoneDepthCondition() {
               super(p_189755_);
            }

            @Override
            protected boolean compute() {
               int i = flag ? this.context.stoneDepthBelow : this.context.stoneDepthAbove;
               int j = StoneDepthCheck.this.addSurfaceDepth ? this.context.surfaceDepth : 0;
               int k = StoneDepthCheck.this.secondaryDepthRange == 0
                  ? 0
                  : (int)Mth.map(this.context.getSurfaceSecondary(), -1.0, 1.0, 0.0, StoneDepthCheck.this.secondaryDepthRange);
               return i <= 1 + StoneDepthCheck.this.offset + j + k;
            }
         }

         return new StoneDepthCondition();
      }
   }

   protected interface SurfaceRule {
      @Nullable BlockState tryApply(int var1, int var2, int var3);
   }

   enum Temperature implements SurfaceRules.ConditionSource {
      INSTANCE;

      static final KeyDispatchDataCodec<SurfaceRules.Temperature> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(SurfaceRules.Context p_189786_) {
         return p_189786_.temperature;
      }
   }

   record TestRule(SurfaceRules.Condition condition, SurfaceRules.SurfaceRule followup) implements SurfaceRules.SurfaceRule {
      @Override
      public @Nullable BlockState tryApply(int p_189805_, int p_189806_, int p_189807_) {
         return !this.condition.test() ? null : this.followup.tryApply(p_189805_, p_189806_, p_189807_);
      }
   }

   record TestRuleSource(SurfaceRules.ConditionSource ifTrue, SurfaceRules.RuleSource thenRun) implements SurfaceRules.RuleSource {
      static final KeyDispatchDataCodec<SurfaceRules.TestRuleSource> CODEC = KeyDispatchDataCodec.of(
         RecordCodecBuilder.mapCodec(
            p_189817_ -> p_189817_.group(
                  SurfaceRules.ConditionSource.CODEC.fieldOf("if_true").forGetter(SurfaceRules.TestRuleSource::ifTrue),
                  SurfaceRules.RuleSource.CODEC.fieldOf("then_run").forGetter(SurfaceRules.TestRuleSource::thenRun)
               )
               .apply(p_189817_, SurfaceRules.TestRuleSource::new)
         )
      );

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
         return CODEC;
      }

      public SurfaceRules.SurfaceRule apply(SurfaceRules.Context p_189819_) {
         return new SurfaceRules.TestRule(this.ifTrue.apply(p_189819_), this.thenRun.apply(p_189819_));
      }
   }

   record VerticalGradientConditionSource(Identifier randomName, VerticalAnchor trueAtAndBelow, VerticalAnchor falseAtAndAbove)
      implements SurfaceRules.ConditionSource {
      static final KeyDispatchDataCodec<SurfaceRules.VerticalGradientConditionSource> CODEC = KeyDispatchDataCodec.of(
         RecordCodecBuilder.mapCodec(
            p_449975_ -> p_449975_.group(
                  Identifier.CODEC.fieldOf("random_name").forGetter(SurfaceRules.VerticalGradientConditionSource::randomName),
                  VerticalAnchor.CODEC.fieldOf("true_at_and_below").forGetter(SurfaceRules.VerticalGradientConditionSource::trueAtAndBelow),
                  VerticalAnchor.CODEC.fieldOf("false_at_and_above").forGetter(SurfaceRules.VerticalGradientConditionSource::falseAtAndAbove)
               )
               .apply(p_449975_, SurfaceRules.VerticalGradientConditionSource::new)
         )
      );

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(final SurfaceRules.Context p_189841_) {
         final int i = this.trueAtAndBelow().resolveY(p_189841_.context);
         final int j = this.falseAtAndAbove().resolveY(p_189841_.context);
         final PositionalRandomFactory positionalrandomfactory = p_189841_.randomState.getOrCreateRandomFactory(this.randomName());

         class VerticalGradientCondition extends SurfaceRules.LazyYCondition {
            VerticalGradientCondition() {
               super(p_189841_);
            }

            @Override
            protected boolean compute() {
               int k = this.context.blockY;
               if (k <= i) {
                  return true;
               }

               if (k >= j) {
                  return false;
               }

               double d0 = Mth.map(k, i, j, 1.0, 0.0);
               RandomSource randomsource = positionalrandomfactory.at(this.context.blockX, k, this.context.blockZ);
               return randomsource.nextFloat() < d0;
            }
         }

         return new VerticalGradientCondition();
      }
   }

   record WaterConditionSource(int offset, int surfaceDepthMultiplier, boolean addStoneDepth) implements SurfaceRules.ConditionSource {
      static final KeyDispatchDataCodec<SurfaceRules.WaterConditionSource> CODEC = KeyDispatchDataCodec.of(
         RecordCodecBuilder.mapCodec(
            p_189874_ -> p_189874_.group(
                  Codec.INT.fieldOf("offset").forGetter(SurfaceRules.WaterConditionSource::offset),
                  Codec.intRange(-20, 20).fieldOf("surface_depth_multiplier").forGetter(SurfaceRules.WaterConditionSource::surfaceDepthMultiplier),
                  Codec.BOOL.fieldOf("add_stone_depth").forGetter(SurfaceRules.WaterConditionSource::addStoneDepth)
               )
               .apply(p_189874_, SurfaceRules.WaterConditionSource::new)
         )
      );

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(final SurfaceRules.Context p_189876_) {
         class WaterCondition extends SurfaceRules.LazyYCondition {
            WaterCondition() {
               super(p_189876_);
            }

            @Override
            protected boolean compute() {
               return this.context.waterHeight == Integer.MIN_VALUE
                  || this.context.blockY + (WaterConditionSource.this.addStoneDepth ? this.context.stoneDepthAbove : 0)
                     >= this.context.waterHeight
                        + WaterConditionSource.this.offset
                        + this.context.surfaceDepth * WaterConditionSource.this.surfaceDepthMultiplier;
            }
         }

         return new WaterCondition();
      }
   }

   record YConditionSource(VerticalAnchor anchor, int surfaceDepthMultiplier, boolean addStoneDepth) implements SurfaceRules.ConditionSource {
      static final KeyDispatchDataCodec<SurfaceRules.YConditionSource> CODEC = KeyDispatchDataCodec.of(
         RecordCodecBuilder.mapCodec(
            p_189455_ -> p_189455_.group(
                  VerticalAnchor.CODEC.fieldOf("anchor").forGetter(SurfaceRules.YConditionSource::anchor),
                  Codec.intRange(-20, 20).fieldOf("surface_depth_multiplier").forGetter(SurfaceRules.YConditionSource::surfaceDepthMultiplier),
                  Codec.BOOL.fieldOf("add_stone_depth").forGetter(SurfaceRules.YConditionSource::addStoneDepth)
               )
               .apply(p_189455_, SurfaceRules.YConditionSource::new)
         )
      );

      @Override
      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(final SurfaceRules.Context p_189457_) {
         class YCondition extends SurfaceRules.LazyYCondition {
            YCondition() {
               super(p_189457_);
            }

            @Override
            protected boolean compute() {
               return this.context.blockY + (YConditionSource.this.addStoneDepth ? this.context.stoneDepthAbove : 0)
                  >= YConditionSource.this.anchor.resolveY(this.context.context) + this.context.surfaceDepth * YConditionSource.this.surfaceDepthMultiplier;
            }
         }

         return new YCondition();
      }
   }
}
