package net.minecraft.client.gui.components.debug;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryChunkGeneration implements DebugScreenEntry {
   private static final Identifier GROUP = Identifier.withDefaultNamespace("chunk_generation");

   @Override
   public void display(DebugScreenDisplayer p_430411_, @Nullable Level p_426967_, @Nullable LevelChunk p_429967_, @Nullable LevelChunk p_423873_) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.getCameraEntity();
      ServerLevel serverlevel = p_426967_ instanceof ServerLevel ? (ServerLevel)p_426967_ : null;
      if (entity != null && serverlevel != null) {
         BlockPos blockpos = entity.blockPosition();
         ServerChunkCache serverchunkcache = serverlevel.getChunkSource();
         List<String> list = new ArrayList<>();
         ChunkGenerator chunkgenerator = serverchunkcache.getGenerator();
         RandomState randomstate = serverchunkcache.randomState();
         chunkgenerator.addDebugScreenInfo(list, randomstate, blockpos);
         Climate.Sampler climate$sampler = randomstate.sampler();
         BiomeSource biomesource = chunkgenerator.getBiomeSource();
         biomesource.addDebugInfo(list, blockpos, climate$sampler);
         if (p_423873_ != null && p_423873_.isOldNoiseGeneration()) {
            list.add("Blending: Old");
         }

         p_430411_.addToGroup(GROUP, list);
      }
   }
}
