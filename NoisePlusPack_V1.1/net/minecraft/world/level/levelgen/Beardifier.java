package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.jspecify.annotations.Nullable;

public class Beardifier implements DensityFunctions.BeardifierOrMarker {
   public static final int BEARD_KERNEL_RADIUS = 12;
   private static final int BEARD_KERNEL_SIZE = 24;
   private static final float[] BEARD_KERNEL = Util.make(new float[13824], p_158082_ -> {
      for (int i = 0; i < 24; i++) {
         for (int j = 0; j < 24; j++) {
            for (int k = 0; k < 24; k++) {
               p_158082_[i * 24 * 24 + j * 24 + k] = (float)computeBeardContribution(j - 12, k - 12, i - 12);
            }
         }
      }
   });
   public static final Beardifier EMPTY = new Beardifier(List.of(), List.of(), null);
   private final List<Beardifier.Rigid> pieces;
   private final List<JigsawJunction> junctions;
   private final @Nullable BoundingBox affectedBox;

   public static Beardifier forStructuresInChunk(StructureManager p_223938_, ChunkPos p_223939_) {
      List<StructureStart> list = p_223938_.startsForStructure(p_223939_, p_223941_ -> p_223941_.terrainAdaptation() != TerrainAdjustment.NONE);
      if (list.isEmpty()) {
         return EMPTY;
      }

      int i = p_223939_.getMinBlockX();
      int j = p_223939_.getMinBlockZ();
      List<Beardifier.Rigid> list1 = new ArrayList<>();
      List<JigsawJunction> list2 = new ArrayList<>();
      BoundingBox boundingbox = null;

      for (StructureStart structurestart : list) {
         TerrainAdjustment terrainadjustment = structurestart.getStructure().terrainAdaptation();

         for (StructurePiece structurepiece : structurestart.getPieces()) {
            if (structurepiece.isCloseToChunk(p_223939_, 12)) {
               if (structurepiece instanceof PoolElementStructurePiece poolelementstructurepiece) {
                  StructureTemplatePool.Projection structuretemplatepool$projection = poolelementstructurepiece.getElement().getProjection();
                  if (structuretemplatepool$projection == StructureTemplatePool.Projection.RIGID) {
                     list1.add(
                        new Beardifier.Rigid(poolelementstructurepiece.getBoundingBox(), terrainadjustment, poolelementstructurepiece.getGroundLevelDelta())
                     );
                     boundingbox = includeBoundingBox(boundingbox, structurepiece.getBoundingBox());
                  }

                  for (JigsawJunction jigsawjunction : poolelementstructurepiece.getJunctions()) {
                     int k = jigsawjunction.getSourceX();
                     int l = jigsawjunction.getSourceZ();
                     if (k > i - 12 && l > j - 12 && k < i + 15 + 12 && l < j + 15 + 12) {
                        list2.add(jigsawjunction);
                        BoundingBox boundingbox1 = new BoundingBox(new BlockPos(k, jigsawjunction.getSourceGroundY(), l));
                        boundingbox = includeBoundingBox(boundingbox, boundingbox1);
                     }
                  }
               } else {
                  list1.add(new Beardifier.Rigid(structurepiece.getBoundingBox(), terrainadjustment, 0));
                  boundingbox = includeBoundingBox(boundingbox, structurepiece.getBoundingBox());
               }
            }
         }
      }

      if (boundingbox == null) {
         return EMPTY;
      }

      BoundingBox boundingbox2 = boundingbox.inflatedBy(24);
      return new Beardifier(List.copyOf(list1), List.copyOf(list2), boundingbox2);
   }

   private static BoundingBox includeBoundingBox(@Nullable BoundingBox p_426589_, BoundingBox p_427502_) {
      return p_426589_ == null ? p_427502_ : BoundingBox.encapsulating(p_426589_, p_427502_);
   }

   @VisibleForTesting
   public Beardifier(List<Beardifier.Rigid> p_430256_, List<JigsawJunction> p_428173_, @Nullable BoundingBox p_428923_) {
      this.pieces = p_430256_;
      this.junctions = p_428173_;
      this.affectedBox = p_428923_;
   }

