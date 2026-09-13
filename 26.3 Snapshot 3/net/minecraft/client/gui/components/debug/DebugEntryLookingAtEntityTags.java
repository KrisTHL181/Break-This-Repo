package net.minecraft.client.gui.components.debug;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugEntryLookingAtEntityTags implements DebugScreenEntry {
   @Override
   public void display(
      final DebugScreenDisplayer displayer,
      final @Nullable Level serverOrClientLevel,
      final @Nullable LevelChunk clientChunk,
      final @Nullable LevelChunk serverChunk
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.crosshairPickEntity;
      List<String> result = new ArrayList<>();
      if (entity != null) {
         DebugEntryLookingAt.addTagEntries(result, entity);
      }

      displayer.addToGroup(DebugEntryLookingAtEntity.GROUP, result);
   }
}
