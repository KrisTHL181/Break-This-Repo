package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ViewArea {
   protected final LevelRenderer levelRenderer;
   protected final Level level;
   protected int sectionGridSizeY;
   protected int sectionGridSizeX;
   protected int sectionGridSizeZ;
   private int viewDistance;
   private SectionPos cameraSectionPos;
   public SectionRenderDispatcher.RenderSection[] sections;

   public ViewArea(SectionRenderDispatcher p_298339_, Level p_110846_, int p_110847_, LevelRenderer p_110848_) {
      this.levelRenderer = p_110848_;
      this.level = p_110846_;
      this.setViewDistance(p_110847_);
      this.createSections(p_298339_);
      this.cameraSectionPos = SectionPos.of(this.viewDistance + 1, 0, this.viewDistance + 1);
   }

   protected void createSections(SectionRenderDispatcher p_299921_) {
      if (!Minecraft.getInstance().isSameThread()) {
         throw new IllegalStateException("createSections called from wrong thread: " + Thread.currentThread().getName());
      }

      int i = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
      this.sections = new SectionRenderDispatcher.RenderSection[i];

      for (int j = 0; j < this.sectionGridSizeX; j++) {
         for (int k = 0; k < this.sectionGridSizeY; k++) {
            for (int l = 0; l < this.sectionGridSizeZ; l++) {
               int i1 = this.getSectionIndex(j, k, l);
               this.sections[i1] = p_299921_.new RenderSection(i1, SectionPos.asLong(j, k + this.level.getMinSectionY(), l));
            }
         }
      }
   }

   public void releaseAllBuffers() {
      for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.sections) {
         sectionrenderdispatcher$rendersection.reset();
      }
   }

   private int getSectionIndex(int p_297902_, int p_298060_, int p_297930_) {
      return (p_297930_ * this.sectionGridSizeY + p_298060_) * this.sectionGridSizeX + p_297902_;
   }

   protected void setViewDistance(int p_110854_) {
      int i = p_110854_ * 2 + 1;
      this.sectionGridSizeX = i;
      this.sectionGridSizeY = this.level.getSectionsCount();
      this.sectionGridSizeZ = i;
      this.viewDistance = p_110854_;
   }

   public int getViewDistance() {
      return this.viewDistance;
   }

   public LevelHeightAccessor getLevelHeightAccessor() {
      return this.level;
   }

   public void repositionCamera(SectionPos p_362419_) {
      for (int i = 0; i < this.sectionGridSizeX; i++) {
         int j = p_362419_.x() - this.viewDistance;
         int k = j + Math.floorMod(i - j, this.sectionGridSizeX);

         for (int l = 0; l < this.sectionGridSizeZ; l++) {
            int i1 = p_362419_.z() - this.viewDistance;
            int j1 = i1 + Math.floorMod(l - i1, this.sectionGridSizeZ);

            for (int k1 = 0; k1 < this.sectionGridSizeY; k1++) {
               int l1 = this.level.getMinSectionY() + k1;
               SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.sections[this.getSectionIndex(i, k1, l)];
               long i2 = sectionrenderdispatcher$rendersection.getSectionNode();
               if (i2 != SectionPos.asLong(k, l1, j1)) {
                  sectionrenderdispatcher$rendersection.setSectionNode(SectionPos.asLong(k, l1, j1));
               }
            }
         }
      }

      this.cameraSectionPos = p_362419_;
      this.levelRenderer.getSectionOcclusionGraph().invalidate();
   }

   public SectionPos getCameraSectionPos() {
      return this.cameraSectionPos;
   }

   public void setDirty(int p_110860_, int p_110861_, int p_110862_, boolean p_110863_) {
      SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.getRenderSection(p_110860_, p_110861_, p_110862_);
      if (sectionrenderdispatcher$rendersection != null) {
         sectionrenderdispatcher$rendersection.setDirty(p_110863_);
      }
   }

   protected SectionRenderDispatcher.@Nullable RenderSection getRenderSectionAt(BlockPos p_299271_) {
      return this.getRenderSection(SectionPos.asLong(p_299271_));
   }

   protected SectionRenderDispatcher.@Nullable RenderSection getRenderSection(long p_365615_) {
      int i = SectionPos.x(p_365615_);
      int j = SectionPos.y(p_365615_);
      int k = SectionPos.z(p_365615_);
      return this.getRenderSection(i, j, k);
   }

   private SectionRenderDispatcher.@Nullable RenderSection getRenderSection(int p_364566_, int p_363739_, int p_369645_) {
      if (!this.containsSection(p_364566_, p_363739_, p_369645_)) {
         return null;
      }

      int i = p_363739_ - this.level.getMinSectionY();
      int j = Math.floorMod(p_364566_, this.sectionGridSizeX);
      int k = Math.floorMod(p_369645_, this.sectionGridSizeZ);
      return this.sections[this.getSectionIndex(j, i, k)];
   }

   private boolean containsSection(int p_364426_, int p_367122_, int p_363673_) {
      if (p_367122_ >= this.level.getMinSectionY() && p_367122_ <= this.level.getMaxSectionY()) {
         return p_364426_ >= this.cameraSectionPos.x() - this.viewDistance && p_364426_ <= this.cameraSectionPos.x() + this.viewDistance
            ? p_363673_ >= this.cameraSectionPos.z() - this.viewDistance && p_363673_ <= this.cameraSectionPos.z() + this.viewDistance
            : false;
      } else {
         return false;
      }
   }
}
