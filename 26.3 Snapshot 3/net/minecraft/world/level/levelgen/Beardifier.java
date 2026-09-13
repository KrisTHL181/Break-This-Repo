package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Interval;
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
   public static final Interval RANGE = Interval.INFINITE;
   public static final int BEARD_KERNEL_RADIUS = 12;
   private static final int BEARD_KERNEL_SIZE = 24;
   private static final float[] BEARD_KERNEL = Util.make(new float[13824], kernel -> {
      for (int zi = 0; zi < 24; zi++) {
         for (int xi = 0; xi < 24; xi++) {
            for (int yi = 0; yi < 24; yi++) {
               kernel[zi * 24 * 24 + xi * 24 + yi] = (float)computeBeardContribution(xi - 12, yi - 12, zi - 12);
            }
         }
      }
   });
   public static final Beardifier EMPTY = new Beardifier(List.of(), List.of(), null);
   private final List<Beardifier.Rigid> pieces;
   private final List<JigsawJunction> junctions;
   private final @Nullable BoundingBox affectedBox;

   public static Beardifier forStructuresInChunk(final StructureManager structureManager, final ChunkPos chunkPos) {
      List<StructureStart> structureStarts = structureManager.startsForStructure(chunkPos, s -> s.terrainAdaptation() != TerrainAdjustment.NONE);
      if (structureStarts.isEmpty()) {
         return EMPTY;
      }

      int chunkStartBlockX = chunkPos.getMinBlockX();
      int chunkStartBlockZ = chunkPos.getMinBlockZ();
      List<Beardifier.Rigid> rigids = new ArrayList<>();
      List<JigsawJunction> junctions = new ArrayList<>();
      BoundingBox anyPieceBoundingBox = null;

      for (StructureStart start : structureStarts) {
         TerrainAdjustment terrainAdjustment = start.getStructure().terrainAdaptation();

         for (StructurePiece piece : start.getPieces()) {
            if (piece.isCloseToChunk(chunkPos, 12)) {
               if (piece instanceof PoolElementStructurePiece poolPiece) {
                  StructureTemplatePool.Projection projection = poolPiece.getElement().getProjection();
                  if (projection == StructureTemplatePool.Projection.RIGID) {
                     rigids.add(new Beardifier.Rigid(poolPiece.getBoundingBox(), terrainAdjustment, poolPiece.getGroundLevelDelta()));
                     anyPieceBoundingBox = includeBoundingBox(anyPieceBoundingBox, piece.getBoundingBox());
                  }

                  for (JigsawJunction junction : poolPiece.getJunctions()) {
                     int junctionX = junction.getSourceX();
                     int junctionZ = junction.getSourceZ();
                     if (junctionX > chunkStartBlockX - 12
                        && junctionZ > chunkStartBlockZ - 12
                        && junctionX < chunkStartBlockX + 15 + 12
                        && junctionZ < chunkStartBlockZ + 15 + 12) {
                        junctions.add(junction);
                        BoundingBox junctionBox = new BoundingBox(new BlockPos(junctionX, junction.getSourceGroundY(), junctionZ));
                        anyPieceBoundingBox = includeBoundingBox(anyPieceBoundingBox, junctionBox);
                     }
                  }
               } else {
                  rigids.add(new Beardifier.Rigid(piece.getBoundingBox(), terrainAdjustment, 0));
                  anyPieceBoundingBox = includeBoundingBox(anyPieceBoundingBox, piece.getBoundingBox());
               }
            }
         }
      }

      if (anyPieceBoundingBox == null) {
         return EMPTY;
      }

      BoundingBox affectedBox = anyPieceBoundingBox.inflatedBy(24);
      return new Beardifier(List.copyOf(rigids), List.copyOf(junctions), affectedBox);
   }

   private static BoundingBox includeBoundingBox(final @Nullable BoundingBox encompassingBox, final BoundingBox newBox) {
      return encompassingBox == null ? newBox : BoundingBox.encapsulating(encompassingBox, newBox);
   }

   @VisibleForTesting
   public Beardifier(final List<Beardifier.Rigid> pieces, final List<JigsawJunction> junctions, final @Nullable BoundingBox affectedBox) {
      this.pieces = pieces;
      this.junctions = junctions;
      this.affectedBox = affectedBox;
   }

   @Override
   public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
      if (this.affectedBox == null) {
         Arrays.fill(output, 0.0);
      } else {
         DensityFunctions.BeardifierOrMarker.super.fillArray(output, contextProvider);
      }
   }

   @Override
   public double compute(final DensityFunction.FunctionContext context) {
      if (this.affectedBox == null) {
         return 0.0;
      }

      int blockX = context.blockX();
      int blockY = context.blockY();
      int blockZ = context.blockZ();
      if (!this.affectedBox.isInside(blockX, blockY, blockZ)) {
         return 0.0;
      }

      double noiseValue = 0.0;

      for (Beardifier.Rigid rigid : this.pieces) {
         BoundingBox box = rigid.box();
         int groundLevelDelta = rigid.groundLevelDelta();
         int dx = Math.max(0, Math.max(box.minX() - blockX, blockX - box.maxX()));
         int dz = Math.max(0, Math.max(box.minZ() - blockZ, blockZ - box.maxZ()));
         int groundY = box.minY() + groundLevelDelta;
         int dyToGround = blockY - groundY;

         int dy = switch (rigid.terrainAdjustment()) {
            case NONE -> 0;
            case BURY, BEARD_THIN -> dyToGround;
            case BEARD_BOX -> Math.max(0, Math.max(groundY - blockY, blockY - box.maxY()));
            case ENCAPSULATE -> Math.max(0, Math.max(box.minY() - blockY, blockY - box.maxY()));
         };

         noiseValue += switch (rigid.terrainAdjustment()) {
            case NONE -> 0.0;
            case BURY -> getBuryContribution(dx, dy / 2.0, dz);
            case BEARD_THIN, BEARD_BOX -> getBeardContribution(dx, dy, dz, dyToGround) * 0.8;
            case ENCAPSULATE -> getBuryContribution(dx / 2.0, dy / 2.0, dz / 2.0) * 0.8;
         };
      }

      for (JigsawJunction junction : this.junctions) {
         int dx = blockX - junction.getSourceX();
         int dy = blockY - junction.getSourceGroundY();
         int dz = blockZ - junction.getSourceZ();
         noiseValue += getBeardContribution(dx, dy, dz, dy) * 0.4;
      }

      return noiseValue;
   }

   private static double getBuryContribution(final double dx, final double dy, final double dz) {
      double distance = Mth.length(dx, dy, dz);
      return Mth.clampedMap(distance, 0.0, 6.0, 1.0, 0.0);
   }

   private static double getBeardContribution(final int dx, final int dy, final int dz, final int yToGround) {
      int xi = dx + 12;
      int yi = dy + 12;
      int zi = dz + 12;
      if (isInKernelRange(xi) && isInKernelRange(yi) && isInKernelRange(zi)) {
         double dyWithOffset = yToGround + 0.5;
         double distanceSqr = Mth.lengthSquared(dx, dyWithOffset, dz);
         double value = -dyWithOffset * Mth.fastInvSqrt(distanceSqr / 2.0) / 2.0;
         return value * BEARD_KERNEL[zi * 24 * 24 + xi * 24 + yi];
      } else {
         return 0.0;
      }
   }

   private static boolean isInKernelRange(final int xi) {
      return xi >= 0 && xi < 24;
   }

   private static double computeBeardContribution(final int dx, final int dy, final int dz) {
      return computeBeardContribution(dx, dy + 0.5, dz);
   }

   private static double computeBeardContribution(final int dx, final double dy, final int dz) {
      double distanceSqr = Mth.lengthSquared(dx, dy, dz);
      return Math.pow(Math.E, -distanceSqr / 16.0);
   }

   @VisibleForTesting
   public record Rigid(BoundingBox box, TerrainAdjustment terrainAdjustment, int groundLevelDelta) {
   }
}
