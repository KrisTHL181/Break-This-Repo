package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderRegionCache {
   private final Long2ObjectMap<SectionCopy> sectionCopyCache = new Long2ObjectOpenHashMap();

   public RenderSectionRegion createRegion(Level p_200466_, long p_410217_) {
      int i = SectionPos.x(p_410217_);
      int j = SectionPos.y(p_410217_);
      int k = SectionPos.z(p_410217_);
      int l = i - 1;
      int i1 = j - 1;
      int j1 = k - 1;
      int k1 = i + 1;
      int l1 = j + 1;
      int i2 = k + 1;
      SectionCopy[] asectioncopy = new SectionCopy[27];

      for (int j2 = j1; j2 <= i2; j2++) {
         for (int k2 = i1; k2 <= l1; k2++) {
            for (int l2 = l; l2 <= k1; l2++) {
               int i3 = RenderSectionRegion.index(l, i1, j1, l2, k2, j2);
               asectioncopy[i3] = this.getSectionDataCopy(p_200466_, l2, k2, j2);
            }
         }
      }

      return new RenderSectionRegion(p_200466_, l, i1, j1, asectioncopy);
   }

   private SectionCopy getSectionDataCopy(Level p_410643_, int p_409548_, int p_408944_, int p_406133_) {
      return (SectionCopy)this.sectionCopyCache.computeIfAbsent(SectionPos.asLong(p_409548_, p_408944_, p_406133_), p_404989_ -> {
         LevelChunk levelchunk = p_410643_.getChunk(p_409548_, p_406133_);
         return new SectionCopy(levelchunk, levelchunk.getSectionIndexFromSectionY(p_408944_));
      });
   }
}
