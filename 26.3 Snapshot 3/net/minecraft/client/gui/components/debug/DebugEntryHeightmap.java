package net.minecraft.client.gui.components.debug;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

public class DebugEntryHeightmap implements DebugScreenEntry {
   private static final Map<Heightmap.Types, String> HEIGHTMAP_NAMES = Maps.newEnumMap(
      Map.of(
         Heightmap.Types.WORLD_SURFACE_WG,
         "SW",
         Heightmap.Types.WORLD_SURFACE,
         "S",
         Heightmap.Types.OCEAN_FLOOR_WG,
         "OW",
         Heightmap.Types.OCEAN_FLOOR,
         "O",
         Heightmap.Types.MOTION_BLOCKING,
         "M",
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
         "ML"
      )
   );
   private static final Identifier GROUP = Identifier.withDefaultNamespace("heightmaps");

   @Override
   public void display(
      final DebugScreenDisplayer displayer,
      final @Nullable Level serverOrClientLevel,
      final @Nullable LevelChunk clientChunk,
      final @Nullable LevelChunk serverChunk
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.getCameraEntity();
      if (entity != null && minecraft.level != null && clientChunk != null) {
         BlockPos feetPos = entity.blockPosition();
         List<String> result = new ArrayList<>();
         StringBuilder heightmaps = new StringBuilder("CH");

         for (Heightmap.Types type : Heightmap.Types.values()) {
            if (type.sendToClient()) {
               heightmaps.append(" ").append(HEIGHTMAP_NAMES.get(type)).append(": ").append(clientChunk.getHeight(type, feetPos.getX(), feetPos.getZ()));
            }
         }

         result.add(heightmaps.toString());
         heightmaps.setLength(0);
         heightmaps.append("SH");

         for (Heightmap.Types type : Heightmap.Types.values()) {
            if (type.keepAfterWorldgen()) {
               heightmaps.append(" ").append(HEIGHTMAP_NAMES.get(type)).append(": ");
               if (serverChunk != null) {
                  heightmaps.append(serverChunk.getHeight(type, feetPos.getX(), feetPos.getZ()));
               } else {
                  heightmaps.append("??");
               }
            }
         }

         result.add(heightmaps.toString());
         displayer.addToGroup(GROUP, result);
      }
   }
}
