package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntrySectionPosition implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_427834_, @Nullable Level p_426991_, @Nullable LevelChunk p_426448_, @Nullable LevelChunk p_429370_) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.getCameraEntity();
      if (entity != null) {
         BlockPos blockpos = minecraft.getCameraEntity().blockPosition();
         p_427834_.addToGroup(
            DebugEntryPosition.GROUP,
            String.format(Locale.ROOT, "Section-relative: %02d %02d %02d", blockpos.getX() & 15, blockpos.getY() & 15, blockpos.getZ() & 15)
         );
      }
   }

   @Override
   public boolean isAllowed(boolean p_430276_) {
      return true;
   }
}
