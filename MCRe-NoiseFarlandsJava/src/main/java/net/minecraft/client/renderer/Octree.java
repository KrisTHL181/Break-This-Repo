package net.minecraft.client.renderer;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

/**
 * Octree — 区块节遮挡树（MCRe NoiseFarlands far lands long 化版）
 * 原版以 int 方块坐标构建 BoundingBox，section 坐标超过 2^27（世界方块坐标 2^31）
 * 时 section << 4 溢出，树边界与比较全错 → 地形渲染失效。
 * 本版：相机中心与全部方块坐标 long 化，边界改用 AABB（double），Frustum 剔除用
 * double 相减（相机相对，float 精度安全）。
 */
@OnlyIn(Dist.CLIENT)
public class Octree {
    private final Octree.Branch root;
    private final long cameraCenterX;
    private final long cameraCenterY;
    private final long cameraCenterZ;

    public Octree(final SectionPos cameraSection, final int renderDistance, final int sectionsPerChunk, final int minBlockY) {
        int visibleAreaDiameterInSections = renderDistance * 2 + 1;
        int boundingBoxSizeInSections = Mth.smallestEncompassingPowerOfTwo(visibleAreaDiameterInSections);
        int distanceToBBEdgeInBlocks = renderDistance * 16;
        this.cameraCenterX = cameraSection.centerXLong();
        this.cameraCenterY = cameraSection.centerYLong();
        this.cameraCenterZ = cameraSection.centerZLong();
        long sizeInBlocks = boundingBoxSizeInSections * 16L;
        long minX = this.cameraCenterX - distanceToBBEdgeInBlocks;
        long minY = boundingBoxSizeInSections >= sectionsPerChunk ? minBlockY : this.cameraCenterY - distanceToBBEdgeInBlocks;
        long minZ = this.cameraCenterZ - distanceToBBEdgeInBlocks;
        // AABB max 为排他边界（= 原 BoundingBox 包含 max + 1）
        this.root = new Octree.Branch(new AABB(minX, minY, minZ, minX + sizeInBlocks, minY + sizeInBlocks, minZ + sizeInBlocks));
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
        return this.cameraCenterX > minX - closeDistance
            && this.cameraCenterX < maxX + closeDistance
            && this.cameraCenterY > minY - closeDistance
            && this.cameraCenterY < maxY + closeDistance
            && this.cameraCenterZ > minZ - closeDistance
            && this.cameraCenterZ < maxZ + closeDistance;
    }

    @OnlyIn(Dist.CLIENT)
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

        public static Octree.AxisSorting getAxisSorting(final long absXDiff, final long absYDiff, final long absZDiff) {
            if (absXDiff > absYDiff && absXDiff > absZDiff) {
                return absYDiff > absZDiff ? XYZ : XZY;
            } else if (absYDiff > absXDiff && absYDiff > absZDiff) {
                return absXDiff > absZDiff ? YXZ : YZX;
            } else {
                return absXDiff > absYDiff ? ZXY : ZYX;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private class Branch implements Octree.Node {
        private final Octree.@Nullable Node[] nodes = new Octree.Node[8];
        private final AABB boundingBox;
        private final long bbCenterX;
        private final long bbCenterY;
        private final long bbCenterZ;
        private final Octree.AxisSorting sorting;
        private final boolean cameraXDiffNegative;
        private final boolean cameraYDiffNegative;
        private final boolean cameraZDiffNegative;

        public Branch(final AABB boundingBox) {
            this.boundingBox = boundingBox;
            this.bbCenterX = (long)(boundingBox.minX + boundingBox.getXsize() / 2.0);
            this.bbCenterY = (long)(boundingBox.minY + boundingBox.getYsize() / 2.0);
            this.bbCenterZ = (long)(boundingBox.minZ + boundingBox.getZsize() / 2.0);
            long cameraXDiff = Octree.this.cameraCenterX - this.bbCenterX;
            long cameraYDiff = Octree.this.cameraCenterY - this.bbCenterY;
            long cameraZDiff = Octree.this.cameraCenterZ - this.bbCenterZ;
            this.sorting = Octree.AxisSorting.getAxisSorting(Math.abs(cameraXDiff), Math.abs(cameraYDiff), Math.abs(cameraZDiff));
            this.cameraXDiffNegative = cameraXDiff < 0;
            this.cameraYDiffNegative = cameraYDiff < 0;
            this.cameraZDiffNegative = cameraZDiff < 0;
        }

        public boolean add(final SectionRenderDispatcher.RenderSection section) {
            SectionPos sectionNode = section.getSectionNode();
            boolean sectionXDiffNegative = sectionNode.minBlockXLong() - this.bbCenterX < 0;
            boolean sectionYDiffNegative = sectionNode.minBlockYLong() - this.bbCenterY < 0;
            boolean sectionZDiffNegative = sectionNode.minBlockZLong() - this.bbCenterZ < 0;
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
                AABB childBoundingBox = this.createChildBoundingBox(sectionXDiffNegative, sectionYDiffNegative, sectionZDiffNegative);
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
            return this.boundingBox.getXsize() == 32.0;
        }

        private AABB createChildBoundingBox(final boolean sectionXDiffNegative, final boolean sectionYDiffNegative, final boolean sectionZDiffNegative) {
            double minX;
            double maxX;
            if (sectionXDiffNegative) {
                minX = this.boundingBox.minX;
                maxX = this.bbCenterX;
            } else {
                minX = this.bbCenterX;
                maxX = this.boundingBox.maxX;
            }

            double minY;
            double maxY;
            if (sectionYDiffNegative) {
                minY = this.boundingBox.minY;
                maxY = this.bbCenterY;
            } else {
                minY = this.bbCenterY;
                maxY = this.boundingBox.maxY;
            }

            double minZ;
            double maxZ;
            if (sectionZDiffNegative) {
                minZ = this.boundingBox.minZ;
                maxZ = this.bbCenterZ;
            } else {
                minZ = this.bbCenterZ;
                maxZ = this.boundingBox.maxZ;
            }

            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
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
                        this.boundingBox.minX,
                        this.boundingBox.minY,
                        this.boundingBox.minZ,
                        this.boundingBox.maxX,
                        this.boundingBox.maxY,
                        this.boundingBox.maxZ,
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
            return this.boundingBox;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private final class Leaf implements Octree.Node {
        private final SectionRenderDispatcher.RenderSection section;

        private Leaf(final SectionRenderDispatcher.RenderSection section) {
            this.section = section;
        }

        @Override
        public void visitNodes(
            final Octree.OctreeVisitor visitor,
            final boolean skipFrustumCheck,
            final Frustum frustum,
            final int depth,
            final int closeDistance,
            boolean isClose
        ) {
            AABB boundingBox = this.section.getBoundingBox();
            if (skipFrustumCheck || frustum.isVisible(this.getSection().getBoundingBox())) {
                isClose = isClose
                    && Octree.this.isClose(
                        boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ, closeDistance
                    );
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

    @OnlyIn(Dist.CLIENT)
    public interface Node {
        void visitNodes(Octree.OctreeVisitor visitor, boolean skipFrustumCheck, Frustum frustum, int depth, final int closeDistance, boolean isClose);

        SectionRenderDispatcher.@Nullable RenderSection getSection();

        AABB getAABB();
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface OctreeVisitor {
        void visit(final Octree.Node node, final boolean fullyVisible, int depth, boolean isClose);
    }
}
