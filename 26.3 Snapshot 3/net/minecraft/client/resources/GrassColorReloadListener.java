package net.minecraft.client.resources;

import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.GrassColor;

public class GrassColorReloadListener extends SimplePreparableReloadListener<int[]> {
   private static final Identifier LOCATION = Identifier.withDefaultNamespace("textures/colormap/grass.png");

   protected int[] prepare(final ResourceManager manager, final ProfilerFiller profiler) {
      try {
         return LegacyStuffWrapper.getPixels(manager, LOCATION);
      } catch (IOException e) {
         throw new IllegalStateException("Failed to load grass color texture", e);
      }
   }

   protected void apply(final int[] pixels, final ResourceManager manager, final ProfilerFiller profiler) {
      GrassColor.init(pixels);
   }
}
