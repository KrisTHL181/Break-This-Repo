package net.minecraft.server.level;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;

/**
 * SimulationChunkTracker — 模拟区块追踪器（MCRe NoiseFarlands 对象化版）
 * 原版以 long 打包键（ChunkPos.pack），本版以 ChunkPos 对象为键。
 */
public class SimulationChunkTracker extends ChunkTracker {
    public static final int MAX_LEVEL = 33;
    protected final Map<ChunkPos, Byte> chunks = new HashMap<>();
    private final TicketStorage ticketStorage;

    public SimulationChunkTracker(final TicketStorage ticketStorage) {
        super(34, 16, 256);
        this.ticketStorage = ticketStorage;
        ticketStorage.setSimulationChunkUpdatedListener(this::update);
    }

    @Override
    protected int getLevelFromSource(final ChunkPos to) {
        return this.ticketStorage.getTicketLevelAt(to, true);
    }

    @Override
    public int getLevel(final ChunkPos node) {
        return this.chunks.getOrDefault(node, (byte)33);
    }

    @Override
    protected void setLevel(final ChunkPos node, final int level) {
        if (level >= 33) {
            this.chunks.remove(node);
        } else {
            this.chunks.put(node, (byte)level);
        }
    }

    public void runAllUpdates() {
        this.runUpdates(Integer.MAX_VALUE);
    }
}
