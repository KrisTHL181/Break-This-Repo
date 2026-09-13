package net.minecraft.client.gui.components.debug;

import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
class DebugEntryVersion implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_429141_, @Nullable Level p_424693_, @Nullable LevelChunk p_428094_, @Nullable LevelChunk p_428934_) {
      p_429141_.addPriorityLine(
         "Minecraft "
            + SharedConstants.getCurrentVersion().name()
            + " ("
            + Minecraft.getInstance().getLaunchedVersion()
            + "/"
            + ClientBrandRetriever.getClientModName()
            + ")"
      );
   }

   @Override
   public boolean isAllowed(boolean p_427225_) {
      return true;
   }
}
