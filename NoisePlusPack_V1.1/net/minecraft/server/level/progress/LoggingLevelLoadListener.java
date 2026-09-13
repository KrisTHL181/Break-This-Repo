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

   public LoggingLevelLoadListener(boolean p_429386_) {
      this.includePlayerChunks = p_429386_;
      this.progressTracker = new LevelLoadProgressTracker(p_429386_);
   }

   public static LoggingLevelLoadListener forDedicatedServer() {
      return new LoggingLevelLoadListener(false);
   }

   public static LoggingLevelLoadListener forSingleplayer() {
      return new LoggingLevelLoadListener(true);
   }

   @Override
   public void start(LevelLoadListener.Stage p_425242_, int p_430051_) {
      if (!this.closed) {
         if (this.startTime == Long.MAX_VALUE) {
            long i = Util.getMillis();
            this.startTime = i;
            this.nextLogTime = i;
         }

         this.progressTracker.start(p_425242_, p_430051_);
         switch (p_425242_) {
            case PREPARE_GLOBAL_SPAWN:
               LOGGER.info("Selecting global world spawn...");
               break;
            case LOAD_INITIAL_CHUNKS:
               LOGGER.info("Loading {} persistent chunks...", p_430051_);
               break;
            case LOAD_PLAYER_CHUNKS:
               LOGGER.info("Loading {} chunks for player spawn...", p_430051_);
         }
      }
   }

   @Override
   public void update(LevelLoadListener.Stage p_429786_, int p_428210_, int p_423527_) {
      if (!this.closed) {
         this.progressTracker.update(p_429786_, p_428210_, p_423527_);
         if (Util.getMillis() > this.nextLogTime) {
            this.nextLogTime += 500L;
            int i = Mth.floor(this.progressTracker.get() * 100.0F);
            LOGGER.info(Component.translatable("menu.preparingSpawn", i).getString());
         }
      }
   }

   @Override
   public void finish(LevelLoadListener.Stage p_427045_) {
      if (!this.closed) {
         this.progressTracker.finish(p_427045_);
         LevelLoadListener.Stage levelloadlistener$stage = this.includePlayerChunks
            ? LevelLoadListener.Stage.LOAD_PLAYER_CHUNKS
            : LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS;
         if (p_427045_ == levelloadlistener$stage) {
            LOGGER.info("Time elapsed: {} ms", Util.getMillis() - this.startTime);
            this.nextLogTime = Long.MAX_VALUE;
            this.closed = true;
         }
      }
   }

   @Override
   public void updateFocus(ResourceKey<Level> p_431169_, ChunkPos p_424587_) {
   }
}
