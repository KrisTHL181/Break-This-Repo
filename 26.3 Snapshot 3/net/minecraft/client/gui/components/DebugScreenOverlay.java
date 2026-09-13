package net.minecraft.client.gui.components;

import com.google.common.base.Strings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debugchart.BandwidthDebugChart;
import net.minecraft.client.gui.components.debugchart.FpsDebugChart;
import net.minecraft.client.gui.components.debugchart.PingDebugChart;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.client.gui.components.debugchart.TpsDebugChart;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.RemoteDebugSampleType;
import net.minecraft.util.debugchart.TpsDebugDimensions;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

public class DebugScreenOverlay {
   private static final int MARGIN_RIGHT = 2;
   private static final int MARGIN_LEFT = 2;
   private static final int MARGIN_TOP = 2;
   private final Minecraft minecraft;
   private final Font font;
   private @Nullable ChunkPos lastPos;
   private @Nullable LevelChunk clientChunk;
   private @Nullable CompletableFuture<LevelChunk> serverChunk;
   private boolean renderProfilerChart;
   private boolean renderFpsCharts;
   private boolean renderNetworkCharts;
   private boolean renderLightmapTexture;
   private final LocalSampleLogger frameTimeLogger = new LocalSampleLogger(1);
   private final LocalSampleLogger tickTimeLogger = new LocalSampleLogger(TpsDebugDimensions.values().length);
   private final LocalSampleLogger pingLogger = new LocalSampleLogger(1);
   private final LocalSampleLogger bandwidthLogger = new LocalSampleLogger(1);
   private final Map<RemoteDebugSampleType, LocalSampleLogger> remoteSupportingLoggers = Map.of(RemoteDebugSampleType.TICK_TIME, this.tickTimeLogger);
   private final FpsDebugChart fpsChart;
   private final TpsDebugChart tpsChart;
   private final PingDebugChart pingChart;
   private final BandwidthDebugChart bandwidthChart;
   private final ProfilerPieChart profilerPieChart;

   public DebugScreenOverlay(final Minecraft minecraft) {
      this.minecraft = minecraft;
      this.font = minecraft.font;
      this.fpsChart = new FpsDebugChart(this.font, this.frameTimeLogger);
      this.tpsChart = new TpsDebugChart(
         this.font, this.tickTimeLogger, () -> minecraft.level == null ? 0.0F : minecraft.level.tickRateManager().millisecondsPerTick()
      );
      this.pingChart = new PingDebugChart(this.font, this.pingLogger);
      this.bandwidthChart = new BandwidthDebugChart(this.font, this.bandwidthLogger);
      this.profilerPieChart = new ProfilerPieChart(this.font);
   }

   public void clearChunkCache() {
      this.serverChunk = null;
      this.clientChunk = null;
   }

