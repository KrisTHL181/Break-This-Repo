package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryFps implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_430292_, @Nullable Level p_426610_, @Nullable LevelChunk p_428846_, @Nullable LevelChunk p_430980_) {
      Minecraft minecraft = Minecraft.getInstance();
      int i = minecraft.getFramerateLimitTracker().getFramerateLimit();
      Options options = minecraft.options;
      p_430292_.addPriorityLine(
         String.format(Locale.ROOT, "%d fps T: %s%s", minecraft.getFps(), i == 260 ? "inf" : i, options.enableVsync().get() ? " vsync" : "")
      );
   }

   @Override
   public boolean isAllowed(boolean p_428450_) {
      return true;
   }
}
