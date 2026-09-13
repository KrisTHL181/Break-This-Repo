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
    public void display(
        final DebugScreenDisplayer displayer,
        final @Nullable Level serverOrClientLevel,
        final @Nullable LevelChunk clientChunk,
        final @Nullable LevelChunk serverChunk
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        ServerLevel serverLevel = serverOrClientLevel instanceof ServerLevel level ? level : null;
        if (entity != null && serverLevel != null) {
            ServerChunkCache chunkSource = serverLevel.getChunkSource();
            NaturalSpawner.SpawnState lastSpawnState = chunkSource.getLastSpawnState();
            if (lastSpawnState != null) {
                Object2IntMap<MobCategory> mobCategoryCounts = lastSpawnState.getMobCategoryCounts();
                int chunkCount = lastSpawnState.getSpawnableChunkCount();
                displayer.addLine(
                    "SC: "
                        + chunkCount
                        + ", "
                        + Stream.of(MobCategory.values())
                            .map(c -> c.getDebugAbbreviation() + ": " + mobCategoryCounts.getInt(c))
                            .collect(Collectors.joining(", "))
                );
            }
        }
    }
}