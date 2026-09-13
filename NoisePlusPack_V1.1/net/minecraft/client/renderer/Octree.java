package net.minecraft.client.renderer;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class Octree {
   private final Octree.Branch root;
   final BlockPos cameraSectionCenter;

   public Octree(SectionPos p_370029_, int p_366086_, int p_369498_, int p_361013_) {
      int i = p_366086_ * 2 + 1;
      int j = Mth.smallestEncompassingPowerOfTwo(i);
      int k = p_366086_ * 16;
      BlockPos blockpos = p_370029_.origin();
      this.cameraSectionCenter = p_370029_.center();
      int l = blockpos.getX() - k;
      int i1 = l + j * 16 - 1;
      int j1 = j >= p_369498_ ? p_361013_ : blockpos.getY() - k;
      int k1 = j1 + j * 16 - 1;
      int l1 = blockpos.getZ() - k;
      int i2 = l1 + j * 16 - 1;
      this.root = new Octree.Branch(new BoundingBox(l, j1, l1, i1, k1, i2));
   }

   public boolean add(SectionRenderDispatcher.RenderSection p_369314_) {
      return this.root.add(p_369314_);
   }

   public void visitNodes(Octree.OctreeVisitor p_364694_, Frustum p_368650_, int p_366939_) {
      this.root.visitNodes(p_364694_, false, p_368650_, 0, p_366939_, true);
   }

   boolean isClose(double p_361646_, double p_363586_, double p_364484_, double p_366426_, double p_367659_, double p_363335_, int p_370074_) {
      int i = this.cameraSectionCenter.getX();
      int j = this.cameraSectionCenter.getY();
      int k = this.cameraSectionCenter.getZ();
      return i > p_361646_ - p_370074_
         && i < p_366426_ + p_370074_
         && j > p_363586_ - p_370074_
         && j < p_367659_ + p_370074_
         && k > p_364484_ - p_370074_
         && k < p_363335_ + p_370074_;
   }

   @OnlyIn(Dist.CLIENT)
   enum AxisSorting {
      XYZ(4, 2, 1),
      XZY(4, 1, 2),
      YXZ(2, 4, 1),
      YZX(1, 4, 2),
      ZXY(2, 1, 4),
      ZYX(1, 2, 4);

      final int xShift;
      final int yShift;
      final int zShift;

      AxisSorting(final int p_369508_, final int p_365211_, final int p_368387_) {
         this.xShift = p_369508_;
         this.yShift = p_365211_;
         this.zShift = p_368387_;
      }

      public static Octree.AxisSorting getAxisSorting(int p_362893_, int p_361700_, int p_362465_) {
         if (p_362893_ > p_361700_ && p_362893_ > p_362465_) {
            return p_361700_ > p_362465_ ? XYZ : XZY;
         } else if (p_361700_ > p_362893_ && p_361700_ > p_362465_) {
            return p_362893_ > p_362465_ ? YXZ : YZX;
         } else {
            return p_362893_ > p_361700_ ? ZXY : ZYX;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class Branch implements Octree.Node {
      private final Octree.@Nullable Node[] nodes = new Octree.Node[8];
      private final BoundingBox boundingBox;
      private final int bbCenterX;
      private final int bbCenterY;
      private final int bbCenterZ;
      private final Octree.AxisSorting sorting;
      private final boolean cameraXDiffNegative;
      private final boolean cameraYDiffNegative;
      private final boolean cameraZDiffNegative;

      public Branch(final BoundingBox p_369054_) {
         this.boundingBox = p_369054_;
         this.bbCenterX = this.boundingBox.minX() + this.boundingBox.getXSpan() / 2;
         this.bbCenterY = this.boundingBox.minY() + this.boundingBox.getYSpan() / 2;
         this.bbCenterZ = this.boundingBox.minZ() + this.boundingBox.getZSpan() / 2;
         int i = Octree.this.cameraSectionCenter.getX() - this.bbCenterX;
         int j = Octree.this.cameraSectionCenter.getY() - this.bbCenterY;
         int k = Octree.this.cameraSectionCenter.getZ() - this.bbCenterZ;
         this.sorting = Octree.AxisSorting.getAxisSorting(Math.abs(i), Math.abs(j), Math.abs(k));
         this.cameraXDiffNegative = i < 0;
         this.cameraYDiffNegative = j < 0;
         this.cameraZDiffNegative = k < 0;
      }

      public boolean add(SectionRenderDispatcher.RenderSection p_366103_) {
         long i = p_366103_.getSectionNode();
         boolean flag = SectionPos.sectionToBlockCoord(SectionPos.x(i)) - this.bbCenterX < 0;
         boolean flag1 = SectionPos.sectionToBlockCoord(SectionPos.y(i)) - this.bbCenterY < 0;
         boolean flag2 = SectionPos.sectionToBlockCoord(SectionPos.z(i)) - this.bbCenterZ < 0;
         boolean flag3 = flag != this.cameraXDiffNegative;
         boolean flag4 = flag1 != this.cameraYDiffNegative;
         boolean flag5 = flag2 != this.cameraZDiffNegative;
         int j = getNodeIndex(this.sorting, flag3, flag4, flag5);
         if (this.areChildrenLeaves()) {
            boolean flag6 = this.nodes[j] != null;
            this.nodes[j] = Octree.this.new Leaf(p_366103_);
            return !flag6;
         } else if (this.nodes[j] != null) {
            Octree.Branch octree$branch1 = (Octree.Branch)this.nodes[j];
            return octree$branch1.add(p_366103_);
         } else {
            BoundingBox boundingbox = this.createChildBoundingBox(flag, flag1, flag2);
            Octree.Branch octree$branch = Octree.this.new Branch(boundingbox);
            this.nodes[j] = octree$branch;
            return octree$branch.add(p_366103_);
         }
      }

      private static int getNodeIndex(Octree.AxisSorting p_362519_, boolean p_363738_, boolean p_363441_, boolean p_360792_) {
         int i = 0;
         if (p_363738_) {
            i += p_362519_.xShift;
         }

         if (p_363441_) {
            i += p_362519_.yShift;
         }

         if (p_360792_) {
            i += p_362519_.zShift;
         }

         return i;
      }

      private boolean areChildrenLeaves() {
         return this.boundingBox.getXSpan() == 32;
      }

      private BoundingBox createChildBoundingBox(boolean p_364452_, boolean p_368731_, boolean p_366789_) {
         int i;
         int j;
         if (p_364452_) {
            i = this.boundingBox.minX();
            j = this.bbCenterX - 1;
         } else {
            i = this.bbCenterX;
            j = this.boundingBox.maxX();
         }

         int k;
         int l;
         if (p_368731_) {
            k = this.boundingBox.minY();
            l = this.bbCenterY - 1;
         } else {
            k = this.bbCenterY;
            l = this.boundingBox.maxY();
         }

         int i1;
         int j1;
         if (p_366789_) {
            i1 = this.boundingBox.minZ();
            j1 = this.bbCenterZ - 1;
         } else {
            i1 = this.bbCenterZ;
            j1 = this.boundingBox.maxZ();
         }

         return new BoundingBox(i, k, i1, j, l, j1);
      }

      @Override
      public void visitNodes(Octree.OctreeVisitor p_369870_, boolean p_363049_, Frustum p_363949_, int p_363158_, int p_368250_, boolean p_369443_) {
         boolean flag = p_363049_;
         if (!p_363049_) {
            int i = p_363949_.cubeInFrustum(this.boundingBox);
            p_363049_ = i == -2;
            flag = i == -2 || i == -1;
         }

         if (flag) {
            p_369443_ = p_369443_
               && Octree.this.isClose(
                  this.boundingBox.minX(),
                  this.boundingBox.minY(),
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX(),
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ(),
                  p_368250_
               );
            p_369870_.visit(this, p_363049_, p_363158_, p_369443_);

            for (Octree.Node octree$node : this.nodes) {
               if (octree$node != null) {
                  octree$node.visitNodes(p_369870_, p_363049_, p_363949_, p_363158_ + 1, p_368250_, p_369443_);
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

   @OnlyIn(Dist.CLIENT)
   final class Leaf implements Octree.Node {
      private final SectionRenderDispatcher.RenderSection section;

      Leaf(final SectionRenderDispatcher.RenderSection p_368561_) {
         this.section = p_368561_;
      }

      @Override
      public void visitNodes(Octree.OctreeVisitor p_366276_, boolean p_365424_, Frustum p_366156_, int p_361139_, int p_366518_, boolean p_368604_) {
         AABB aabb = this.section.getBoundingBox();
         if (p_365424_ || p_366156_.isVisible(this.getSection().getBoundingBox())) {
            p_368604_ = p_368604_ && Octree.this.isClose(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, p_366518_);
            p_366276_.visit(this, p_365424_, p_361139_, p_368604_);
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
      void visitNodes(Octree.OctreeVisitor var1, boolean var2, Frustum var3, int var4, int var5, boolean var6);

      SectionRenderDispatcher.@Nullable RenderSection getSection();

      AABB getAABB();
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface OctreeVisitor {
      void visit(Octree.Node var1, boolean var2, int var3, boolean var4);
   }
}
