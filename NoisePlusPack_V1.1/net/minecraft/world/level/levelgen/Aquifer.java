package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jspecify.annotations.Nullable;

public interface Aquifer {
   static Aquifer create(
      NoiseChunk p_223881_,
      ChunkPos p_223882_,
      NoiseRouter p_223883_,
      PositionalRandomFactory p_223884_,
      int p_223885_,
      int p_223886_,
      Aquifer.FluidPicker p_223887_
   ) {
      return new Aquifer.NoiseBasedAquifer(p_223881_, p_223882_, p_223883_, p_223884_, p_223885_, p_223886_, p_223887_);
   }

   static Aquifer createDisabled(final Aquifer.FluidPicker p_188375_) {
      return new Aquifer() {
         @Override
         public @Nullable BlockState computeSubstance(DensityFunction.FunctionContext p_208172_, double p_208173_) {
            return p_208173_ > 0.0 ? null : p_188375_.computeFluid(p_208172_.blockX(), p_208172_.blockY(), p_208172_.blockZ()).at(p_208172_.blockY());
         }

         @Override
         public boolean shouldScheduleFluidUpdate() {
            return false;
         }
      };
   }

   @Nullable BlockState computeSubstance(DensityFunction.FunctionContext var1, double var2);

   boolean shouldScheduleFluidUpdate();

   interface FluidPicker {
      Aquifer.FluidStatus computeFluid(int var1, int var2, int var3);
   }

   record FluidStatus(int fluidLevel, BlockState fluidType) {
      public BlockState at(int p_188406_) {
         return p_188406_ < this.fluidLevel ? this.fluidType : Blocks.AIR.defaultBlockState();
      }
   }

