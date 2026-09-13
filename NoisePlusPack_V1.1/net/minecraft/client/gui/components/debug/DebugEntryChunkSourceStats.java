package net.minecraft.client.gui.components.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryChunkSourceStats implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_423524_, @Nullable Level p_428857_, @Nullable LevelChunk p_430753_, @Nullable LevelChunk p_425006_) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != null) {
         p_423524_.addLine(minecraft.level.gatherChunkSourceStats());
      }

      if (p_428857_ != null && p_428857_ != minecraft.level) {
         p_423524_.addLine(p_428857_.gatherChunkSourceStats());
      }
   }

   @Override
   public boolean isAllowed(boolean p_431204_) {
      return true;
   }
}
