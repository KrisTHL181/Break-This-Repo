package net.minecraft.client.gui.components.debug;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugEntryLookingAtEntity implements DebugScreenEntry {
   public static final Identifier GROUP = Identifier.withDefaultNamespace("looking_at_entity");

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
         result.add(ChatFormatting.UNDERLINE + "Targeted Entity");
         result.add(entity.typeHolder().getRegisteredName());
      }

      displayer.addToGroup(GROUP, result);
   }
}