   class NoiseBasedAquifer implements Aquifer {
      private static final int X_RANGE = 10;
      private static final int Y_RANGE = 9;
      private static final int Z_RANGE = 10;
      private static final int X_SEPARATION = 6;
      private static final int Y_SEPARATION = 3;
      private static final int Z_SEPARATION = 6;
      private static final int X_SPACING = 16;
      private static final int Y_SPACING = 12;
      private static final int Z_SPACING = 16;
      private static final int X_SPACING_SHIFT = 4;
      private static final int Z_SPACING_SHIFT = 4;
      private static final int MAX_REASONABLE_DISTANCE_TO_AQUIFER_CENTER = 11;
      private static final double FLOWING_UPDATE_SIMULARITY = similarity(Mth.square(10), Mth.square(12));
      private static final int SAMPLE_OFFSET_X = -5;
      private static final int SAMPLE_OFFSET_Y = 1;
      private static final int SAMPLE_OFFSET_Z = -5;
      private static final int MIN_CELL_SAMPLE_X = 0;
      private static final int MIN_CELL_SAMPLE_Y = -1;
      private static final int MIN_CELL_SAMPLE_Z = 0;
      private static final int MAX_CELL_SAMPLE_X = 1;
      private static final int MAX_CELL_SAMPLE_Y = 1;
      private static final int MAX_CELL_SAMPLE_Z = 1;
      private final NoiseChunk noiseChunk;
      private final DensityFunction barrierNoise;
      private final DensityFunction fluidLevelFloodednessNoise;
      private final DensityFunction fluidLevelSpreadNoise;
      private final DensityFunction lavaNoise;
      private final PositionalRandomFactory positionalRandomFactory;
      private final Aquifer.@Nullable FluidStatus[] aquiferCache;
      private final long[] aquiferLocationCache;
      private final Aquifer.FluidPicker globalFluidPicker;
      private final DensityFunction erosion;
      private final DensityFunction depth;
      private boolean shouldScheduleFluidUpdate;
      private final int skipSamplingAboveY;
      private final int minGridX;
      private final int minGridY;
      private final int minGridZ;
      private final int gridSizeX;
      private final int gridSizeZ;
      private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][]{
         {0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
      };

      NoiseBasedAquifer(
         NoiseChunk p_223891_,
         ChunkPos p_223892_,
         NoiseRouter p_223893_,
         PositionalRandomFactory p_223894_,
         int p_223895_,
         int p_223896_,
         Aquifer.FluidPicker p_223897_
      ) {
         this.noiseChunk = p_223891_;
         this.barrierNoise = p_223893_.barrierNoise();
         this.fluidLevelFloodednessNoise = p_223893_.fluidLevelFloodednessNoise();
         this.fluidLevelSpreadNoise = p_223893_.fluidLevelSpreadNoise();
         this.lavaNoise = p_223893_.lavaNoise();
         this.erosion = p_223893_.erosion();
         this.depth = p_223893_.depth();
         this.positionalRandomFactory = p_223894_;
         this.minGridX = gridX(p_223892_.getMinBlockX() + -5) + 0;
         this.globalFluidPicker = p_223897_;
         int i = gridX(p_223892_.getMaxBlockX() + -5) + 1;
         this.gridSizeX = i - this.minGridX + 1;
         this.minGridY = gridY(p_223895_ + 1) + -1;
         int j = gridY(p_223895_ + p_223896_ + 1) + 1;
         int k = j - this.minGridY + 1;
         this.minGridZ = gridZ(p_223892_.getMinBlockZ() + -5) + 0;
         int l = gridZ(p_223892_.getMaxBlockZ() + -5) + 1;
         this.gridSizeZ = l - this.minGridZ + 1;
         int i1 = this.gridSizeX * k * this.gridSizeZ;
         this.aquiferCache = new Aquifer.FluidStatus[i1];
         this.aquiferLocationCache = new long[i1];
         Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
         int j1 = this.adjustSurfaceLevel(
            p_223891_.maxPreliminarySurfaceLevel(fromGridX(this.minGridX, 0), fromGridZ(this.minGridZ, 0), fromGridX(i, 9), fromGridZ(l, 9))
         );
         int k1 = gridY(j1 + 12) - -1;
         this.skipSamplingAboveY = fromGridY(k1, 11) - 1;
      }

      private int getIndex(int p_158028_, int p_158029_, int p_158030_) {
         int i = p_158028_ - this.minGridX;
         int j = p_158029_ - this.minGridY;
         int k = p_158030_ - this.minGridZ;
         return (j * this.gridSizeZ + k) * this.gridSizeX + i;
      }

      @Override
      public @Nullable BlockState computeSubstance(DensityFunction.FunctionContext p_208186_, double p_208187_) {
         if (p_208187_ > 0.0) {
            this.shouldScheduleFluidUpdate = false;
            return null;
         }

         int i = p_208186_.blockX();
         int j = p_208186_.blockY();
         int k = p_208186_.blockZ();
         Aquifer.FluidStatus aquifer$fluidstatus = this.globalFluidPicker.computeFluid(i, j, k);
         if (j > this.skipSamplingAboveY) {
            this.shouldScheduleFluidUpdate = false;
            return aquifer$fluidstatus.at(j);
         }

         if (aquifer$fluidstatus.at(j).is(Blocks.LAVA)) {
            this.shouldScheduleFluidUpdate = false;
            return SharedConstants.DEBUG_DISABLE_FLUID_GENERATION ? Blocks.AIR.defaultBlockState() : Blocks.LAVA.defaultBlockState();
         }

         int l = gridX(i + -5);
         int i1 = gridY(j + 1);
         int j1 = gridZ(k + -5);
         int k1 = Integer.MAX_VALUE;
         int l1 = Integer.MAX_VALUE;
         int i2 = Integer.MAX_VALUE;
         int j2 = Integer.MAX_VALUE;
         int k2 = 0;
         int l2 = 0;
         int i3 = 0;
         int j3 = 0;

         for (int k3 = 0; k3 <= 1; k3++) {
            for (int l3 = -1; l3 <= 1; l3++) {
               for (int i4 = 0; i4 <= 1; i4++) {
                  int j4 = l + k3;
                  int k4 = i1 + l3;
                  int l4 = j1 + i4;
                  int i5 = this.getIndex(j4, k4, l4);
                  long k5 = this.aquiferLocationCache[i5];
                  long j5;
                  if (k5 != Long.MAX_VALUE) {
                     j5 = k5;
                  } else {
                     RandomSource randomsource = this.positionalRandomFactory.at(j4, k4, l4);
                     j5 = BlockPos.asLong(
                        fromGridX(j4, randomsource.nextInt(10)), fromGridY(k4, randomsource.nextInt(9)), fromGridZ(l4, randomsource.nextInt(10))
                     );
                     this.aquiferLocationCache[i5] = j5;
                  }

                  int k6 = BlockPos.getX(j5) - i;
                  int l5 = BlockPos.getY(j5) - j;
                  int i6 = BlockPos.getZ(j5) - k;
                  int j6 = k6 * k6 + l5 * l5 + i6 * i6;
                  if (k1 >= j6) {
                     j3 = i3;
                     i3 = l2;
                     l2 = k2;
                     k2 = i5;
                     j2 = i2;
                     i2 = l1;
                     l1 = k1;
                     k1 = j6;
                  } else if (l1 >= j6) {
                     j3 = i3;
                     i3 = l2;
                     l2 = i5;
                     j2 = i2;
                     i2 = l1;
                     l1 = j6;
                  } else if (i2 >= j6) {
                     j3 = i3;
                     i3 = i5;
                     j2 = i2;
                     i2 = j6;
                  } else if (j2 >= j6) {
                     j3 = i5;
                     j2 = j6;
                  }
               }
            }
         }

         Aquifer.FluidStatus aquifer$fluidstatus1 = this.getAquiferStatus(k2);
         double d1 = similarity(k1, l1);
         BlockState blockstate = aquifer$fluidstatus1.at(j);
         BlockState blockstate1 = SharedConstants.DEBUG_DISABLE_FLUID_GENERATION ? Blocks.AIR.defaultBlockState() : blockstate;
         if (d1 <= 0.0) {
            if (d1 >= FLOWING_UPDATE_SIMULARITY) {
               Aquifer.FluidStatus aquifer$fluidstatus2 = this.getAquiferStatus(l2);
               this.shouldScheduleFluidUpdate = !aquifer$fluidstatus1.equals(aquifer$fluidstatus2);
            } else {
               this.shouldScheduleFluidUpdate = false;
            }

            return blockstate1;
         } else {
            if (blockstate.is(Blocks.WATER) && this.globalFluidPicker.computeFluid(i, j - 1, k).at(j - 1).is(Blocks.LAVA)) {
               this.shouldScheduleFluidUpdate = true;
               return blockstate1;
            }

            MutableDouble mutabledouble = new MutableDouble(Double.NaN);
            Aquifer.FluidStatus aquifer$fluidstatus3 = this.getAquiferStatus(l2);
            double d2 = d1 * this.calculatePressure(p_208186_, mutabledouble, aquifer$fluidstatus1, aquifer$fluidstatus3);
            if (p_208187_ + d2 > 0.0) {
               this.shouldScheduleFluidUpdate = false;
               return null;
            }

            Aquifer.FluidStatus aquifer$fluidstatus4 = this.getAquiferStatus(i3);
            double d0 = similarity(k1, i2);
            if (d0 > 0.0) {
               double d3 = d1 * d0 * this.calculatePressure(p_208186_, mutabledouble, aquifer$fluidstatus1, aquifer$fluidstatus4);
               if (p_208187_ + d3 > 0.0) {
                  this.shouldScheduleFluidUpdate = false;
                  return null;
               }
            }

            double d4 = similarity(l1, i2);
            if (d4 > 0.0) {
               double d5 = d1 * d4 * this.calculatePressure(p_208186_, mutabledouble, aquifer$fluidstatus3, aquifer$fluidstatus4);
               if (p_208187_ + d5 > 0.0) {
                  this.shouldScheduleFluidUpdate = false;
                  return null;
               }
            }

            boolean flag2 = !aquifer$fluidstatus1.equals(aquifer$fluidstatus3);
            boolean flag = d4 >= FLOWING_UPDATE_SIMULARITY && !aquifer$fluidstatus3.equals(aquifer$fluidstatus4);
            boolean flag1 = d0 >= FLOWING_UPDATE_SIMULARITY && !aquifer$fluidstatus1.equals(aquifer$fluidstatus4);
            if (!flag2 && !flag && !flag1) {
               this.shouldScheduleFluidUpdate = d0 >= FLOWING_UPDATE_SIMULARITY
                  && similarity(k1, j2) >= FLOWING_UPDATE_SIMULARITY
                  && !aquifer$fluidstatus1.equals(this.getAquiferStatus(j3));
            } else {
               this.shouldScheduleFluidUpdate = true;
            }

            return blockstate1;
         }
      }

      @Override
      public boolean shouldScheduleFluidUpdate() {
         return this.shouldScheduleFluidUpdate;
      }

      private static double similarity(int p_158025_, int p_158026_) {
         double d0 = 25.0;
         return 1.0 - (p_158026_ - p_158025_) / 25.0;
      }

      private double calculatePressure(
         DensityFunction.FunctionContext p_208189_, MutableDouble p_208190_, Aquifer.FluidStatus p_208191_, Aquifer.FluidStatus p_208192_
      ) {
         int i = p_208189_.blockY();
         BlockState blockstate = p_208191_.at(i);
         BlockState blockstate1 = p_208192_.at(i);
         if ((!blockstate.is(Blocks.LAVA) || !blockstate1.is(Blocks.WATER)) && (!blockstate.is(Blocks.WATER) || !blockstate1.is(Blocks.LAVA))) {
            int j = Math.abs(p_208191_.fluidLevel - p_208192_.fluidLevel);
            if (j == 0) {
               return 0.0;
            }

            double d0 = 0.5 * (p_208191_.fluidLevel + p_208192_.fluidLevel);
            double d1 = i + 0.5 - d0;
            double d2 = j / 2.0;
            double d3 = 0.0;
            double d4 = 2.5;
            double d5 = 1.5;
            double d6 = 3.0;
            double d7 = 10.0;
            double d8 = 3.0;
            double d9 = d2 - Math.abs(d1);
            double d10;
            if (d1 > 0.0) {
               double d11 = 0.0 + d9;
               if (d11 > 0.0) {
                  d10 = d11 / 1.5;
               } else {
                  d10 = d11 / 2.5;
               }
            } else {
               double d15 = 3.0 + d9;
               if (d15 > 0.0) {
                  d10 = d15 / 3.0;
               } else {
                  d10 = d15 / 10.0;
               }
            }

            double d16 = 2.0;
            double d12;
            if (!(d10 < -2.0) && !(d10 > 2.0)) {
               double d13 = p_208190_.doubleValue();
               if (Double.isNaN(d13)) {
                  double d14 = this.barrierNoise.compute(p_208189_);
                  p_208190_.setValue(d14);
                  d12 = d14;
               } else {
                  d12 = d13;
               }
            } else {
               d12 = 0.0;
            }

            return 2.0 * (d12 + d10);
         } else {
            return 2.0;
         }
      }

      private static int gridX(int p_158040_) {
         return p_158040_ >> 4;
      }

      private static int fromGridX(int p_425117_, int p_425814_) {
         return (p_425117_ << 4) + p_425814_;
      }

      private static int gridY(int p_158046_) {
         return Math.floorDiv(p_158046_, 12);
      }

      private static int fromGridY(int p_425513_, int p_427105_) {
         return p_425513_ * 12 + p_427105_;
      }

      private static int gridZ(int p_158048_) {
         return p_158048_ >> 4;
      }

      private static int fromGridZ(int p_430486_, int p_429974_) {
         return (p_430486_ << 4) + p_429974_;
      }

      private Aquifer.FluidStatus getAquiferStatus(int p_426638_) {
         Aquifer.FluidStatus aquifer$fluidstatus = this.aquiferCache[p_426638_];
         if (aquifer$fluidstatus != null) {
            return aquifer$fluidstatus;
         }

         long i = this.aquiferLocationCache[p_426638_];
         Aquifer.FluidStatus aquifer$fluidstatus1 = this.computeFluid(BlockPos.getX(i), BlockPos.getY(i), BlockPos.getZ(i));
         this.aquiferCache[p_426638_] = aquifer$fluidstatus1;
         return aquifer$fluidstatus1;
      }

      private Aquifer.FluidStatus computeFluid(int p_188448_, int p_188449_, int p_188450_) {
         Aquifer.FluidStatus aquifer$fluidstatus = this.globalFluidPicker.computeFluid(p_188448_, p_188449_, p_188450_);
         int i = Integer.MAX_VALUE;
         int j = p_188449_ + 12;
         int k = p_188449_ - 12;
         boolean flag = false;

         for (int[] aint : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
            int l = p_188448_ + SectionPos.sectionToBlockCoord(aint[0]);
            int i1 = p_188450_ + SectionPos.sectionToBlockCoord(aint[1]);
            int j1 = this.noiseChunk.preliminarySurfaceLevel(l, i1);
            int k1 = this.adjustSurfaceLevel(j1);
            boolean flag1 = aint[0] == 0 && aint[1] == 0;
            if (flag1 && k > k1) {
               return aquifer$fluidstatus;
            }

            boolean flag2 = j > k1;
            if (flag2 || flag1) {
               Aquifer.FluidStatus aquifer$fluidstatus1 = this.globalFluidPicker.computeFluid(l, k1, i1);
               if (!aquifer$fluidstatus1.at(k1).isAir()) {
                  if (flag1) {
                     flag = true;
                  }

                  if (flag2) {
                     return aquifer$fluidstatus1;
                  }
               }
            }

            i = Math.min(i, j1);
         }

         int l1 = this.computeSurfaceLevel(p_188448_, p_188449_, p_188450_, aquifer$fluidstatus, i, flag);
         return new Aquifer.FluidStatus(l1, this.computeFluidType(p_188448_, p_188449_, p_188450_, aquifer$fluidstatus, l1));
      }

      private int adjustSurfaceLevel(int p_428499_) {
         return p_428499_ + 8;
      }

      private int computeSurfaceLevel(int p_223910_, int p_223911_, int p_223912_, Aquifer.FluidStatus p_223913_, int p_223914_, boolean p_223915_) {
         DensityFunction.SinglePointContext densityfunction$singlepointcontext = new DensityFunction.SinglePointContext(p_223910_, p_223911_, p_223912_);
         double d0;
         double d1;
         if (OverworldBiomeBuilder.isDeepDarkRegion(this.erosion, this.depth, densityfunction$singlepointcontext)) {
            d0 = -1.0;
            d1 = -1.0;
         } else {
            int i = p_223914_ + 8 - p_223911_;
            int j = 64;
            double d2 = p_223915_ ? Mth.clampedMap(i, 0.0, 64.0, 1.0, 0.0) : 0.0;
            double d3 = Mth.clamp(this.fluidLevelFloodednessNoise.compute(densityfunction$singlepointcontext), -1.0, 1.0);
            double d4 = Mth.map(d2, 1.0, 0.0, -0.3, 0.8);
            double d5 = Mth.map(d2, 1.0, 0.0, -0.8, 0.4);
            d0 = d3 - d5;
            d1 = d3 - d4;
         }

         int k;
         if (d1 > 0.0) {
            k = p_223913_.fluidLevel;
         } else if (d0 > 0.0) {
            k = this.computeRandomizedFluidSurfaceLevel(p_223910_, p_223911_, p_223912_, p_223914_);
         } else {
            k = DimensionType.WAY_BELOW_MIN_Y;
         }

         return k;
      }

      private int computeRandomizedFluidSurfaceLevel(int p_223899_, int p_223900_, int p_223901_, int p_223902_) {
         int i = 16;
         int j = 40;
         int k = Math.floorDiv(p_223899_, 16);
         int l = Math.floorDiv(p_223900_, 40);
         int i1 = Math.floorDiv(p_223901_, 16);
         int j1 = l * 40 + 20;
         int k1 = 10;
         double d0 = this.fluidLevelSpreadNoise.compute(new DensityFunction.SinglePointContext(k, l, i1)) * 10.0;
         int l1 = Mth.quantize(d0, 3);
         int i2 = j1 + l1;
         return Math.min(p_223902_, i2);
      }

      private BlockState computeFluidType(int p_223904_, int p_223905_, int p_223906_, Aquifer.FluidStatus p_223907_, int p_223908_) {
         BlockState blockstate = p_223907_.fluidType;
         if (p_223908_ <= -10 && p_223908_ != DimensionType.WAY_BELOW_MIN_Y && p_223907_.fluidType != Blocks.LAVA.defaultBlockState()) {
            int i = 64;
            int j = 40;
            int k = Math.floorDiv(p_223904_, 64);
            int l = Math.floorDiv(p_223905_, 40);
            int i1 = Math.floorDiv(p_223906_, 64);
            double d0 = this.lavaNoise.compute(new DensityFunction.SinglePointContext(k, l, i1));
            if (Math.abs(d0) > 0.3) {
               blockstate = Blocks.LAVA.defaultBlockState();
            }
         }

         return blockstate;
      }
   }
}
