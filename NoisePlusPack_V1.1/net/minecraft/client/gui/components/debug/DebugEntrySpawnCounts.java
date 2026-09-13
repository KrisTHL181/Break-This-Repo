package net.minecraft.client.gui.components.debug;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntrySpawnCounts implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer p_424068_, @Nullable Level p_425991_, @Nullable LevelChunk p_429361_, @Nullable LevelChunk p_427551_) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.getCameraEntity();
      ServerLevel serverlevel = p_425991_ instanceof ServerLevel ? (ServerLevel)p_425991_ : null;
      if (entity != null && serverlevel != null) {
         ServerChunkCache serverchunkcache = serverlevel.getChunkSource();
         NaturalSpawner.SpawnState naturalspawner$spawnstate = serverchunkcache.getLastSpawnState();
         if (naturalspawner$spawnstate != null) {
            Object2IntMap<MobCategory> object2intmap = naturalspawner$spawnstate.getMobCategoryCounts();
            int i = naturalspawner$spawnstate.getSpawnableChunkCount();
            p_424068_.addLine(
               "SC: "
                  + i
                  + ", "
                  + Stream.of(MobCategory.values())
                     .map(p_423442_ -> Character.toUpperCase(p_423442_.getName().charAt(0)) + ": " + object2intmap.getInt(p_423442_))
                     .collect(Collectors.joining(", "))
            );
         }
      }
   }
}
