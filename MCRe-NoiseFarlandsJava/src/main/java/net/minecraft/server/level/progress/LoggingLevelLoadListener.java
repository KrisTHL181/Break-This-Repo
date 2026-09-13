package net.minecraft.server.level.progress;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class LoggingLevelLoadListener implements LevelLoadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final boolean includePlayerChunks;
    private final LevelLoadProgressTracker progressTracker;
    private boolean closed;
    private long startTime = Long.MAX_VALUE;
    private long nextLogTime = Long.MAX_VALUE;

    public LoggingLevelLoadListener(final boolean includePlayerChunks) {
        this.includePlayerChunks = includePlayerChunks;
        this.progressTracker = new LevelLoadProgressTracker(includePlayerChunks);
    }

    public static LoggingLevelLoadListener forDedicatedServer() {
        return new LoggingLevelLoadListener(false);
    }

    public static LoggingLevelLoadListener forSingleplayer() {
        return new LoggingLevelLoadListener(true);
    }

    @Override
    public void start(final LevelLoadListener.Stage stage, final int totalChunks) {
        if (!this.closed) {
            if (this.startTime == Long.MAX_VALUE) {
                long now = Util.getMillis();
                this.startTime = now;
                this.nextLogTime = now;
            }

            this.progressTracker.start(stage, totalChunks);
            switch (stage) {
                case PREPARE_GLOBAL_SPAWN:
                    LOGGER.info("Selecting global world spawn...");
                    break;
                case LOAD_INITIAL_CHUNKS:
                    LOGGER.info("Loading {} persistent chunks...", totalChunks);
                    break;
                case LOAD_PLAYER_CHUNKS:
                    LOGGER.info("Loading {} chunks for player spawn...", totalChunks);
            }
        }
    }

    @Override
    public void update(final LevelLoadListener.Stage stage, final int currentChunks, final int totalChunks) {
        if (!this.closed) {
            this.progressTracker.update(stage, currentChunks, totalChunks);
            if (Util.getMillis() > this.nextLogTime) {
                this.nextLogTime += 500L;
                int percent = Mth.floor(this.progressTracker.get() * 100.0F);
                LOGGER.info(Component.translatable("menu.preparingSpawn", percent).getString());
            }
        }
    }

    @Override
    public void finish(final LevelLoadListener.Stage stage) {
        if (!this.closed) {
            this.progressTracker.finish(stage);
            LevelLoadListener.Stage finalStage = this.includePlayerChunks
                ? LevelLoadListener.Stage.LOAD_PLAYER_CHUNKS
                : LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS;
            if (stage == finalStage) {
                LOGGER.info("Time elapsed: {} ms", Util.getMillis() - this.startTime);
                this.nextLogTime = Long.MAX_VALUE;
                this.closed = true;
            }
        }
    }

    @Override
    public void updateFocus(final ResourceKey<Level> dimension, final ChunkPos chunkPos) {
    }
}