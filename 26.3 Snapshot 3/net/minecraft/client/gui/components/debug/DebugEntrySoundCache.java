package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugEntrySoundCache implements DebugScreenEntry {
   @Override
   public boolean isAllowed(final boolean reducedDebugInfo) {
      return true;
   }

   @Override
   public void display(
      final DebugScreenDisplayer displayer,
      final @Nullable Level serverOrClientLevel,
      final @Nullable LevelChunk clientChunk,
      final @Nullable LevelChunk serverChunk
   ) {
      SoundBufferLibrary.DebugOutput.Counter counter = new SoundBufferLibrary.DebugOutput.Counter();
      Minecraft.getInstance().getSoundManager().getSoundCacheDebugStats(counter);
      displayer.addLine(String.format(Locale.ROOT, "Sound cache: %d buffers, %d MiB", counter.totalCount(), bytesToMegabytes(counter.totalSize())));
   }

   private static long bytesToMegabytes(final long used) {
      return Mth.ceilLong(used / 1024.0 / 1024.0);
   }
}
