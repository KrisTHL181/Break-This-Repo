package net.minecraft.client.resources;

import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.DryFoliageColor;

public class DryFoliageColorReloadListener extends SimplePreparableReloadListener<int[]> {
   private static final Identifier LOCATION = Identifier.withDefaultNamespace("textures/colormap/dry_foliage.png");

   protected int[] prepare(final ResourceManager manager, final ProfilerFiller profiler) {
      try {
         return LegacyStuffWrapper.getPixels(manager, LOCATION);
      } catch (IOException e) {
         throw new IllegalStateException("Failed to load dry foliage color texture", e);
      }
   }

   protected void apply(final int[] pixels, final ResourceManager manager, final ProfilerFiller profiler) {
      DryFoliageColor.init(pixels);
   }
}
