package net.minecraft.client.gui.components.debug;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugEntryPostEffects implements DebugScreenEntry {
   @Override
   public void display(
      final DebugScreenDisplayer displayer,
      final @Nullable Level serverOrClientLevel,
      final @Nullable LevelChunk clientChunk,
      final @Nullable LevelChunk serverChunk
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      List<Identifier> effectIds = minecraft.gameRenderer.getAppliedPostEffects();
      if (!effectIds.isEmpty()) {
         displayer.addLine("Post: " + effectIds.stream().map(Identifier::toString).collect(Collectors.joining(", ")));
      }
   }
}
