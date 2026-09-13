package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntrySoundMood implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_426392_, @Nullable Level p_425339_, @Nullable LevelChunk p_425787_, @Nullable LevelChunk p_422662_) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null) {
         p_426392_.addLine(
            minecraft.getSoundManager().getDebugString() + String.format(Locale.ROOT, " (Mood %d%%)", Math.round(minecraft.player.getCurrentMood() * 100.0F))
         );
      }
   }
}
