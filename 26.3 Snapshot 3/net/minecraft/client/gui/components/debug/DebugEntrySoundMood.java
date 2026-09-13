package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugEntrySoundMood implements DebugScreenEntry {
   @Override
   public void display(
      final DebugScreenDisplayer displayer,
      final @Nullable Level serverOrClientLevel,
      final @Nullable LevelChunk clientChunk,
      final @Nullable LevelChunk serverChunk
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null) {
         displayer.addLine(
            minecraft.getSoundManager().getChannelDebugString()
               + String.format(Locale.ROOT, " (Mood %d%%)", Math.round(minecraft.player.getCurrentMood() * 100.0F))
         );
      }
   }
}
