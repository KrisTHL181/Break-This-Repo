package net.minecraft.client.server;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.CrashReport;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.stats.Stats;
import net.minecraft.util.ModCheck;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class IntegratedServer extends MinecraftServer {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MIN_SIM_DISTANCE = 2;
   public static final int MAX_PLAYERS = 8;
   private final Minecraft minecraft;
   private boolean paused = true;
   private int publishedPort = -1;
   private @Nullable GameType publishedGameType;
   private @Nullable LanServerPinger lanPinger;
   private @Nullable UUID uuid;
   private int previousSimulationDistance = 0;
   private volatile List<SimpleGizmoCollector.GizmoInstance> latestTicksGizmos = new ArrayList<>();
   private final SimpleGizmoCollector gizmoCollector = new SimpleGizmoCollector();

   public IntegratedServer(
      Thread p_235248_,
      Minecraft p_235249_,
      LevelStorageSource.LevelStorageAccess p_235250_,
      PackRepository p_235251_,
      WorldStem p_235252_,
      Services p_235253_,
      LevelLoadListener p_425240_
   ) {
      super(p_235248_, p_235250_, p_235251_, p_235252_, p_235249_.getProxy(), p_235249_.getFixerUpper(), p_235253_, p_425240_);
      this.setSingleplayerProfile(p_235249_.getGameProfile());
      this.setDemo(p_235249_.isDemo());
      this.setPlayerList(new IntegratedPlayerList(this, this.registries(), this.playerDataStorage));
      this.minecraft = p_235249_;
   }

   @Override
   public boolean initServer() {
      LOGGER.info("Starting integrated minecraft server version {}", SharedConstants.getCurrentVersion().name());
      this.setUsesAuthentication(true);
      this.initializeKeyPair();
      this.loadLevel();
      GameProfile gameprofile = this.getSingleplayerProfile();
      String s = this.getWorldData().getLevelName();
      this.setMotd(gameprofile != null ? gameprofile.name() + " - " + s : s);
      return true;
   }

   @Override
   public boolean isPaused() {
      return this.paused;
   }

   @Override
   public void processPacketsAndTick(boolean p_457740_) {
      try (Gizmos.TemporaryCollection gizmos$temporarycollection = Gizmos.withCollector(this.gizmoCollector)) {
         super.processPacketsAndTick(p_457740_);
      }

      if (this.tickRateManager().runsNormally()) {
         this.latestTicksGizmos = this.gizmoCollector.drainGizmos();
      }
   }

   @Override
   public void tickServer(BooleanSupplier p_120049_) {
      boolean flag = this.paused;
      this.paused = Minecraft.getInstance().isPaused() || this.getPlayerList().getPlayers().isEmpty();
      ProfilerFiller profilerfiller = Profiler.get();
      if (!flag && this.paused) {
         profilerfiller.push("autoSave");
         LOGGER.info("Saving and pausing game...");
         this.saveEverything(false, false, false);
         profilerfiller.pop();
      }

      if (this.paused) {
         this.tickPaused();
      } else {
         if (flag) {
            this.forceTimeSynchronization();
         }

         super.tickServer(p_120049_);
         int i = Math.max(2, this.minecraft.options.renderDistance().get());
         if (i != this.getPlayerList().getViewDistance()) {
            LOGGER.info("Changing view distance to {}, from {}", i, this.getPlayerList().getViewDistance());
            this.getPlayerList().setViewDistance(i);
         }

         int j = Math.max(2, this.minecraft.options.simulationDistance().get());
         if (j != this.previousSimulationDistance) {
            LOGGER.info("Changing simulation distance to {}, from {}", j, this.previousSimulationDistance);
            this.getPlayerList().setSimulationDistance(j);
            this.previousSimulationDistance = j;
         }
      }
   }

   protected LocalSampleLogger getTickTimeLogger() {
      return this.minecraft.getDebugOverlay().getTickTimeLogger();
   }

   @Override
   public boolean isTickTimeLoggingEnabled() {
      return true;
   }

   private void tickPaused() {
      this.tickConnection();

      for (ServerPlayer serverplayer : this.getPlayerList().getPlayers()) {
         serverplayer.awardStat(Stats.TOTAL_WORLD_TIME);
      }
   }

   @Override
   public boolean shouldRconBroadcast() {
      return true;
   }

   @Override
   public boolean shouldInformAdmins() {
      return true;
   }

   @Override
   public Path getServerDirectory() {
      return this.minecraft.gameDirectory.toPath();
   }

   @Override
   public boolean isDedicatedServer() {
      return false;
   }

   @Override
   public int getRateLimitPacketsPerSecond() {
      return 0;
   }

   @Override
   public boolean useNativeTransport() {
      return this.minecraft.options.useNativeTransport();
   }

   @Override
   public void onServerCrash(CrashReport p_120051_) {
      this.minecraft.delayCrashRaw(p_120051_);
   }

   @Override
   public SystemReport fillServerSystemReport(SystemReport p_174970_) {
      p_174970_.setDetail("Type", "Integrated Server (map_client.txt)");
      p_174970_.setDetail("Is Modded", () -> this.getModdedStatus().fullDescription());
      p_174970_.setDetail("Launched Version", this.minecraft::getLaunchedVersion);
      return p_174970_;
   }

   @Override
   public ModCheck getModdedStatus() {
      return Minecraft.checkModStatus().merge(super.getModdedStatus());
   }

   @Override
   public boolean publishServer(@Nullable GameType p_120041_, boolean p_120042_, int p_120043_) {
      try {
         this.minecraft.prepareForMultiplayer();
         this.minecraft.getConnection().prepareKeyPair();
         this.getConnection().startTcpServerListener(null, p_120043_);
         LOGGER.info("Started serving on {}", p_120043_);
         this.publishedPort = p_120043_;
         this.lanPinger = new LanServerPinger(this.getMotd(), p_120043_ + "");
         this.lanPinger.start();
         this.publishedGameType = p_120041_;
         this.getPlayerList().setAllowCommandsForAllPlayers(p_120042_);
         PermissionSet permissionset = this.getProfilePermissions(this.minecraft.player.nameAndId());
         this.minecraft.player.setPermissions(permissionset);

         for (ServerPlayer serverplayer : this.getPlayerList().getPlayers()) {
            this.getCommands().sendCommands(serverplayer);
         }

         return true;
      } catch (IOException ioexception) {
         return false;
      }
   }

   @Override
   public void stopServer() {
      super.stopServer();
      if (this.lanPinger != null) {
         this.lanPinger.interrupt();
         this.lanPinger = null;
      }
   }

   @Override
   public void halt(boolean p_120053_) {
      this.executeBlocking(() -> {
         for (ServerPlayer serverplayer : Lists.newArrayList(this.getPlayerList().getPlayers())) {
            if (!serverplayer.getUUID().equals(this.uuid)) {
               this.getPlayerList().remove(serverplayer);
            }
         }
      });
      super.halt(p_120053_);
      if (this.lanPinger != null) {
         this.lanPinger.interrupt();
         this.lanPinger = null;
      }
   }

   @Override
   public boolean isPublished() {
      return this.publishedPort > -1;
   }

   @Override
   public int getPort() {
      return this.publishedPort;
   }

   @Override
   public void setDefaultGameType(GameType p_120039_) {
      super.setDefaultGameType(p_120039_);
      this.publishedGameType = null;
   }

   @Override
   public LevelBasedPermissionSet operatorUserPermissions() {
      return LevelBasedPermissionSet.GAMEMASTER;
   }

   public LevelBasedPermissionSet getFunctionCompilationPermissions() {
      return LevelBasedPermissionSet.GAMEMASTER;
   }

   public void setUUID(UUID p_120047_) {
      this.uuid = p_120047_;
   }

   @Override
   public boolean isSingleplayerOwner(NameAndId p_429000_) {
      return this.getSingleplayerProfile() != null && p_429000_.name().equalsIgnoreCase(this.getSingleplayerProfile().name());
   }

   @Override
   public int getScaledTrackingDistance(int p_120056_) {
      return (int)(this.minecraft.options.entityDistanceScaling().get() * p_120056_);
   }

   @Override
   public boolean forceSynchronousWrites() {
      return this.minecraft.options.syncWrites;
   }

   @Override
   public @Nullable GameType getForcedGameType() {
      return this.isPublished() && !this.isHardcore() ? (GameType)MoreObjects.firstNonNull(this.publishedGameType, this.worldData.getGameType()) : null;
   }

   @Override
   public GlobalPos selectLevelLoadFocusPos() {
      CompoundTag compoundtag = this.worldData.getLoadedPlayerTag();
      if (compoundtag == null) {
         return super.selectLevelLoadFocusPos();
      }

      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
         ValueInput valueinput = TagValueInput.create(problemreporter$scopedcollector, this.registryAccess(), compoundtag);
         ServerPlayer.SavedPosition serverplayer$savedposition = valueinput.<ServerPlayer.SavedPosition>read(ServerPlayer.SavedPosition.MAP_CODEC)
            .orElse(ServerPlayer.SavedPosition.EMPTY);
         if (serverplayer$savedposition.dimension().isPresent() && serverplayer$savedposition.position().isPresent()) {
            return new GlobalPos(serverplayer$savedposition.dimension().get(), BlockPos.containing(serverplayer$savedposition.position().get()));
         }
      }

      return super.selectLevelLoadFocusPos();
   }

   @Override
   public boolean saveEverything(boolean p_329604_, boolean p_328766_, boolean p_334434_) {
      boolean flag = super.saveEverything(p_329604_, p_328766_, p_334434_);
      this.warnOnLowDiskSpace();
      return flag;
   }

   private void warnOnLowDiskSpace() {
      if (this.storageSource.checkForLowDiskSpace()) {
         this.minecraft.execute(() -> SystemToast.onLowDiskSpace(this.minecraft));
      }
   }

   @Override
   public void reportChunkLoadFailure(Throwable p_344018_, RegionStorageInfo p_345415_, ChunkPos p_335057_) {
      super.reportChunkLoadFailure(p_344018_, p_345415_, p_335057_);
      this.warnOnLowDiskSpace();
      this.minecraft.execute(() -> SystemToast.onChunkLoadFailure(this.minecraft, p_335057_));
   }

   @Override
   public void reportChunkSaveFailure(Throwable p_345295_, RegionStorageInfo p_345019_, ChunkPos p_328809_) {
      super.reportChunkSaveFailure(p_345295_, p_345019_, p_328809_);
      this.warnOnLowDiskSpace();
      this.minecraft.execute(() -> SystemToast.onChunkSaveFailure(this.minecraft, p_328809_));
   }

   @Override
   public int getMaxPlayers() {
      return 8;
   }

   public Collection<SimpleGizmoCollector.GizmoInstance> getPerTickGizmos() {
      return this.latestTicksGizmos;
   }
}
