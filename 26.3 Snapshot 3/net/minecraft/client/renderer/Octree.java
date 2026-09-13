package net.minecraft.client.renderer;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class Octree {
   private final Octree.Branch root;
   private final BlockPos cameraSectionCenter;

   public Octree(final SectionPos cameraSection, final int renderDistance, final int sectionsPerChunk, final int minBlockY) {
      int visibleAreaDiameterInSections = renderDistance * 2 + 1;
      int boundingBoxSizeInSections = Mth.smallestEncompassingPowerOfTwo(visibleAreaDiameterInSections);
      int distanceToBBEdgeInBlocks = renderDistance * 16;
      BlockPos cameraSectionOrigin = cameraSection.origin();
      this.cameraSectionCenter = cameraSection.center();
      int minX = cameraSectionOrigin.getX() - distanceToBBEdgeInBlocks;
      int maxX = minX + boundingBoxSizeInSections * 16 - 1;
      int minY = boundingBoxSizeInSections >= sectionsPerChunk ? minBlockY : cameraSectionOrigin.getY() - distanceToBBEdgeInBlocks;
      int maxY = minY + boundingBoxSizeInSections * 16 - 1;
      int minZ = cameraSectionOrigin.getZ() - distanceToBBEdgeInBlocks;
      int maxZ = minZ + boundingBoxSizeInSections * 16 - 1;
      this.root = new Octree.Branch(new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
   }

   public boolean add(final SectionRenderDispatcher.RenderSection section) {
      return this.root.add(section);
   }

   public void visitNodes(final Octree.OctreeVisitor visitor, final Frustum frustum, final int closeDistance) {
      this.root.visitNodes(visitor, false, frustum, 0, closeDistance, true);
   }

   private boolean isClose(
      final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ, final int closeDistance
   ) {
      int cameraX = this.cameraSectionCenter.getX();
      int cameraY = this.cameraSectionCenter.getY();
      int cameraZ = this.cameraSectionCenter.getZ();
      return cameraX > minX - closeDistance
         && cameraX < maxX + closeDistance
         && cameraY > minY - closeDistance
         && cameraY < maxY + closeDistance
         && cameraZ > minZ - closeDistance
         && cameraZ < maxZ + closeDistance;
   }

   private enum AxisSorting {
      XYZ(4, 2, 1),
      XZY(4, 1, 2),
      YXZ(2, 4, 1),
      YZX(1, 4, 2),
      ZXY(2, 1, 4),
      ZYX(1, 2, 4);

      private final int xShift;
      private final int yShift;
      private final int zShift;

      AxisSorting(final int xShift, final int yShift, final int zShift) {
         this.xShift = xShift;
         this.yShift = yShift;
         this.zShift = zShift;
      }

      public static Octree.AxisSorting getAxisSorting(final int absXDiff, final int absYDiff, final int absZDiff) {
         if (absXDiff > absYDiff && absXDiff > absZDiff) {
            return absYDiff > absZDiff ? XYZ : XZY;
         } else if (absYDiff > absXDiff && absYDiff > absZDiff) {
            return absXDiff > absZDiff ? YXZ : YZX;
         } else {
            return absXDiff > absYDiff ? ZXY : ZYX;
         }
      }
   }

   private class Branch implements Octree.Node {
      private final Octree.@Nullable Node[] nodes = new Octree.Node[8];
      private final BoundingBox boundingBox;
      private final int bbCenterX;
      private final int bbCenterY;
      private final int bbCenterZ;
      private final Octree.AxisSorting sorting;
      private final boolean cameraXDiffNegative;
      private final boolean cameraYDiffNegative;
      private final boolean cameraZDiffNegative;

      public Branch(final BoundingBox boundingBox) {
         this.boundingBox = boundingBox;
         this.bbCenterX = this.boundingBox.minX() + this.boundingBox.getXSpan() / 2;
         this.bbCenterY = this.boundingBox.minY() + this.boundingBox.getYSpan() / 2;
         this.bbCenterZ = this.boundingBox.minZ() + this.boundingBox.getZSpan() / 2;
         int cameraXDiff = Octree.this.cameraSectionCenter.getX() - this.bbCenterX;
         int cameraYDiff = Octree.this.cameraSectionCenter.getY() - this.bbCenterY;
         int cameraZDiff = Octree.this.cameraSectionCenter.getZ() - this.bbCenterZ;
         this.sorting = Octree.AxisSorting.getAxisSorting(Math.abs(cameraXDiff), Math.abs(cameraYDiff), Math.abs(cameraZDiff));
         this.cameraXDiffNegative = cameraXDiff < 0;
         this.cameraYDiffNegative = cameraYDiff < 0;
         this.cameraZDiffNegative = cameraZDiff < 0;
      }

      public boolean add(final SectionRenderDispatcher.RenderSection section) {
         long sectionNode = section.getSectionNode();
         boolean sectionXDiffNegative = SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode)) - this.bbCenterX < 0;
         boolean sectionYDiffNegative = SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode)) - this.bbCenterY < 0;
         boolean sectionZDiffNegative = SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode)) - this.bbCenterZ < 0;
         boolean xDiffsOppositeSides = sectionXDiffNegative != this.cameraXDiffNegative;
         boolean yDiffsOppositeSides = sectionYDiffNegative != this.cameraYDiffNegative;
         boolean zDiffsOppositeSides = sectionZDiffNegative != this.cameraZDiffNegative;
         int nodeIndex = getNodeIndex(this.sorting, xDiffsOppositeSides, yDiffsOppositeSides, zDiffsOppositeSides);
         if (this.areChildrenLeaves()) {
            boolean alreadyExisted = this.nodes[nodeIndex] != null;
            this.nodes[nodeIndex] = Octree.this.new Leaf(section);
            return !alreadyExisted;
         } else if (this.nodes[nodeIndex] != null) {
            Octree.Branch branch = (Octree.Branch)this.nodes[nodeIndex];
            return branch.add(section);
         } else {
            BoundingBox childBoundingBox = this.createChildBoundingBox(sectionXDiffNegative, sectionYDiffNegative, sectionZDiffNegative);
            Octree.Branch branch = Octree.this.new Branch(childBoundingBox);
            this.nodes[nodeIndex] = branch;
            return branch.add(section);
         }
      }

      private static int getNodeIndex(
         final Octree.AxisSorting sorting, final boolean xDiffsOppositeSides, final boolean yDiffsOppositeSides, final boolean zDiffsOppositeSides
      ) {
         int index = 0;
         if (xDiffsOppositeSides) {
            index += sorting.xShift;
         }

         if (yDiffsOppositeSides) {
            index += sorting.yShift;
         }

         if (zDiffsOppositeSides) {
            index += sorting.zShift;
         }

         return index;
      }

      private boolean areChildrenLeaves() {
         return this.boundingBox.getXSpan() == 32;
      }

      private BoundingBox createChildBoundingBox(final boolean sectionXDiffNegative, final boolean sectionYDiffNegative, final boolean sectionZDiffNegative) {
         int minX;
         int maxX;
         if (sectionXDiffNegative) {
            minX = this.boundingBox.minX();
            maxX = this.bbCenterX - 1;
         } else {
            minX = this.bbCenterX;
            maxX = this.boundingBox.maxX();
         }

         int minY;
         int maxY;
         if (sectionYDiffNegative) {
            minY = this.boundingBox.minY();
            maxY = this.bbCenterY - 1;
         } else {
            minY = this.bbCenterY;
            maxY = this.boundingBox.maxY();
         }

         int minZ;
         int maxZ;
         if (sectionZDiffNegative) {
            minZ = this.boundingBox.minZ();
            maxZ = this.bbCenterZ - 1;
         } else {
            minZ = this.bbCenterZ;
            maxZ = this.boundingBox.maxZ();
         }

         return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
      }

      @Override
      public void visitNodes(
         final Octree.OctreeVisitor visitor, boolean skipFrustumCheck, final Frustum frustum, final int depth, final int closeDistance, boolean isClose
      ) {
         boolean isVisible = skipFrustumCheck;
         if (!skipFrustumCheck) {
            int checkResult = frustum.cubeInFrustum(this.boundingBox);
            skipFrustumCheck = checkResult == -2;
            isVisible = checkResult == -2 || checkResult == -1;
         }

         if (isVisible) {
            isClose = isClose
               && Octree.this.isClose(
                  this.boundingBox.minX(),
                  this.boundingBox.minY(),
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX(),
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ(),
                  closeDistance
               );
            visitor.visit(this, skipFrustumCheck, depth, isClose);

            for (Octree.Node node : this.nodes) {
               if (node != null) {
                  node.visitNodes(visitor, skipFrustumCheck, frustum, depth + 1, closeDistance, isClose);
               }
            }
         }
      }

      @Override
      public SectionRenderDispatcher.@Nullable RenderSection getSection() {
         return null;
      }

      @Override
      public AABB getAABB() {
         return new AABB(
            this.boundingBox.minX(),
            this.boundingBox.minY(),
            this.boundingBox.minZ(),
            this.boundingBox.maxX() + 1,
            this.boundingBox.maxY() + 1,
            this.boundingBox.maxZ() + 1
         );
      }
   }

   private final class Leaf implements Octree.Node {
      private final SectionRenderDispatcher.RenderSection section;

      private Leaf(final SectionRenderDispatcher.RenderSection section) {
         this.section = section;
      }

      @Override
      public void visitNodes(
         final Octree.OctreeVisitor visitor, final boolean skipFrustumCheck, final Frustum frustum, final int depth, final int closeDistance, boolean isClose
      ) {
         AABB boundingBox = this.section.getBoundingBox();
         if (skipFrustumCheck || frustum.isVisible(this.getSection().getBoundingBox())) {
            isClose = isClose
               && Octree.this.isClose(boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ, closeDistance);
            visitor.visit(this, skipFrustumCheck, depth, isClose);
         }
      }

      @Override
      public SectionRenderDispatcher.RenderSection getSection() {
         return this.section;
      }

      @Override
      public AABB getAABB() {
         return this.section.getBoundingBox();
      }
   }

   public interface Node {
      void visitNodes(Octree.OctreeVisitor visitor, boolean skipFrustumCheck, Frustum frustum, int depth, final int closeDistance, boolean isClose);

      SectionRenderDispatcher.@Nullable RenderSection getSection();

      AABB getAABB();
   }

   @FunctionalInterface
   public interface OctreeVisitor {
      void visit(final Octree.Node node, final boolean fullyVisible, int depth, boolean isClose);
   }
}
