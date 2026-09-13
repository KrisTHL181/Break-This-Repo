package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.level.progress.LevelLoadProgressTracker;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class LevelLoadTracker implements LevelLoadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final long CLIENT_WAIT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30L);
   public static final long LEVEL_LOAD_CLOSE_DELAY_MS = 500L;
   private final LevelLoadProgressTracker serverProgressTracker = new LevelLoadProgressTracker(true);
   private @Nullable ChunkLoadStatusView serverChunkStatusView;
   private volatile LevelLoadListener.@Nullable Stage serverStage;
   private LevelLoadTracker.@Nullable ClientState clientState;
   private final long closeDelayMs;

   public LevelLoadTracker() {
      this(0L);
   }

   public LevelLoadTracker(final long closeDelayMs) {
      this.closeDelayMs = closeDelayMs;
   }

   public void setServerChunkStatusView(final ChunkLoadStatusView serverChunkStatusView) {
      this.serverChunkStatusView = serverChunkStatusView;
   }

   public void startClientLoad(final LocalPlayer player, final ClientLevel level) {
      this.clientState = new LevelLoadTracker.WaitingForServer(player, level, Util.getMillis() + CLIENT_WAIT_TIMEOUT_MS);
   }

   public void tickClientLoad() {
      if (this.clientState != null) {
         this.clientState = this.clientState.tick();
      }
   }

   public boolean isLevelReady() {
      return this.clientState instanceof LevelLoadTracker.ClientLevelReady(long readyAt) && Util.getMillis() >= readyAt + this.closeDelayMs;
   }

   public void loadingPacketsReceived() {
      if (this.clientState != null) {
         this.clientState = this.clientState.loadingPacketsReceived();
      }
   }

   @Override
   public void start(final LevelLoadListener.Stage stage, final int totalChunks) {
      this.serverProgressTracker.start(stage, totalChunks);
      this.serverStage = stage;
   }

   @Override
   public void update(final LevelLoadListener.Stage stage, final int currentChunks, final int totalChunks) {
      this.serverProgressTracker.update(stage, currentChunks, totalChunks);
   }

   @Override
   public void finish(final LevelLoadListener.Stage stage) {
      this.serverProgressTracker.finish(stage);
   }

   @Override
   public void updateFocus(final ResourceKey<Level> dimension, final ChunkPos chunkPos) {
      if (this.serverChunkStatusView != null) {
         this.serverChunkStatusView.moveTo(dimension, chunkPos);
      }
   }

   public @Nullable ChunkLoadStatusView statusView() {
      return this.serverChunkStatusView;
   }

   public float serverProgress() {
      return this.serverProgressTracker.get();
   }

   public boolean hasProgress() {
      return this.serverStage != null;
   }

   public @Nullable Runnable getPlayerCompiledSectionCallback() {
      return this.clientState instanceof LevelLoadTracker.WaitingForPlayerChunk waitingForPlayerChunk
         ? () -> waitingForPlayerChunk.playerSectionReady().set(true)
         : null;
   }

   private record ClientLevelReady(long readyAt) implements LevelLoadTracker.ClientState {
   }

   private sealed interface ClientState permits LevelLoadTracker.ClientLevelReady, LevelLoadTracker.WaitingForPlayerChunk, LevelLoadTracker.WaitingForServer {
      default LevelLoadTracker.ClientState tick() {
         return this;
      }

      default LevelLoadTracker.ClientState loadingPacketsReceived() {
         return this;
      }
   }

   private record WaitingForPlayerChunk(LocalPlayer player, ClientLevel level, AtomicBoolean playerSectionReady, long timeoutAfter)
      implements LevelLoadTracker.ClientState {
      @Override
      public LevelLoadTracker.ClientState tick() {
         return this.isReady() ? new LevelLoadTracker.ClientLevelReady(Util.getMillis()) : this;
      }

      private boolean isReady() {
         if (Util.getMillis() > this.timeoutAfter) {
            LevelLoadTracker.LOGGER.warn("Timed out while waiting for the client to load chunks, letting the player into the world anyway");
            return true;
         } else {
            BlockPos cameraPos = Minecraft.getInstance().gameRenderer.mainCamera().blockPosition();
            return !this.level.isOutsideBuildHeight(cameraPos.getY()) && !this.player.isSpectator() && this.player.isAlive()
               ? this.playerSectionReady.get()
               : true;
         }
      }
   }

   private record WaitingForServer(LocalPlayer player, ClientLevel level, long timeoutAfter) implements LevelLoadTracker.ClientState {
      @Override
      public LevelLoadTracker.ClientState loadingPacketsReceived() {
         return new LevelLoadTracker.WaitingForPlayerChunk(this.player, this.level, new AtomicBoolean(), this.timeoutAfter);
      }
   }
}
