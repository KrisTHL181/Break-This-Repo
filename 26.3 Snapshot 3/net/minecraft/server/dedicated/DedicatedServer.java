package net.minecraft.server.dedicated;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.DefaultUncaughtExceptionHandlerWithName;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLowDiskSpaceWarningPacket;
import net.minecraft.server.ConsoleInput;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.gui.MinecraftServerGui;
import net.minecraft.server.jsonrpc.ManagementServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.LoggingLevelLoadListener;
import net.minecraft.server.network.ServerTextFilter;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.server.rcon.thread.QueryThreadGs4;
import net.minecraft.server.rcon.thread.RconThread;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debugchart.RemoteDebugSampleType;
import net.minecraft.util.debugchart.RemoteSampleLogger;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.util.debugchart.TpsDebugDimensions;
import net.minecraft.util.monitoring.jmx.MinecraftServerStatistics;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class DedicatedServer extends MinecraftServer implements ServerInterface {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int CONVERSION_RETRY_DELAY_MS = 5000;
   private static final int CONVERSION_RETRIES = 2;
   private final List<ConsoleInput> consoleInput = Collections.synchronizedList(Lists.newArrayList());
   private @Nullable QueryThreadGs4 queryThreadGs4;
   private final RconConsoleSource rconConsoleSource;
   private @Nullable RconThread rconThread;
   private final DedicatedServerSettings settings;
   private @Nullable MinecraftServerGui gui;
   private final @Nullable ServerTextFilter serverTextFilter;
   private @Nullable RemoteSampleLogger tickTimeLogger;
   private boolean isTickTimeLoggingEnabled;
   private final ServerLinks serverLinks;
   private final Map<String, String> codeOfConductTexts;
   private final @Nullable ManagementServer jsonRpcServer;

   public DedicatedServer(
      final Thread serverThread,
      final LevelStorageSource.LevelStorageAccess levelStorageSource,
      final PackRepository packRepository,
      final WorldStem worldStem,
      final Optional<GameRules> gameRules,
      final DedicatedServerSettings settings,
      final DataFixer fixerUpper,
      final Services services,
      final @Nullable ManagementServer jsonRpcServer,
      final NotificationManager notificationManager
   ) {
      super(
         serverThread,
         levelStorageSource,
         packRepository,
         worldStem,
         gameRules,
         Proxy.NO_PROXY,
         fixerUpper,
         services,
         LoggingLevelLoadListener.forDedicatedServer(),
         true,
         notificationManager
      );
      this.settings = settings;
      this.rconConsoleSource = new RconConsoleSource(this);
      this.serverTextFilter = ServerTextFilter.createFromConfig(settings.getProperties());
      this.jsonRpcServer = jsonRpcServer;
      this.serverLinks = createServerLinks(settings);
      if (settings.getProperties().codeOfConduct) {
         this.codeOfConductTexts = readCodeOfConducts();
      } else {
         this.codeOfConductTexts = Map.of();
      }
   }

   private static Map<String, String> readCodeOfConducts() {
      Path path = Path.of("codeofconduct");
      if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
         throw new IllegalArgumentException("Code of Conduct folder does not exist: " + path);
      }

      try {
         Builder<String, String> builder = ImmutableMap.builder();

         try (Stream<Path> files = Files.list(path)) {
            for (Path file : files.toList()) {
               String filename = file.getFileName().toString();
               if (filename.endsWith(".txt")) {
                  String language = filename.substring(0, filename.length() - 4).toLowerCase(Locale.ROOT);
                  if (!file.toRealPath().getParent().equals(path.toAbsolutePath())) {
                     throw new IllegalArgumentException(
                        "Failed to read Code of Conduct file \"" + filename + "\" because it links to a file outside the allowed directory"
                     );
                  }

                  try {
                     String codeOfConduct = String.join("\n", Files.readAllLines(file, StandardCharsets.UTF_8));
                     builder.put(language, StringUtil.stripColor(codeOfConduct));
                  } catch (IOException e) {
                     throw new IllegalArgumentException("Failed to read Code of Conduct file " + filename, e);
                  }
               }
            }
         }

         return builder.build();
      } catch (IOException e) {
         throw new IllegalArgumentException("Failed to read Code of Conduct folder", e);
      }
   }

   @Override
   protected boolean initServer() throws IOException {
      Thread consoleThread = new Thread("Server console handler") {
         @Override
         public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

            String line;
            try {
               while (!DedicatedServer.this.isStopped() && DedicatedServer.this.isRunning() && (line = reader.readLine()) != null) {
                  DedicatedServer.this.handleConsoleInput(line, DedicatedServer.this.createCommandSourceStack());
               }
            } catch (IOException e) {
               DedicatedServer.LOGGER.error("Exception handling console input", e);
            }
         }
      };
      consoleThread.setDaemon(true);
      consoleThread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      consoleThread.start();
      LOGGER.info("Starting minecraft server version {}", SharedConstants.getCurrentVersion().name());
      if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L) {
         LOGGER.warn("To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar minecraft_server.jar\"");
      }

      LOGGER.info("Loading properties");
      DedicatedServerProperties properties = this.settings.getProperties();
      if (this.isSingleplayer()) {
         this.setLocalIp("127.0.0.1");
      } else {
         this.setUsesAuthentication(properties.onlineMode);
         this.setPreventProxyConnections(properties.preventProxyConnections);
         this.setLocalIp(properties.serverIp);
      }

      this.worldData.setGameType(properties.gameMode.get());
      LOGGER.info("Default game type: {}", properties.gameMode.get());
      InetAddress localAddress = null;
      if (!this.getLocalIp().isEmpty()) {
         localAddress = InetAddress.getByName(this.getLocalIp());
      }

      if (this.getPort() < 0) {
         this.setPort(properties.serverPort);
      }

      this.initializeKeyPair();
      LOGGER.info("Starting Minecraft server on {}:{}", this.getLocalIp().isEmpty() ? "*" : this.getLocalIp(), this.getPort());

      try {
         this.getConnection().startTcpServerListener(localAddress, this.getPort());
      } catch (IOException e) {
         LOGGER.warn("**** FAILED TO BIND TO PORT!");
         LOGGER.warn("The exception was: {}", e.toString());
         LOGGER.warn("Perhaps a server is already running on that port?");
         return false;
      }

      if (!this.usesAuthentication()) {
         LOGGER.warn("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
         LOGGER.warn("The server will make no attempt to authenticate usernames. Beware.");
         LOGGER.warn(
            "While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any username they choose."
         );
         LOGGER.warn("To change this, set \"online-mode\" to \"true\" in the server.properties file.");
      }

      if (this.convertOldUsers()) {
         this.services.nameToIdCache().save();
      }

      if (!OldUsersConverter.areOldUserlistsRemoved()) {
         return false;
      }

      this.setPlayerList(new DedicatedPlayerList(this, this.registries(), this.playerDataStorage));
      this.tickTimeLogger = new RemoteSampleLogger(TpsDebugDimensions.values().length, this.debugSubscribers(), RemoteDebugSampleType.TICK_TIME);
      long levelNanoTime = Util.getNanos();
      this.services.nameToIdCache().resolveOfflineUsers(!this.usesAuthentication());
      LOGGER.info("Preparing level \"{}\"", this.getLevelIdName());
      this.loadLevel();
      long elapsed = Util.getNanos() - levelNanoTime;
      String time = String.format(Locale.ROOT, "%.3fs", elapsed / 1.0E9);
      LOGGER.info("Done ({})! For help, type \"help\"", time);
      if (properties.announcePlayerAchievements != null) {
         this.getGameRules().set(GameRules.SHOW_ADVANCEMENT_MESSAGES, properties.announcePlayerAchievements, this);
      }

      if (properties.enableQuery) {
         LOGGER.info("Starting GS4 status listener");
         this.queryThreadGs4 = QueryThreadGs4.create(this);
      }

      if (properties.enableRcon) {
         LOGGER.info("Starting remote control listener");
         this.rconThread = RconThread.create(this);
      }

      if (this.getMaxTickLength() > 0L) {
         Thread watchdog = new Thread(new ServerWatchdog(this), "Server Watchdog");
         watchdog.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandlerWithName(LOGGER));
         watchdog.setDaemon(true);
         watchdog.start();
      }

      if (properties.enableJmxMonitoring) {
         MinecraftServerStatistics.registerJmxMonitoring(this);
         LOGGER.info("JMX monitoring enabled");
      }

      this.saveEverything(false, true, true);
      this.notificationManager().serverStarted();
      return true;
   }

   @Override
   public boolean isEnforceWhitelist() {
      return this.settings.getProperties().enforceWhitelist.get();
   }

   @Override
   public void setEnforceWhitelist(final boolean enforceWhitelist) {
      this.settings.update(p -> p.enforceWhitelist.update(this.registryAccess(), enforceWhitelist));
   }

   @Override
   public boolean isUsingWhitelist() {
      return this.settings.getProperties().whiteList.get();
   }

   @Override
   public void setUsingWhitelist(final boolean usingWhitelist) {
      this.settings.update(p -> p.whiteList.update(this.registryAccess(), usingWhitelist));
   }

   @Override
   protected void tickServer(final BooleanSupplier haveTime) {
      super.tickServer(haveTime);
      if (this.jsonRpcServer != null) {
         this.jsonRpcServer.tick();
      }
   }

   @Override
   public boolean saveAllChunks(final boolean silent, final boolean flush, final boolean force) {
      this.notificationManager().serverSaveStarted();
      boolean savedChunks = super.saveAllChunks(silent, flush, force);
      this.notificationManager().serverSaveCompleted();
      return savedChunks;
   }

   @Override
   public void sendLowDiskSpaceWarning() {
      super.sendLowDiskSpaceWarning();
      Permission.HasCommandLevel adminCheck = new Permission.HasCommandLevel(PermissionLevel.ADMINS);
      this.getPlayerList()
         .getPlayers()
         .stream()
         .filter(p -> p.permissions().hasPermission(adminCheck))
         .forEach(p -> p.connection.send(ClientboundLowDiskSpaceWarningPacket.INSTANCE));
   }

   @Override
   public boolean allowFlight() {
      return this.settings.getProperties().allowFlight.get();
   }

   public void setAllowFlight(final boolean allowed) {
      this.settings.update(p -> p.allowFlight.update(this.registryAccess(), allowed));
   }

   @Override
   public DedicatedServerProperties getProperties() {
      return this.settings.getProperties();
   }

   public void setDifficulty(final Difficulty difficulty) {
      this.settings.update(p -> p.difficulty.update(this.registryAccess(), difficulty));
      this.forceDifficulty();
   }

   @Override
   protected void forceDifficulty() {
      this.setDifficulty(this.getProperties().difficulty.get(), true);
   }

   public int viewDistance() {
      return this.settings.getProperties().viewDistance.get();
   }

   public void setViewDistance(final int viewDistance) {
      this.settings.update(p -> p.viewDistance.update(this.registryAccess(), viewDistance));
      this.getPlayerList().setViewDistance(viewDistance);
   }

   public int simulationDistance() {
      return this.settings.getProperties().simulationDistance.get();
   }

   public void setSimulationDistance(final int simulationDistance) {
      this.settings.update(p -> p.simulationDistance.update(this.registryAccess(), simulationDistance));
      this.getPlayerList().setSimulationDistance(simulationDistance);
   }

   @Override
   public SystemReport fillServerSystemReport(final SystemReport systemReport) {
      systemReport.setDetail("Is Modded", () -> this.getModdedStatus().fullDescription());
      systemReport.setDetail("Type", () -> "Dedicated Server");
      return systemReport;
   }

   @Override
   public void dumpServerProperties(final Path path) throws IOException {
      DedicatedServerProperties serverProperties = this.getProperties();

      try (Writer output = Files.newBufferedWriter(path)) {
         output.write(String.format(Locale.ROOT, "sync-chunk-writes=%s%n", serverProperties.syncChunkWrites));
         output.write(String.format(Locale.ROOT, "gamemode=%s%n", serverProperties.gameMode.get()));
         output.write(String.format(Locale.ROOT, "entity-broadcast-range-percentage=%d%n", serverProperties.entityBroadcastRangePercentage.get()));
         output.write(String.format(Locale.ROOT, "max-world-size=%d%n", serverProperties.maxWorldSize));
         output.write(String.format(Locale.ROOT, "view-distance=%d%n", serverProperties.viewDistance.get()));
         output.write(String.format(Locale.ROOT, "simulation-distance=%d%n", serverProperties.simulationDistance.get()));
         output.write(String.format(Locale.ROOT, "generate-structures=%s%n", serverProperties.worldOptions.generateStructures()));
         output.write(String.format(Locale.ROOT, "use-native=%s%n", serverProperties.useNativeTransport));
         output.write(String.format(Locale.ROOT, "rate-limit=%d%n", serverProperties.rateLimitPacketsPerSecond));
         output.write(String.format(Locale.ROOT, "command-spam-threshold-seconds=%d%n", serverProperties.commandSpamThresholdSeconds));
         output.write(String.format(Locale.ROOT, "chat-spam-threshold-seconds=%d%n", serverProperties.chatSpamThresholdSeconds));
      }
   }

   @Override
   protected void onServerExit() {
      if (this.serverTextFilter != null) {
         this.serverTextFilter.close();
      }

      if (this.gui != null) {
         this.gui.close();
      }

      if (this.rconThread != null) {
         this.rconThread.stop();
      }

      if (this.queryThreadGs4 != null) {
         this.queryThreadGs4.stop();
      }

      if (this.jsonRpcServer != null) {
         try {
            this.jsonRpcServer.stop(true);
         } catch (InterruptedException e) {
            LOGGER.error("Interrupted while stopping the management server", e);
         }
      }
   }

   @Override
   protected void tickConnection() {
      super.tickConnection();
      this.handleConsoleInputs();
   }

   public void handleConsoleInput(final String msg, final CommandSourceStack source) {
      this.consoleInput.add(new ConsoleInput(msg, source));
   }

   public void handleConsoleInputs() {
      while (!this.consoleInput.isEmpty()) {
         ConsoleInput input = this.consoleInput.remove(0);
         this.getCommands().performPrefixedCommand(input.source, input.msg);
      }
   }

   @Override
   public boolean isDedicatedServer() {
      return true;
   }

   @Override
   public int getRateLimitPacketsPerSecond() {
      return this.getProperties().rateLimitPacketsPerSecond;
   }

   @Override
   public int getCommandSpamThresholdSeconds() {
      return this.getProperties().commandSpamThresholdSeconds;
   }

   @Override
   public int getChatSpamThresholdSeconds() {
      return this.getProperties().chatSpamThresholdSeconds;
   }

   @Override
   public boolean useNativeTransport() {
      return this.getProperties().useNativeTransport;
   }

   public DedicatedPlayerList getPlayerList() {
      return (DedicatedPlayerList)super.getPlayerList();
   }

   @Override
   public int getMaxPlayers() {
      return this.settings.getProperties().maxPlayers.get();
   }

   public void setMaxPlayers(final int maxPlayers) {
      this.settings.update(p -> p.maxPlayers.update(this.registryAccess(), maxPlayers));
   }

   @Override
   public boolean isPublished() {
      return true;
   }

   @Override
   public String getServerIp() {
      return this.getLocalIp();
   }

   @Override
   public int getServerPort() {
      return this.getPort();
   }

   @Override
   public String getServerName() {
      return this.getMotd();
   }

   public void showGui() {
      if (this.gui == null) {
         this.gui = MinecraftServerGui.showFrameFor(this);
      }
   }

   public int spawnProtectionRadius() {
      return this.getProperties().spawnProtection.get();
   }

   public void setSpawnProtectionRadius(final int spawnProtectionRadius) {
      this.settings.update(p -> p.spawnProtection.update(this.registryAccess(), spawnProtectionRadius));
   }

   @Override
   public boolean isUnderSpawnProtection(final ServerLevel level, final BlockPos pos, final Player player) {
      LevelData.RespawnData respawnData = level.getRespawnData();
      if (level.dimension() != respawnData.dimension()) {
         return false;
      }

      if (this.getPlayerList().getOps().isEmpty()) {
         return false;
      }

      if (this.getPlayerList().isOp(player.nameAndId())) {
         return false;
      }

      if (this.spawnProtectionRadius() <= 0) {
         return false;
      }

      BlockPos spawnPos = respawnData.pos();
      int xd = Mth.abs(pos.getX() - spawnPos.getX());
      int zd = Mth.abs(pos.getZ() - spawnPos.getZ());
      int dist = Math.max(xd, zd);
      return dist <= this.spawnProtectionRadius();
   }

   @Override
   public boolean repliesToStatus() {
      return this.getProperties().enableStatus.get();
   }

   public void setRepliesToStatus(final boolean enable) {
      this.settings.update(p -> p.enableStatus.update(this.registryAccess(), enable));
   }

   @Override
   public boolean hidesOnlinePlayers() {
      return this.getProperties().hideOnlinePlayers.get();
   }

   public void setHidesOnlinePlayers(final boolean hide) {
      this.settings.update(p -> p.hideOnlinePlayers.update(this.registryAccess(), hide));
   }

   @Override
   public LevelBasedPermissionSet operatorUserPermissions() {
      return this.getProperties().opPermissions.get();
   }

   public void setOperatorUserPermissions(final LevelBasedPermissionSet permissions) {
      this.settings.update(p -> p.opPermissions.update(this.registryAccess(), permissions));
   }

   @Override
   public PermissionSet getFunctionCompilationPermissions() {
      return this.getProperties().functionPermissions;
   }

   @Override
   public int playerIdleTimeout() {
      return this.settings.getProperties().playerIdleTimeout.get();
   }

   @Override
   public void setPlayerIdleTimeout(final int playerIdleTimeout) {
      this.settings.update(p -> p.playerIdleTimeout.update(this.registryAccess(), playerIdleTimeout));
   }

   public int statusHeartbeatInterval() {
      return this.settings.getProperties().statusHeartbeatInterval.get();
   }

   public boolean setStatusHeartbeatInterval(final int statusHeartbeatInterval) {
      if (this.jsonRpcServer != null && this.jsonRpcServer.scheduleHeartbeat(this.notificationManager(), statusHeartbeatInterval)) {
         this.settings.update(p -> p.statusHeartbeatInterval.update(this.registryAccess(), statusHeartbeatInterval));
         return true;
      } else {
         return false;
      }
   }

   @Override
   public String getMotd() {
      return this.settings.getProperties().motd.get();
   }

   @Override
   public void setMotd(final String motd) {
      this.settings.update(p -> p.motd.update(this.registryAccess(), motd));
   }

   @Override
   public boolean shouldRconBroadcast() {
      return this.getProperties().broadcastRconToOps;
   }

   @Override
   public boolean shouldInformAdmins() {
      return this.getProperties().broadcastConsoleToOps;
   }

   @Override
   public int getAbsoluteMaxWorldSize() {
      return this.getProperties().maxWorldSize;
   }

   @Override
   public int getCompressionThreshold() {
      return this.getProperties().networkCompressionThreshold;
   }

   @Override
   public boolean enforceSecureProfile() {
      DedicatedServerProperties properties = this.getProperties();
      return properties.enforceSecureProfile && properties.onlineMode && this.services.canValidateProfileKeys();
   }

   @Override
   public boolean logIPs() {
      return this.getProperties().logIPs;
   }

   protected boolean convertOldUsers() {
      boolean userBanlistConverted = false;

      for (int retries = 0; !userBanlistConverted && retries <= 2; retries++) {
         if (retries > 0) {
            LOGGER.warn("Encountered a problem while converting the user banlist, retrying in a few seconds");
            this.waitForRetry();
         }

         userBanlistConverted = OldUsersConverter.convertUserBanlist(this);
      }

      boolean ipBanlistConverted = false;

      for (int var7 = 0; !ipBanlistConverted && var7 <= 2; var7++) {
         if (var7 > 0) {
            LOGGER.warn("Encountered a problem while converting the ip banlist, retrying in a few seconds");
            this.waitForRetry();
         }

         ipBanlistConverted = OldUsersConverter.convertIpBanlist(this);
      }

      boolean opListConverted = false;

      for (int var8 = 0; !opListConverted && var8 <= 2; var8++) {
         if (var8 > 0) {
            LOGGER.warn("Encountered a problem while converting the op list, retrying in a few seconds");
            this.waitForRetry();
         }

         opListConverted = OldUsersConverter.convertOpsList(this);
      }

      boolean whitelistConverted = false;

      for (int var9 = 0; !whitelistConverted && var9 <= 2; var9++) {
         if (var9 > 0) {
            LOGGER.warn("Encountered a problem while converting the whitelist, retrying in a few seconds");
            this.waitForRetry();
         }

         whitelistConverted = OldUsersConverter.convertWhiteList(this);
      }

      boolean playersConverted = false;

      for (int var10 = 0; !playersConverted && var10 <= 2; var10++) {
         if (var10 > 0) {
            LOGGER.warn("Encountered a problem while converting the player save files, retrying in a few seconds");
            this.waitForRetry();
         }

         playersConverted = OldUsersConverter.convertPlayers(this);
      }

      return userBanlistConverted || ipBanlistConverted || opListConverted || whitelistConverted || playersConverted;
   }

   private void waitForRetry() {
      try {
         Thread.sleep(5000L);
      } catch (InterruptedException ignored) {
      }
   }

   public long getMaxTickLength() {
      return this.getProperties().maxTickTime;
   }

   @Override
   public int getMaxChainedNeighborUpdates() {
      return this.getProperties().maxChainedNeighborUpdates;
   }

   @Override
   public String getPluginNames() {
      return "";
   }

   @Override
   public String runCommand(final String command) {
      this.rconConsoleSource.prepareForCommand();
      this.executeBlocking(() -> this.getCommands().performPrefixedCommand(this.rconConsoleSource.createCommandSourceStack(), command));
      return this.rconConsoleSource.getCommandResponse();
   }

   @Override
   protected void stopServer() {
      this.notificationManager().serverShuttingDown();
      super.stopServer();
      Util.shutdownExecutors();
   }

   @Override
   public boolean isSingleplayerOwner(final NameAndId nameAndId) {
      return false;
   }

   @Override
   public int getScaledTrackingDistance(final int range) {
      return this.entityBroadcastRangePercentage() * range / 100;
   }

   public int entityBroadcastRangePercentage() {
      return this.getProperties().entityBroadcastRangePercentage.get();
   }

   public void setEntityBroadcastRangePercentage(final int range) {
      this.settings.update(p -> p.entityBroadcastRangePercentage.update(this.registryAccess(), range));
   }

   @Override
   public String getLevelIdName() {
      return this.storageSource.getLevelId();
   }

   @Override
   public boolean forceSynchronousWrites() {
      return this.settings.getProperties().syncChunkWrites;
   }

   @Override
   public TextFilter createTextFilterForPlayer(final ServerPlayer player) {
      return this.serverTextFilter != null ? this.serverTextFilter.createContext(player.getGameProfile()) : TextFilter.DUMMY;
   }

   @Override
   public @Nullable GameType getForcedGameType() {
      return this.forceGameMode() ? this.worldData.getGameType() : null;
   }

   public boolean forceGameMode() {
      return this.settings.getProperties().forceGameMode.get();
   }

   public void setForceGameMode(final boolean forceGameMode) {
      this.settings.update(p -> p.forceGameMode.update(this.registryAccess(), forceGameMode));
      this.enforceGameTypeForPlayers(this.getForcedGameType());
   }

   public GameType gameMode() {
      return this.getProperties().gameMode.get();
   }

   public void setGameMode(final GameType gameMode) {
      this.settings.update(p -> p.gameMode.update(this.registryAccess(), gameMode));
      this.worldData.setGameType(this.gameMode());
      this.enforceGameTypeForPlayers(this.getForcedGameType());
   }

   @Override
   public Optional<MinecraftServer.ServerResourcePackInfo> getServerResourcePack() {
      return this.settings.getProperties().serverResourcePackInfo;
   }

   @Override
   protected void endMetricsRecordingTick() {
      super.endMetricsRecordingTick();
      this.isTickTimeLoggingEnabled = this.debugSubscribers().hasAnySubscriberFor(DebugSubscriptions.DEDICATED_SERVER_TICK_TIME);
   }

   @Override
   protected SampleLogger getTickTimeLogger() {
      return this.tickTimeLogger;
   }

   @Override
   public boolean isTickTimeLoggingEnabled() {
      return this.isTickTimeLoggingEnabled;
   }

   @Override
   public boolean acceptsTransfers() {
      return this.settings.getProperties().acceptsTransfers.get();
   }

   public void setAcceptsTransfers(final boolean acceptTransfers) {
      this.settings.update(p -> p.acceptsTransfers.update(this.registryAccess(), acceptTransfers));
   }

   @Override
   public ServerLinks serverLinks() {
      return this.serverLinks;
   }

   @Override
   public int pauseWhenEmptySeconds() {
      return this.settings.getProperties().pauseWhenEmptySeconds.get();
   }

   public void setPauseWhenEmptySeconds(final int seconds) {
      this.settings.update(p -> p.pauseWhenEmptySeconds.update(this.registryAccess(), seconds));
   }

   private static ServerLinks createServerLinks(final DedicatedServerSettings settings) {
      Optional<URI> bugReportLink = parseBugReportLink(settings.getProperties());
      return bugReportLink.<ServerLinks>map(bugLink -> new ServerLinks(List.of(ServerLinks.KnownLinkType.BUG_REPORT.create(bugLink))))
         .orElse(ServerLinks.EMPTY);
   }

   private static Optional<URI> parseBugReportLink(final DedicatedServerProperties properties) {
      String bugReportLink = properties.bugReportLink;
      if (bugReportLink.isEmpty()) {
         return Optional.empty();
      }

      try {
         return Optional.of(Util.parseAndValidateUntrustedUri(bugReportLink));
      } catch (Exception e) {
         LOGGER.warn("Failed to parse bug link {}", bugReportLink, e);
         return Optional.empty();
      }
   }

   @Override
   public Map<String, String> getCodeOfConducts() {
      return this.codeOfConductTexts;
   }
}
