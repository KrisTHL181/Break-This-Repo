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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryBiome implements DebugScreenEntry {
   private static final Identifier GROUP = Identifier.withDefaultNamespace("biome");

   @Override
   public void display(DebugScreenDisplayer p_423807_, @Nullable Level p_427289_, @Nullable LevelChunk p_425713_, @Nullable LevelChunk p_426046_) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.getCameraEntity();
      if (entity != null && minecraft.level != null) {
         BlockPos blockpos = entity.blockPosition();
         if (minecraft.level.isInsideBuildHeight(blockpos.getY())) {
            if (SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES && p_427289_ instanceof ServerLevel) {
               p_423807_.addToGroup(
                  GROUP, List.of("Biome: " + printBiome(minecraft.level.getBiome(blockpos)), "Server Biome: " + printBiome(p_427289_.getBiome(blockpos)))
               );
            } else {
               p_423807_.addLine("Biome: " + printBiome(minecraft.level.getBiome(blockpos)));
            }
         }
      }
   }

   private static String printBiome(Holder<Biome> p_422520_) {
      return (String)p_422520_.unwrap().map(p_447977_ -> p_447977_.identifier().toString(), p_424191_ -> "[unregistered " + p_424191_ + "]");
   }
}