   @Override
   public void fillArray(double[] p_427084_, DensityFunction.ContextProvider p_426211_) {
      if (this.affectedBox == null) {
         Arrays.fill(p_427084_, 0.0);
      } else {
         DensityFunctions.BeardifierOrMarker.super.fillArray(p_427084_, p_426211_);
      }
   }

   @Override
   public double compute(DensityFunction.FunctionContext p_208200_) {
      if (this.affectedBox == null) {
         return 0.0;
      }

      int i = p_208200_.blockX();
      int j = p_208200_.blockY();
      int k = p_208200_.blockZ();
      if (!this.affectedBox.isInside(i, j, k)) {
         return 0.0;
      }

      double d0 = 0.0;

      for (Beardifier.Rigid beardifier$rigid : this.pieces) {
         BoundingBox boundingbox = beardifier$rigid.box();
         int l = beardifier$rigid.groundLevelDelta();
         int i1 = Math.max(0, Math.max(boundingbox.minX() - i, i - boundingbox.maxX()));
         int j1 = Math.max(0, Math.max(boundingbox.minZ() - k, k - boundingbox.maxZ()));
         int k1 = boundingbox.minY() + l;
         int l1 = j - k1;

         int i2 = switch (beardifier$rigid.terrainAdjustment()) {
            case NONE -> 0;
            case BURY, BEARD_THIN -> l1;
            case BEARD_BOX -> Math.max(0, Math.max(k1 - j, j - boundingbox.maxY()));
            case ENCAPSULATE -> Math.max(0, Math.max(boundingbox.minY() - j, j - boundingbox.maxY()));
         };

         d0 += switch (beardifier$rigid.terrainAdjustment()) {
            case NONE -> 0.0;
            case BURY -> getBuryContribution(i1, i2 / 2.0, j1);
            case BEARD_THIN, BEARD_BOX -> getBeardContribution(i1, i2, j1, l1) * 0.8;
            case ENCAPSULATE -> getBuryContribution(i1 / 2.0, i2 / 2.0, j1 / 2.0) * 0.8;
         };
      }

      for (JigsawJunction jigsawjunction : this.junctions) {
         int j2 = i - jigsawjunction.getSourceX();
         int k2 = j - jigsawjunction.getSourceGroundY();
         int l2 = k - jigsawjunction.getSourceZ();
         d0 += getBeardContribution(j2, k2, l2, k2) * 0.4;
      }

      return d0;
   }

   @Override
   public double minValue() {
      return Double.NEGATIVE_INFINITY;
   }

   @Override
   public double maxValue() {
      return Double.POSITIVE_INFINITY;
   }

   private static double getBuryContribution(double p_328731_, double p_336073_, double p_329819_) {
      double d0 = Mth.length(p_328731_, p_336073_, p_329819_);
      return Mth.clampedMap(d0, 0.0, 6.0, 1.0, 0.0);
   }

   private static double getBeardContribution(int p_223926_, int p_223927_, int p_223928_, int p_223929_) {
      int i = p_223926_ + 12;
      int j = p_223927_ + 12;
      int k = p_223928_ + 12;
      if (isInKernelRange(i) && isInKernelRange(j) && isInKernelRange(k)) {
         double d0 = p_223929_ + 0.5;
         double d1 = Mth.lengthSquared(p_223926_, d0, p_223928_);
         double d2 = -d0 * Mth.fastInvSqrt(d1 / 2.0) / 2.0;
         return d2 * BEARD_KERNEL[k * 24 * 24 + i * 24 + j];
      } else {
         return 0.0;
      }
   }

   private static boolean isInKernelRange(int p_223920_) {
      return p_223920_ >= 0 && p_223920_ < 24;
   }

   private static double computeBeardContribution(int p_158092_, int p_158093_, int p_158094_) {
      return computeBeardContribution(p_158092_, p_158093_ + 0.5, p_158094_);
   }

   private static double computeBeardContribution(int p_223922_, double p_223923_, int p_223924_) {
      double d0 = Mth.lengthSquared(p_223922_, p_223923_, p_223924_);
      return Math.pow(Math.E, -d0 / 16.0);
   }

   @VisibleForTesting
   public record Rigid(BoundingBox box, TerrainAdjustment terrainAdjustment, int groundLevelDelta) {
   }
}
