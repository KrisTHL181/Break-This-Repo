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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryLight implements DebugScreenEntry {
   public static final Identifier GROUP = Identifier.withDefaultNamespace("light");

   @Override
   public void display(DebugScreenDisplayer p_430626_, @Nullable Level p_422694_, @Nullable LevelChunk p_423987_, @Nullable LevelChunk p_427456_) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.getCameraEntity();
      if (entity != null && minecraft.level != null) {
         BlockPos blockpos = entity.blockPosition();
         int i = minecraft.level.getChunkSource().getLightEngine().getRawBrightness(blockpos, 0);
         int j = minecraft.level.getBrightness(LightLayer.SKY, blockpos);
         int k = minecraft.level.getBrightness(LightLayer.BLOCK, blockpos);
         String s = "Client Light: " + i + " (" + j + " sky, " + k + " block)";
         if (SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES) {
            String s1;
            if (p_427456_ != null) {
               LevelLightEngine levellightengine = p_427456_.getLevel().getLightEngine();
               s1 = "Server Light: ("
                  + levellightengine.getLayerListener(LightLayer.SKY).getLightValue(blockpos)
                  + " sky, "
                  + levellightengine.getLayerListener(LightLayer.BLOCK).getLightValue(blockpos)
                  + " block)";
            } else {
               s1 = "Server Light: (?? sky, ?? block)";
            }

            p_430626_.addToGroup(GROUP, List.of(s, s1));
         } else {
            p_430626_.addToGroup(GROUP, s);
         }
      }
   }
}
