package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BlockColumn;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class SurfaceSystem {
   private static final BlockState WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.defaultBlockState();
   private static final BlockState ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
   private static final BlockState TERRACOTTA = Blocks.TERRACOTTA.defaultBlockState();
   private static final BlockState YELLOW_TERRACOTTA = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
   private static final BlockState BROWN_TERRACOTTA = Blocks.BROWN_TERRACOTTA.defaultBlockState();
   private static final BlockState RED_TERRACOTTA = Blocks.RED_TERRACOTTA.defaultBlockState();
   private static final BlockState LIGHT_GRAY_TERRACOTTA = Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState();
   private static final BlockState PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState();
   private static final BlockState SNOW_BLOCK = Blocks.SNOW_BLOCK.defaultBlockState();
   private final BlockState defaultBlock;
   private final int seaLevel;
   private final BlockState[] clayBands;
   private final NormalNoise clayBandsOffsetNoise;
   private final NormalNoise badlandsPillarNoise;
   private final NormalNoise badlandsPillarRoofNoise;
   private final NormalNoise badlandsSurfaceNoise;
   private final NormalNoise icebergPillarNoise;
   private final NormalNoise icebergPillarRoofNoise;
   private final NormalNoise icebergSurfaceNoise;
   private final PositionalRandomFactory noiseRandom;
   private final NormalNoise surfaceNoise;
   private final NormalNoise surfaceSecondaryNoise;

   public SurfaceSystem(RandomState p_224637_, BlockState p_224638_, int p_224639_, PositionalRandomFactory p_224640_) {
      this.defaultBlock = p_224638_;
      this.seaLevel = p_224639_;
      this.noiseRandom = p_224640_;
      this.clayBandsOffsetNoise = p_224637_.getOrCreateNoise(Noises.CLAY_BANDS_OFFSET);
      this.clayBands = generateBands(p_224640_.fromHashOf(Identifier.withDefaultNamespace("clay_bands")));
      this.surfaceNoise = p_224637_.getOrCreateNoise(Noises.SURFACE);
      this.surfaceSecondaryNoise = p_224637_.getOrCreateNoise(Noises.SURFACE_SECONDARY);
      this.badlandsPillarNoise = p_224637_.getOrCreateNoise(Noises.BADLANDS_PILLAR);
      this.badlandsPillarRoofNoise = p_224637_.getOrCreateNoise(Noises.BADLANDS_PILLAR_ROOF);
      this.badlandsSurfaceNoise = p_224637_.getOrCreateNoise(Noises.BADLANDS_SURFACE);
      this.icebergPillarNoise = p_224637_.getOrCreateNoise(Noises.ICEBERG_PILLAR);
      this.icebergPillarRoofNoise = p_224637_.getOrCreateNoise(Noises.ICEBERG_PILLAR_ROOF);
      this.icebergSurfaceNoise = p_224637_.getOrCreateNoise(Noises.ICEBERG_SURFACE);
   }

   public void buildSurface(
      RandomState p_224649_,
      BiomeManager p_224650_,
      Registry<Biome> p_224651_,
      boolean p_224652_,
      WorldGenerationContext p_224653_,
      final ChunkAccess p_224654_,
      NoiseChunk p_224655_,
      SurfaceRules.RuleSource p_224656_
   ) {
      final BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      final ChunkPos chunkpos = p_224654_.getPos();
      int i = chunkpos.getMinBlockX();
      int j = chunkpos.getMinBlockZ();
      BlockColumn blockcolumn = new BlockColumn() {
         @Override
         public BlockState getBlock(int p_190006_) {
            return p_224654_.getBlockState(blockpos$mutableblockpos.setY(p_190006_));
         }

         @Override
         public void setBlock(int p_190008_, BlockState p_190009_) {
            LevelHeightAccessor levelheightaccessor = p_224654_.getHeightAccessorForGeneration();
            if (levelheightaccessor.isInsideBuildHeight(p_190008_)) {
               p_224654_.setBlockState(blockpos$mutableblockpos.setY(p_190008_), p_190009_);
               if (!p_190009_.getFluidState().isEmpty()) {
                  p_224654_.markPosForPostprocessing(blockpos$mutableblockpos);
               }
            }
         }

         @Override
         public String toString() {
            return "ChunkBlockColumn " + chunkpos;
         }
      };
      SurfaceRules.Context surfacerules$context = new SurfaceRules.Context(this, p_224649_, p_224654_, p_224655_, p_224650_::getBiome, p_224651_, p_224653_);
      SurfaceRules.SurfaceRule surfacerules$surfacerule = p_224656_.apply(surfacerules$context);
      BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();

      for (int k = 0; k < 16; k++) {
         for (int l = 0; l < 16; l++) {
            int i1 = i + k;
            int j1 = j + l;
            int k1 = p_224654_.getHeight(Heightmap.Types.WORLD_SURFACE_WG, k, l) + 1;
            blockpos$mutableblockpos.setX(i1).setZ(j1);
            Holder<Biome> holder = p_224650_.getBiome(blockpos$mutableblockpos1.set(i1, p_224652_ ? 0 : k1, j1));
            if (holder.is(Biomes.ERODED_BADLANDS)) {
               this.erodedBadlandsExtension(blockcolumn, i1, j1, k1, p_224654_);
            }

            int l1 = p_224654_.getHeight(Heightmap.Types.WORLD_SURFACE_WG, k, l) + 1;
            surfacerules$context.updateXZ(i1, j1);
            int i2 = 0;
            int j2 = Integer.MIN_VALUE;
            int k2 = Integer.MAX_VALUE;
            int l2 = p_224654_.getMinY();

            for (int i3 = l1; i3 >= l2; i3--) {
               BlockState blockstate = blockcolumn.getBlock(i3);
               if (blockstate.isAir()) {
                  i2 = 0;
                  j2 = Integer.MIN_VALUE;
               } else if (!blockstate.getFluidState().isEmpty()) {
                  if (j2 == Integer.MIN_VALUE) {
                     j2 = i3 + 1;
                  }
               } else {
                  if (k2 >= i3) {
                     k2 = DimensionType.WAY_BELOW_MIN_Y;

                     for (int j3 = i3 - 1; j3 >= l2 - 1; j3--) {
                        BlockState blockstate1 = blockcolumn.getBlock(j3);
                        if (!this.isStone(blockstate1)) {
                           k2 = j3 + 1;
                           break;
                        }
                     }
                  }

                  i2++;
                  int k3 = i3 - k2 + 1;
                  surfacerules$context.updateY(i2, k3, j2, i1, i3, j1);
                  if (blockstate == this.defaultBlock) {
                     BlockState blockstate2 = surfacerules$surfacerule.tryApply(i1, i3, j1);
                     if (blockstate2 != null) {
                        blockcolumn.setBlock(i3, blockstate2);
                     }
                  }
               }
            }

            if (holder.is(Biomes.FROZEN_OCEAN) || holder.is(Biomes.DEEP_FROZEN_OCEAN)) {
               this.frozenOceanExtension(surfacerules$context.getMinSurfaceLevel(), holder.value(), blockcolumn, blockpos$mutableblockpos1, i1, j1, k1);
            }
         }
      }
   }

   protected int getSurfaceDepth(int p_189928_, int p_189929_) {
      double d0 = this.surfaceNoise.getValue(p_189928_, 0.0, p_189929_);
      return (int)(d0 * 2.75 + 3.0 + this.noiseRandom.at(p_189928_, 0, p_189929_).nextDouble() * 0.25);
   }

   protected double getSurfaceSecondary(int p_202190_, int p_202191_) {
      return this.surfaceSecondaryNoise.getValue(p_202190_, 0.0, p_202191_);
   }

   private boolean isStone(BlockState p_189953_) {
      return !p_189953_.isAir() && p_189953_.getFluidState().isEmpty();
   }

   public int getSeaLevel() {
      return this.seaLevel;
   }

   @Deprecated
   public Optional<BlockState> topMaterial(
      SurfaceRules.RuleSource p_189972_,
      CarvingContext p_189973_,
      Function<BlockPos, Holder<Biome>> p_189974_,
      ChunkAccess p_189975_,
      NoiseChunk p_189976_,
      BlockPos p_189977_,
      boolean p_189978_
   ) {
      SurfaceRules.Context surfacerules$context = new SurfaceRules.Context(
         this, p_189973_.randomState(), p_189975_, p_189976_, p_189974_, p_189973_.registryAccess().lookupOrThrow(Registries.BIOME), p_189973_
      );
      SurfaceRules.SurfaceRule surfacerules$surfacerule = p_189972_.apply(surfacerules$context);
      int i = p_189977_.getX();
      int j = p_189977_.getY();
      int k = p_189977_.getZ();
      surfacerules$context.updateXZ(i, k);
      surfacerules$context.updateY(1, 1, p_189978_ ? j + 1 : Integer.MIN_VALUE, i, j, k);
      BlockState blockstate = surfacerules$surfacerule.tryApply(i, j, k);
      return Optional.ofNullable(blockstate);
   }

   private void erodedBadlandsExtension(BlockColumn p_189955_, int p_189956_, int p_189957_, int p_189958_, LevelHeightAccessor p_189959_) {
      double d0 = 0.2;
      double d1 = Math.min(
         Math.abs(this.badlandsSurfaceNoise.getValue(p_189956_, 0.0, p_189957_) * 8.25),
         this.badlandsPillarNoise.getValue(p_189956_ * 0.2, 0.0, p_189957_ * 0.2) * 15.0
      );
      if (!(d1 <= 0.0)) {
         double d2 = 0.75;
         double d3 = 1.5;
         double d4 = Math.abs(this.badlandsPillarRoofNoise.getValue(p_189956_ * 0.75, 0.0, p_189957_ * 0.75) * 1.5);
         double d5 = 64.0 + Math.min(d1 * d1 * 2.5, Math.ceil(d4 * 50.0) + 24.0);
         int i = Mth.floor(d5);
         if (p_189958_ <= i) {
            for (int j = i; j >= p_189959_.getMinY(); j--) {
               BlockState blockstate = p_189955_.getBlock(j);
               if (blockstate.is(this.defaultBlock.getBlock())) {
                  break;
               }

               if (blockstate.is(Blocks.WATER)) {
                  return;
               }
            }

            for (int k = i; k >= p_189959_.getMinY() && p_189955_.getBlock(k).isAir(); k--) {
               p_189955_.setBlock(k, this.defaultBlock);
            }
         }
      }
   }

   private void frozenOceanExtension(
      int p_189935_, Biome p_189936_, BlockColumn p_189937_, BlockPos.MutableBlockPos p_189938_, int p_189939_, int p_189940_, int p_189941_
   ) {
      double d0 = 1.28;
      double d1 = Math.min(
         Math.abs(this.icebergSurfaceNoise.getValue(p_189939_, 0.0, p_189940_) * 8.25),
         this.icebergPillarNoise.getValue(p_189939_ * 1.28, 0.0, p_189940_ * 1.28) * 15.0
      );
      if (!(d1 <= 1.8)) {
         double d3 = 1.17;
         double d4 = 1.5;
         double d5 = Math.abs(this.icebergPillarRoofNoise.getValue(p_189939_ * 1.17, 0.0, p_189940_ * 1.17) * 1.5);
         double d6 = Math.min(d1 * d1 * 1.2, Math.ceil(d5 * 40.0) + 14.0);
         if (p_189936_.shouldMeltFrozenOceanIcebergSlightly(p_189938_.set(p_189939_, this.seaLevel, p_189940_), this.seaLevel)) {
            d6 -= 2.0;
         }

         double d2;
         if (d6 > 2.0) {
            d2 = this.seaLevel - d6 - 7.0;
            d6 += this.seaLevel;
         } else {
            d6 = 0.0;
            d2 = 0.0;
         }

         double d7 = d6;
         RandomSource randomsource = this.noiseRandom.at(p_189939_, 0, p_189940_);
         int i = 2 + randomsource.nextInt(4);
         int j = this.seaLevel + 18 + randomsource.nextInt(10);
         int k = 0;

         for (int l = Math.max(p_189941_, (int)d6 + 1); l >= p_189935_; l--) {
            if (p_189937_.getBlock(l).isAir() && l < (int)d7 && randomsource.nextDouble() > 0.01
               || p_189937_.getBlock(l).is(Blocks.WATER) && l > (int)d2 && l < this.seaLevel && d2 != 0.0 && randomsource.nextDouble() > 0.15) {
               if (k <= i && l > j) {
                  p_189937_.setBlock(l, SNOW_BLOCK);
                  k++;
               } else {
                  p_189937_.setBlock(l, PACKED_ICE);
               }
            }
         }
      }
   }

   private static BlockState[] generateBands(RandomSource p_224642_) {
      BlockState[] ablockstate = new BlockState[192];
      Arrays.fill(ablockstate, TERRACOTTA);

      for (int k = 0; k < ablockstate.length; k++) {
         k += p_224642_.nextInt(5) + 1;
         if (k < ablockstate.length) {
            ablockstate[k] = ORANGE_TERRACOTTA;
         }
      }

      makeBands(p_224642_, ablockstate, 1, YELLOW_TERRACOTTA);
      makeBands(p_224642_, ablockstate, 2, BROWN_TERRACOTTA);
      makeBands(p_224642_, ablockstate, 1, RED_TERRACOTTA);
      int l = p_224642_.nextIntBetweenInclusive(9, 15);
      int i = 0;

      for (int j = 0; i < l && j < ablockstate.length; j += p_224642_.nextInt(16) + 4) {
         ablockstate[j] = WHITE_TERRACOTTA;
         if (j - 1 > 0 && p_224642_.nextBoolean()) {
            ablockstate[j - 1] = LIGHT_GRAY_TERRACOTTA;
         }

         if (j + 1 < ablockstate.length && p_224642_.nextBoolean()) {
            ablockstate[j + 1] = LIGHT_GRAY_TERRACOTTA;
         }

         i++;
      }

      return ablockstate;
   }

   private static void makeBands(RandomSource p_224644_, BlockState[] p_224645_, int p_224646_, BlockState p_224647_) {
      int i = p_224644_.nextIntBetweenInclusive(6, 15);

      for (int j = 0; j < i; j++) {
         int k = p_224646_ + p_224644_.nextInt(3);
         int l = p_224644_.nextInt(p_224645_.length);

         for (int i1 = 0; l + i1 < p_224645_.length && i1 < k; i1++) {
            p_224645_[l + i1] = p_224647_;
         }
      }
   }

   protected BlockState getBand(int p_189931_, int p_189932_, int p_189933_) {
      int i = (int)Math.round(this.clayBandsOffsetNoise.getValue(p_189931_, 0.0, p_189933_) * 4.0);
      return this.clayBands[(p_189932_ + i + this.clayBands.length) % this.clayBands.length];
   }
}
