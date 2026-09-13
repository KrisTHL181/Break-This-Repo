package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Locale;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum ChunkSectionLayer {
   SOLID(RenderPipelines.SOLID_TERRAIN, 4194304, false),
   CUTOUT(RenderPipelines.CUTOUT_TERRAIN, 4194304, false),
   TRANSLUCENT(RenderPipelines.TRANSLUCENT_TERRAIN, 786432, true),
   TRIPWIRE(RenderPipelines.TRIPWIRE_TERRAIN, 1536, true);

   private final RenderPipeline pipeline;
   private final int bufferSize;
   private final boolean sortOnUpload;
   private final String label;

   ChunkSectionLayer(final RenderPipeline p_409365_, final int p_407735_, final boolean p_406897_) {
      this.pipeline = p_409365_;
      this.bufferSize = p_407735_;
      this.sortOnUpload = p_406897_;
      this.label = this.toString().toLowerCase(Locale.ROOT);
   }

   public RenderPipeline pipeline() {
      return this.pipeline;
   }

   public int bufferSize() {
      return this.bufferSize;
   }

   public String label() {
      return this.label;
   }

   public boolean sortOnUpload() {
      return this.sortOnUpload;
   }
}
