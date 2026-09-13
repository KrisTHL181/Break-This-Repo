package net.minecraft.client.gui.components.debug;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryGpuUtilization implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_428277_, @Nullable Level p_427847_, @Nullable LevelChunk p_430901_, @Nullable LevelChunk p_429239_) {
      Minecraft minecraft = Minecraft.getInstance();
      String s = "GPU: " + (minecraft.getGpuUtilization() > 100.0 ? ChatFormatting.RED + "100%" : Math.round(minecraft.getGpuUtilization()) + "%");
      p_428277_.addLine(s);
   }

   @Override
   public boolean isAllowed(boolean p_427996_) {
      return true;
   }
}
