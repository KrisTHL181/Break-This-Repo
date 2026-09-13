package net.minecraft.server.level;



import java.util.ArrayList;
import net.minecraft.world.level.ChunkPos;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class ChunkLoadCounter {
    private final List<ChunkHolder> pendingChunks = new ArrayList<>();
    private int totalChunks;

    public void track(final ServerLevel level, final Runnable scheduler) {
        ServerChunkCache chunkSource = level.getChunkSource();
        Set<ChunkPos> alreadyLoadedChunks = new HashSet<>();
        chunkSource.runDistanceManagerUpdates();
        chunkSource.chunkMap.allChunksWithAtLeastStatus(ChunkStatus.FULL).forEach(chunkHolder -> alreadyLoadedChunks.add(chunkHolder.getPos()));
        scheduler.run();
        chunkSource.runDistanceManagerUpdates();
        chunkSource.chunkMap.allChunksWithAtLeastStatus(ChunkStatus.FULL).forEach(chunkHolder -> {
            if (!alreadyLoadedChunks.contains(chunkHolder.getPos())) {
                this.pendingChunks.add(chunkHolder);
                this.totalChunks++;
            }
        });
    }

    public int readyChunks() {
        return this.totalChunks - this.pendingChunks();
    }

    public int pendingChunks() {
        this.pendingChunks.removeIf(chunkHolder -> chunkHolder.getLatestStatus() == ChunkStatus.FULL);
        return this.pendingChunks.size();
    }

    public int totalChunks() {
        return this.totalChunks;
    }
}