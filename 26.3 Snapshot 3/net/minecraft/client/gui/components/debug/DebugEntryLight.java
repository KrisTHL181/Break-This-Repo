package net.minecraft.client.gui.components.debug;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

public class DebugEntryLight implements DebugScreenEntry {
   public static final Identifier GROUP = Identifier.withDefaultNamespace("light");

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
         int rawBrightness = minecraft.level.getChunkSource().getLightEngine().getRawBrightness(feetPos, 0);
         int sky = minecraft.level.getBrightness(LightLayer.SKY, feetPos);
         int block = minecraft.level.getBrightness(LightLayer.BLOCK, feetPos);
         String clientLight = "Client Light: " + rawBrightness + " (" + sky + " sky, " + block + " block)";
         if (SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES) {
            String serverLight;
            if (serverChunk != null) {
               LevelLightEngine lightEngine = serverChunk.getLevel().getLightEngine();
               serverLight = "Server Light: ("
                  + lightEngine.getLayerListener(LightLayer.SKY).getLightValue(feetPos)
                  + " sky, "
                  + lightEngine.getLayerListener(LightLayer.BLOCK).getLightValue(feetPos)
                  + " block)";
            } else {
               serverLight = "Server Light: (?? sky, ?? block)";
            }

            displayer.addToGroup(GROUP, List.of(clientLight, serverLight));
         } else {
            displayer.addToGroup(GROUP, clientLight);
         }
      }
   }
}
