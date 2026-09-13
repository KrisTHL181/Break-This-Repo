package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntrySimplePerformanceImpactors implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_428126_, @Nullable Level p_422748_, @Nullable LevelChunk p_424513_, @Nullable LevelChunk p_425623_) {
      Minecraft minecraft = Minecraft.getInstance();
      Options options = minecraft.options;
      p_428126_.addLine(
         String.format(
            Locale.ROOT,
            "%s%s B: %d",
            options.improvedTransparency().get() ? "improved-transparency" : "",
            options.cloudStatus().get() == CloudStatus.OFF ? "" : (options.cloudStatus().get() == CloudStatus.FAST ? " fast-clouds" : " fancy-clouds"),
            options.biomeBlendRadius().get()
         )
      );
      TextureFilteringMethod texturefilteringmethod = options.textureFiltering().get();
      if (texturefilteringmethod == TextureFilteringMethod.ANISOTROPIC) {
         p_428126_.addLine(String.format(Locale.ROOT, "Filtering: %s %dx", texturefilteringmethod.caption().getString(), options.maxAnisotropyValue()));
      } else {
         p_428126_.addLine(String.format(Locale.ROOT, "Filtering: %s", texturefilteringmethod.caption().getString()));
      }
   }

   @Override
   public boolean isAllowed(boolean p_424802_) {
      return true;
   }
}
