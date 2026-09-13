package net.minecraft.client.resources;

import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.DryFoliageColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DryFoliageColorReloadListener extends SimplePreparableReloadListener<int[]> {
   private static final Identifier LOCATION = Identifier.withDefaultNamespace("textures/colormap/dry_foliage.png");

   protected int[] prepare(ResourceManager p_395810_, ProfilerFiller p_395589_) {
      try {
         return LegacyStuffWrapper.getPixels(p_395810_, LOCATION);
      } catch (IOException ioexception) {
         throw new IllegalStateException("Failed to load dry foliage color texture", ioexception);
      }
   }

   protected void apply(int[] p_393397_, ResourceManager p_394980_, ProfilerFiller p_394597_) {
      DryFoliageColor.init(p_393397_);
   }
}
