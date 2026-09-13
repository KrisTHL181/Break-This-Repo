package net.minecraft.client.gui.components.debug;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugEntryBiome implements DebugScreenEntry {
   private static final Identifier GROUP = Identifier.withDefaultNamespace("biome");

   @Override
   public void display(
      final DebugScreenDisplayer displayer,
      final @Nullable Level serverOrClientLevel,
      final @Nullable LevelChunk clientChunk,
      final @Nullable LevelChunk serverChunk
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.getCameraEntity();
      if (entity != null && minecraft.level != null) {
         BlockPos feetPos = entity.blockPosition();
         if (minecraft.level.isInsideBuildHeight(feetPos.getY())) {
            if (SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES && serverOrClientLevel instanceof ServerLevel) {
               displayer.addToGroup(
                  GROUP,
                  List.of("Biome: " + printBiome(minecraft.level.getBiome(feetPos)), "Server Biome: " + printBiome(serverOrClientLevel.getBiome(feetPos)))
               );
            } else {
               displayer.addLine("Biome: " + printBiome(minecraft.level.getBiome(feetPos)));
            }
         }
      }
   }

   private static String printBiome(final Holder<Biome> biome) {
      return (String)biome.unwrap().map(key -> key.identifier().toString(), l -> "[unregistered " + l + "]");
   }
}