   public void extractRenderState(final GuiGraphicsExtractor graphics) {
      Options options = this.minecraft.options;
      if (this.minecraft.isGameLoadFinished() && (!this.minecraft.gui.hud.isHidden() || this.minecraft.gui.screen() != null)) {
         Collection<Identifier> visibleEntries = this.minecraft.debugEntries.getCurrentlyEnabled();
         if (!visibleEntries.isEmpty()) {
            graphics.nextStratum();
            ProfilerFiller profiler = Profiler.get();
            profiler.push("debug");
            ChunkPos chunkPos;
            if (this.minecraft.getCameraEntity() != null && this.minecraft.level != null) {
               BlockPos feetPos = this.minecraft.getCameraEntity().blockPosition();
               chunkPos = ChunkPos.containing(feetPos);
            } else {
               chunkPos = null;
            }

            if (!Objects.equals(this.lastPos, chunkPos)) {
               this.lastPos = chunkPos;
               this.clearChunkCache();
            }

            final List<String> leftLines = new ArrayList<>();
            final List<String> rightLines = new ArrayList<>();
            final Map<Identifier, Collection<String>> groups = new LinkedHashMap<>();
            final List<String> regularLines = new ArrayList<>();
            DebugScreenDisplayer displayer = new DebugScreenDisplayer() {
               @Override
               public void addPriorityLine(final String line) {
                  if (leftLines.size() > rightLines.size()) {
                     rightLines.add(line);
                  } else {
                     leftLines.add(line);
                  }
               }

               @Override
               public void addLine(final String line) {
                  regularLines.add(line);
               }

               @Override
               public void addToGroup(final Identifier group, final Collection<String> lines) {
                  groups.computeIfAbsent(group, k -> new ArrayList<>()).addAll(lines);
               }

               @Override
               public void addToGroup(final Identifier group, final String lines) {
                  groups.computeIfAbsent(group, k -> new ArrayList<>()).add(lines);
               }
            };
            Level level = this.getLevel();

            for (Identifier id : visibleEntries) {
               DebugScreenEntry entry = DebugScreenEntries.getEntry(id);
               if (entry != null) {
                  entry.display(displayer, level, this.getClientChunk(), this.getServerChunk());
               }
            }

            if (!leftLines.isEmpty()) {
               leftLines.add("");
            }

            if (!rightLines.isEmpty()) {
               rightLines.add("");
            }

            if (!regularLines.isEmpty()) {
               int mid = (regularLines.size() + 1) / 2;
               leftLines.addAll(regularLines.subList(0, mid));
               rightLines.addAll(regularLines.subList(mid, regularLines.size()));
               leftLines.add("");
               if (mid < regularLines.size()) {
                  rightLines.add("");
               }
            }

            List<Collection<String>> finalGroups = new ArrayList<>(groups.values());
            if (!finalGroups.isEmpty()) {
               int mid = (finalGroups.size() + 1) / 2;

               for (int i = 0; i < finalGroups.size(); i++) {
                  Collection<String> lines = finalGroups.get(i);
                  if (!lines.isEmpty()) {
                     if (i < mid) {
                        leftLines.addAll(lines);
                        leftLines.add("");
                     } else {
                        rightLines.addAll(lines);
                        rightLines.add("");
                     }
                  }
               }
            }

            if (this.minecraft.debugEntries.isOverlayVisible()) {
               leftLines.add("");
               boolean hasServer = this.minecraft.getSingleplayerServer() != null;
               KeyMapping keyDebugModifier = options.keyDebugModifier;
               leftLines.add(
                  "Debug charts: "
                     + formatChart(keyDebugModifier, options.keyDebugPofilingChart, "Profiler", this.renderProfilerChart)
                     + "; "
                     + formatChart(keyDebugModifier, options.keyDebugFpsCharts, hasServer ? "fps + tps" : "fps", this.renderFpsCharts)
                     + ";"
               );
               leftLines.add(
                  formatChart(
                        keyDebugModifier,
                        options.keyDebugNetworkCharts,
                        !this.minecraft.isLocalServer() ? "Bandwidth + Ping" : "Ping",
                        this.renderNetworkCharts
                     )
                     + "; "
                     + formatChart(keyDebugModifier, options.keyDebugLightmapTexture, "Lightmap", this.renderLightmapTexture)
               );
               leftLines.add("To edit: press " + formatKeybind(keyDebugModifier, options.keyDebugDebugOptions));
            }

            this.extractLines(graphics, leftLines, true);
            this.extractLines(graphics, rightLines, false);
            graphics.nextStratum();
            this.profilerPieChart.setBottomOffset(10);
            if (this.showFpsCharts()) {
               int scaledWidth = graphics.guiWidth();
               int maxWidth = scaledWidth / 2;
               this.fpsChart.extractRenderState(graphics, 0, this.fpsChart.getWidth(maxWidth));
               if (this.tickTimeLogger.size() > 0) {
                  int width = this.tpsChart.getWidth(maxWidth);
                  this.tpsChart.extractRenderState(graphics, scaledWidth - width, width);
               }

               this.profilerPieChart.setBottomOffset(this.tpsChart.getFullHeight());
            }

            if (this.showNetworkCharts() && this.minecraft.getConnection() != null) {
               int scaledWidth = graphics.guiWidth();
               int maxWidth = scaledWidth / 2;
               if (!this.minecraft.isLocalServer()) {
                  this.bandwidthChart.extractRenderState(graphics, 0, this.bandwidthChart.getWidth(maxWidth));
               }

               int width = this.pingChart.getWidth(maxWidth);
               this.pingChart.extractRenderState(graphics, scaledWidth - width, width);
               this.profilerPieChart.setBottomOffset(this.pingChart.getFullHeight());
            }

            if (this.showLightmapTexture()) {
               GpuTextureView lightmapTextureView = this.minecraft.gameRenderer.levelLightmap();
               int displaySize = 64;
               int x = graphics.guiWidth() - 64 - 2;
               int y = graphics.guiHeight() - 64 - 2;
               graphics.fill(x - 1, y - 1, x + 64 + 1, y + 64 + 1, -16777216);
               graphics.blit(
                  lightmapTextureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST), x, y, x + 64, y + 64, 0.0F, 1.0F, 1.0F, 0.0F
               );
            }

            if (this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_CHUNKS_ON_SERVER)) {
               IntegratedServer singleplayerServer = this.minecraft.getSingleplayerServer();
               if (singleplayerServer != null && this.minecraft.player != null) {
                  ChunkLoadStatusView statusView = singleplayerServer.createChunkLoadStatusView(16 + ChunkLevel.RADIUS_AROUND_FULL_CHUNK);
                  statusView.moveTo(this.minecraft.player.level().dimension(), this.minecraft.player.chunkPosition());
                  LevelLoadingScreen.extractChunksForRendering(graphics, graphics.guiWidth() / 2, graphics.guiHeight() / 2, 4, 1, statusView);
               }
            }

            try (Zone ignored = profiler.zone("profilerPie")) {
               this.profilerPieChart.extractRenderState(graphics);
            }

            profiler.pop();
         }
      }
   }

   private static String formatChart(final KeyMapping keyDebugModifier, final KeyMapping keybind, final String name, final boolean status) {
      return formatKeybind(keyDebugModifier, keybind) + " " + name + " " + (status ? "visible" : "hidden");
   }

   private static String formatKeybind(final KeyMapping keyDebugModifier, final KeyMapping keybind) {
      return "["
         + (keyDebugModifier.isUnbound() ? "" : keyDebugModifier.getTranslatedKeyMessage().getString() + "+")
         + keybind.getTranslatedKeyMessage().getString()
         + "]";
   }

   private void extractLines(final GuiGraphicsExtractor graphics, final List<String> lines, final boolean alignLeft) {
      int height = 9;

      for (int i = 0; i < lines.size(); i++) {
         String line = lines.get(i);
         if (!Strings.isNullOrEmpty(line)) {
            int width = this.font.width(line);
            int left = alignLeft ? 2 : graphics.guiWidth() - 2 - width;
            int top = 2 + height * i;
            graphics.fill(left - 1, top - 1, left + width + 1, top + height - 1, -1873784752);
         }
      }

      for (int i = 0; i < lines.size(); i++) {
         String line = lines.get(i);
         if (!Strings.isNullOrEmpty(line)) {
            int width = this.font.width(line);
            int left = alignLeft ? 2 : graphics.guiWidth() - 2 - width;
            int top = 2 + height * i;
            graphics.text(this.font, line, left, top, -2039584, false);
         }
      }
   }

   private @Nullable ServerLevel getServerLevel() {
      if (this.minecraft.level == null) {
         return null;
      }

      IntegratedServer server = this.minecraft.getSingleplayerServer();
      return server != null ? server.getLevel(this.minecraft.level.dimension()) : null;
   }

   private @Nullable Level getLevel() {
      return this.minecraft.level == null
         ? null
         : (Level)DataFixUtils.orElse(
            Optional.ofNullable(this.minecraft.getSingleplayerServer()).flatMap(s -> Optional.ofNullable(s.getLevel(this.minecraft.level.dimension()))),
            this.minecraft.level
         );
   }

   private @Nullable LevelChunk getServerChunk() {
      if (this.minecraft.level != null && this.lastPos != null) {
         if (this.serverChunk == null) {
            ServerLevel level = this.getServerLevel();
            if (level == null) {
               return null;
            }

            this.serverChunk = level.getChunkSource()
               .getChunkFuture(this.lastPos.x(), this.lastPos.z(), ChunkStatus.FULL, false)
               .thenApply(chunkResult -> (LevelChunk)chunkResult.orElse(null));
         }

         return this.serverChunk.getNow(null);
      } else {
         return null;
      }
   }

   private @Nullable LevelChunk getClientChunk() {
      if (this.minecraft.level != null && this.lastPos != null) {
         if (this.clientChunk == null) {
            this.clientChunk = this.minecraft.level.getChunk(this.lastPos.x(), this.lastPos.z());
         }

         return this.clientChunk;
      } else {
         return null;
      }
   }

   public boolean showDebugScreen() {
      DebugScreenEntryList entries = this.minecraft.debugEntries;
      return (entries.isOverlayVisible() || !entries.getCurrentlyEnabled().isEmpty())
         && (!this.minecraft.gui.hud.isHidden() || this.minecraft.gui.screen() != null);
   }

   public boolean showProfilerChart() {
      return this.minecraft.debugEntries.isOverlayVisible() && this.renderProfilerChart;
   }

   public boolean showNetworkCharts() {
      return this.minecraft.debugEntries.isOverlayVisible() && this.renderNetworkCharts;
   }

   public boolean showFpsCharts() {
      return this.minecraft.debugEntries.isOverlayVisible() && this.renderFpsCharts;
   }

   public boolean showLightmapTexture() {
      return this.minecraft.debugEntries.isOverlayVisible() && this.renderLightmapTexture;
   }

   public void toggleNetworkCharts() {
      this.renderNetworkCharts = !this.minecraft.debugEntries.isOverlayVisible() || !this.renderNetworkCharts;
      if (this.renderNetworkCharts) {
         this.minecraft.debugEntries.setOverlayVisible(true);
         this.renderFpsCharts = false;
         this.renderLightmapTexture = false;
      }
   }

   public void toggleFpsCharts() {
      this.renderFpsCharts = !this.minecraft.debugEntries.isOverlayVisible() || !this.renderFpsCharts;
      if (this.renderFpsCharts) {
         this.minecraft.debugEntries.setOverlayVisible(true);
         this.renderNetworkCharts = false;
         this.renderLightmapTexture = false;
      }
   }

   public void toggleLightmapTexture() {
      this.renderLightmapTexture = !this.minecraft.debugEntries.isOverlayVisible() || !this.renderLightmapTexture;
      if (this.renderLightmapTexture) {
         this.minecraft.debugEntries.setOverlayVisible(true);
         this.renderFpsCharts = false;
         this.renderNetworkCharts = false;
      }
   }

   public void toggleProfilerChart() {
      this.renderProfilerChart = !this.minecraft.debugEntries.isOverlayVisible() || !this.renderProfilerChart;
      if (this.renderProfilerChart) {
         this.minecraft.debugEntries.setOverlayVisible(true);
      }
   }

   public void logFrameDuration(final long frameDuration) {
      this.frameTimeLogger.logSample(frameDuration);
   }

   public LocalSampleLogger getTickTimeLogger() {
      return this.tickTimeLogger;
   }

   public LocalSampleLogger getPingLogger() {
      return this.pingLogger;
   }

   public LocalSampleLogger getBandwidthLogger() {
      return this.bandwidthLogger;
   }

   public ProfilerPieChart getProfilerPieChart() {
      return this.profilerPieChart;
   }

   public void logRemoteSample(final long[] sample, final RemoteDebugSampleType type) {
      LocalSampleLogger logger = this.remoteSupportingLoggers.get(type);
      if (logger != null) {
         logger.logFullSample(sample);
      }
   }

   public void reset() {
      this.tickTimeLogger.reset();
      this.pingLogger.reset();
      this.bandwidthLogger.reset();
   }
}
