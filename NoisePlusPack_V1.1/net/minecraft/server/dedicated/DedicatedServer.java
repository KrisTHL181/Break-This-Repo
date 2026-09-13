package net.minecraft.server.dedicated;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.net.HostAndPort;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import io.netty.handler.ssl.SslContext;
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
import net.minecraft.server.ConsoleInput;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.gui.MinecraftServerGui;
import net.minecraft.server.jsonrpc.JsonRpcNotificationService;
import net.minecraft.server.jsonrpc.ManagementServer;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.security.AuthenticationHandler;
import net.minecraft.server.jsonrpc.security.JsonRpcSslContextProvider;
import net.minecraft.server.jsonrpc.security.SecurityConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.LoggingLevelLoadListener;
import net.minecraft.server.network.ServerTextFilter;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.server.rcon.thread.QueryThreadGs4;
import net.minecraft.server.rcon.thread.RconThread;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.TimeUtil;
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
   static final Logger LOGGER = LogUtils.getLogger();
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
   private @Nullable ManagementServer jsonRpcServer;
   private long lastHeartbeat;

   public DedicatedServer(
      Thread p_214789_,
      LevelStorageSource.LevelStorageAccess p_214790_,
      PackRepository p_214791_,
      WorldStem p_214792_,
      DedicatedServerSettings p_214793_,
      DataFixer p_214794_,
      Services p_214795_
   ) {
      super(p_214789_, p_214790_, p_214791_, p_214792_, Proxy.NO_PROXY, p_214794_, p_214795_, LoggingLevelLoadListener.forDedicatedServer());
      this.settings = p_214793_;
      this.rconConsoleSource = new RconConsoleSource(this);
      this.serverTextFilter = ServerTextFilter.createFromConfig(p_214793_.getProperties());
      this.serverLinks = createServerLinks(p_214793_);
      if (p_214793_.getProperties().codeOfConduct) {
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

         try (Stream<Path> stream = Files.list(path)) {
            for (Path path1 : stream.toList()) {
               String s = path1.getFileName().toString();
               if (s.endsWith(".txt")) {
                  String s1 = s.substring(0, s.length() - 4).toLowerCase(Locale.ROOT);
                  if (!path1.toRealPath().getParent().equals(path.toAbsolutePath())) {
                     throw new IllegalArgumentException(
                        "Failed to read Code of Conduct file \"" + s + "\" because it links to a file outside the allowed directory"
                     );
                  }

                  try {
                     String s2 = String.join("\n", Files.readAllLines(path1, StandardCharsets.UTF_8));
                     builder.put(s1, StringUtil.stripColor(s2));
                  } catch (IOException ioexception) {
                     throw new IllegalArgumentException("Failed to read Code of Conduct file " + s, ioexception);
                  }
               }
            }
         }

         return builder.build();
      } catch (IOException ioexception1) {
         throw new IllegalArgumentException("Failed to read Code of Conduct folder", ioexception1);
      }
   }

   private SslContext createSslContext() {
      try {
         return JsonRpcSslContextProvider.createFrom(this.getProperties().managementServerTlsKeystore, this.getProperties().managementServerTlsKeystorePassword);
      } catch (Exception exception) {
         JsonRpcSslContextProvider.printInstructions();
         throw new IllegalStateException("Failed to configure TLS for the server management protocol", exception);
      }
   }

   @Override
   public boolean initServer() throws IOException {
      int i = this.getProperties().managementServerPort;
      if (this.getProperties().managementServerEnabled) {
         String s = this.settings.getProperties().managementServerSecret;
         if (!SecurityConfig.isValid(s)) {
            throw new IllegalStateException("Invalid management server secret, must be 40 alphanumeric characters");
         }

         String s1 = this.getProperties().managementServerHost;
         HostAndPort hostandport = HostAndPort.fromParts(s1, i);
         SecurityConfig securityconfig = new SecurityConfig(s);
         String s2 = this.getProperties().managementServerAllowedOrigins;
         AuthenticationHandler authenticationhandler = new AuthenticationHandler(securityconfig, s2);
         LOGGER.info("Starting json RPC server on {}", hostandport);
         this.jsonRpcServer = new ManagementServer(hostandport, authenticationhandler);
         MinecraftApi minecraftapi = MinecraftApi.of(this);
         minecraftapi.notificationManager().registerService(new JsonRpcNotificationService(minecraftapi, this.jsonRpcServer));
         if (this.getProperties().managementServerTlsEnabled) {
            SslContext sslcontext = this.createSslContext();
            this.jsonRpcServer.startWithTls(minecraftapi, sslcontext);
         } else {
            this.jsonRpcServer.startWithoutTls(minecraftapi);
         }
      }

      Thread thread1 = new Thread("Server console handler") {
         @Override
         public void run() {
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

            String s4;
            try {
               while (!DedicatedServer.this.isStopped() && DedicatedServer.this.isRunning() && (s4 = bufferedreader.readLine()) != null) {
                  DedicatedServer.this.handleConsoleInput(s4, DedicatedServer.this.createCommandSourceStack());
               }
            } catch (IOException ioexception1) {
               DedicatedServer.LOGGER.error("Exception handling console input", ioexception1);
            }
         }
      };
      thread1.setDaemon(true);
      thread1.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      thread1.start();
      LOGGER.info("Starting minecraft server version {}", SharedConstants.getCurrentVersion().name());
      if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L) {
         LOGGER.warn("To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar minecraft_server.jar\"");
      }

      LOGGER.info("Loading properties");
      DedicatedServerProperties dedicatedserverproperties = this.settings.getProperties();
      if (this.isSingleplayer()) {
         this.setLocalIp("127.0.0.1");
      } else {
         this.setUsesAuthentication(dedicatedserverproperties.onlineMode);
         this.setPreventProxyConnections(dedicatedserverproperties.preventProxyConnections);
         this.setLocalIp(dedicatedserverproperties.serverIp);
      }

      this.worldData.setGameType(dedicatedserverproperties.gameMode.get());
      LOGGER.info("Default game type: {}", dedicatedserverproperties.gameMode.get());
      InetAddress inetaddress = null;
      if (!this.getLocalIp().isEmpty()) {
         inetaddress = InetAddress.getByName(this.getLocalIp());
      }

      if (this.getPort() < 0) {
         this.setPort(dedicatedserverproperties.serverPort);
      }

      this.initializeKeyPair();
      LOGGER.info("Starting Minecraft server on {}:{}", this.getLocalIp().isEmpty() ? "*" : this.getLocalIp(), this.getPort());

      try {
         this.getConnection().startTcpServerListener(inetaddress, this.getPort());
      } catch (IOException ioexception) {
         LOGGER.warn("**** FAILED TO BIND TO PORT!");
         LOGGER.warn("The exception was: {}", ioexception.toString());
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

      if (!OldUsersConverter.serverReadyAfterUserconversion(this)) {
         return false;
      }

      this.setPlayerList(new DedicatedPlayerList(this, this.registries(), this.playerDataStorage));
      this.tickTimeLogger = new RemoteSampleLogger(TpsDebugDimensions.values().length, this.debugSubscribers(), RemoteDebugSampleType.TICK_TIME);
      long j = Util.getNanos();
      this.services.nameToIdCache().resolveOfflineUsers(!this.usesAuthentication());
      LOGGER.info("Preparing level \"{}\"", this.getLevelIdName());
      this.loadLevel();
      long k = Util.getNanos() - j;
      String s3 = String.format(Locale.ROOT, "%.3fs", k / 1.0E9);
      LOGGER.info("Done ({})! For help, type \"help\"", s3);
      if (dedicatedserverproperties.announcePlayerAchievements != null) {
         this.worldData.getGameRules().set(GameRules.SHOW_ADVANCEMENT_MESSAGES, dedicatedserverproperties.announcePlayerAchievements, this);
      }

      if (dedicatedserverproperties.enableQuery) {
         LOGGER.info("Starting GS4 status listener");
         this.queryThreadGs4 = QueryThreadGs4.create(this);
      }

      if (dedicatedserverproperties.enableRcon) {
         LOGGER.info("Starting remote control listener");
         this.rconThread = RconThread.create(this);
      }

      if (this.getMaxTickLength() > 0L) {
         Thread thread = new Thread(new ServerWatchdog(this));
         thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandlerWithName(LOGGER));
         thread.setName("Server Watchdog");
         thread.setDaemon(true);
         thread.start();
      }

      if (dedicatedserverproperties.enableJmxMonitoring) {
         MinecraftServerStatistics.registerJmxMonitoring(this);
         LOGGER.info("JMX monitoring enabled");
      }

      this.notificationManager().serverStarted();
      return true;
   }

   @Override
   public boolean isEnforceWhitelist() {
      return this.settings.getProperties().enforceWhitelist.get();
   }

   @Override
   public void setEnforceWhitelist(boolean p_425369_) {
      this.settings.update(p_449082_ -> p_449082_.enforceWhitelist.update(this.registryAccess(), p_425369_));
   }

   @Override
   public boolean isUsingWhitelist() {
      return this.settings.getProperties().whiteList.get();
   }

   @Override
   public void setUsingWhitelist(boolean p_429836_) {
      this.settings.update(p_449086_ -> p_449086_.whiteList.update(this.registryAccess(), p_429836_));
   }

   @Override
   public void tickServer(BooleanSupplier p_428616_) {
      super.tickServer(p_428616_);
      if (this.jsonRpcServer != null) {
         this.jsonRpcServer.tick();
      }

      long i = Util.getMillis();
      int j = this.statusHeartbeatInterval();
      if (j > 0) {
         long k = j * TimeUtil.MILLISECONDS_PER_SECOND;
         if (i - this.lastHeartbeat >= k) {
            this.lastHeartbeat = i;
            this.notificationManager().statusHeartbeat();
         }
      }
   }

   @Override
   public boolean saveAllChunks(boolean p_422731_, boolean p_427964_, boolean p_430093_) {
      this.notificationManager().serverSaveStarted();
      boolean flag = super.saveAllChunks(p_422731_, p_427964_, p_430093_);
      this.notificationManager().serverSaveCompleted();
      return flag;
   }

   @Override
   public boolean allowFlight() {
      return this.settings.getProperties().allowFlight.get();
   }

   public void setAllowFlight(boolean p_431428_) {
      this.settings.update(p_449069_ -> p_449069_.allowFlight.update(this.registryAccess(), p_431428_));
   }

   @Override
   public DedicatedServerProperties getProperties() {
      return this.settings.getProperties();
   }

   public void setDifficulty(Difficulty p_422823_) {
      this.settings.update(p_449078_ -> p_449078_.difficulty.update(this.registryAccess(), p_422823_));
      this.forceDifficulty();
   }

   @Override
   public void forceDifficulty() {
      this.setDifficulty(this.getProperties().difficulty.get(), true);
   }

   public int viewDistance() {
      return this.settings.getProperties().viewDistance.get();
   }

   public void setViewDistance(int p_430686_) {
      this.settings.update(p_449100_ -> p_449100_.viewDistance.update(this.registryAccess(), p_430686_));
      this.getPlayerList().setViewDistance(p_430686_);
   }

   public int simulationDistance() {
      return this.settings.getProperties().simulationDistance.get();
   }

   public void setSimulationDistance(int p_422394_) {
      this.settings.update(p_449094_ -> p_449094_.simulationDistance.update(this.registryAccess(), p_422394_));
      this.getPlayerList().setSimulationDistance(p_422394_);
   }

   @Override
   public SystemReport fillServerSystemReport(SystemReport p_142870_) {
      p_142870_.setDetail("Is Modded", () -> this.getModdedStatus().fullDescription());
      p_142870_.setDetail("Type", () -> "Dedicated Server (map_server.txt)");
      return p_142870_;
   }

   @Override
   public void dumpServerProperties(Path p_142872_) throws IOException {
      DedicatedServerProperties dedicatedserverproperties = this.getProperties();

      try (Writer writer = Files.newBufferedWriter(p_142872_)) {
         writer.write(String.format(Locale.ROOT, "sync-chunk-writes=%s%n", dedicatedserverproperties.syncChunkWrites));
         writer.write(String.format(Locale.ROOT, "gamemode=%s%n", dedicatedserverproperties.gameMode.get()));
         writer.write(String.format(Locale.ROOT, "entity-broadcast-range-percentage=%d%n", dedicatedserverproperties.entityBroadcastRangePercentage.get()));
         writer.write(String.format(Locale.ROOT, "max-world-size=%d%n", dedicatedserverproperties.maxWorldSize));
         writer.write(String.format(Locale.ROOT, "view-distance=%d%n", dedicatedserverproperties.viewDistance.get()));
         writer.write(String.format(Locale.ROOT, "simulation-distance=%d%n", dedicatedserverproperties.simulationDistance.get()));
         writer.write(String.format(Locale.ROOT, "generate-structures=%s%n", dedicatedserverproperties.worldOptions.generateStructures()));
         writer.write(String.format(Locale.ROOT, "use-native=%s%n", dedicatedserverproperties.useNativeTransport));
         writer.write(String.format(Locale.ROOT, "rate-limit=%d%n", dedicatedserverproperties.rateLimitPacketsPerSecond));
      }
   }

   @Override
   public void onServerExit() {
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
         } catch (InterruptedException interruptedexception) {
            LOGGER.error("Interrupted while stopping the management server", interruptedexception);
         }
      }
   }

   @Override
   public void tickConnection() {
      super.tickConnection();
      this.handleConsoleInputs();
   }

   public void handleConsoleInput(String p_139646_, CommandSourceStack p_139647_) {
      this.consoleInput.add(new ConsoleInput(p_139646_, p_139647_));
   }

   public void handleConsoleInputs() {
      while (!this.consoleInput.isEmpty()) {
         ConsoleInput consoleinput = this.consoleInput.remove(0);
         this.getCommands().performPrefixedCommand(consoleinput.source, consoleinput.msg);
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

   public void setMaxPlayers(int p_430291_) {
      this.settings.update(p_449098_ -> p_449098_.maxPlayers.update(this.registryAccess(), p_430291_));
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

   public void setSpawnProtectionRadius(int p_428869_) {
      this.settings.update(p_449076_ -> p_449076_.spawnProtection.update(this.registryAccess(), p_428869_));
   }

   @Override
   public boolean isUnderSpawnProtection(ServerLevel p_139630_, BlockPos p_139631_, Player p_139632_) {
      LevelData.RespawnData leveldata$respawndata = p_139630_.getRespawnData();
      if (p_139630_.dimension() != leveldata$respawndata.dimension()) {
         return false;
      }

      if (this.getPlayerList().getOps().isEmpty()) {
         return false;
      }

      if (this.getPlayerList().isOp(p_139632_.nameAndId())) {
         return false;
      }

      if (this.spawnProtectionRadius() <= 0) {
         return false;
      }

      BlockPos blockpos = leveldata$respawndata.pos();
      int i = Mth.abs(p_139631_.getX() - blockpos.getX());
      int j = Mth.abs(p_139631_.getZ() - blockpos.getZ());
      int k = Math.max(i, j);
      return k <= this.spawnProtectionRadius();
   }

   @Override
   public boolean repliesToStatus() {
      return this.getProperties().enableStatus.get();
   }

   public void setRepliesToStatus(boolean p_426097_) {
      this.settings.update(p_449074_ -> p_449074_.enableStatus.update(this.registryAccess(), p_426097_));
   }

   @Override
   public boolean hidesOnlinePlayers() {
      return this.getProperties().hideOnlinePlayers.get();
   }

   public void setHidesOnlinePlayers(boolean p_428969_) {
      this.settings.update(p_449090_ -> p_449090_.hideOnlinePlayers.update(this.registryAccess(), p_428969_));
   }

   @Override
   public LevelBasedPermissionSet operatorUserPermissions() {
      return this.getProperties().opPermissions.get();
   }

   public void setOperatorUserPermissions(LevelBasedPermissionSet p_457800_) {
      this.settings.update(p_449102_ -> p_449102_.opPermissions.update(this.registryAccess(), p_457800_));
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
   public void setPlayerIdleTimeout(int p_139676_) {
      this.settings.update(p_449072_ -> p_449072_.playerIdleTimeout.update(this.registryAccess(), p_139676_));
   }

   public int statusHeartbeatInterval() {
      return this.settings.getProperties().statusHeartbeatInterval.get();
   }

   public void setStatusHeartbeatInterval(int p_427745_) {
      this.settings.update(p_449088_ -> p_449088_.statusHeartbeatInterval.update(this.registryAccess(), p_427745_));
   }

   @Override
   public String getMotd() {
      return this.settings.getProperties().motd.get();
   }

   @Override
   public void setMotd(String p_430126_) {
      this.settings.update(p_449096_ -> p_449096_.motd.update(this.registryAccess(), p_430126_));
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
      DedicatedServerProperties dedicatedserverproperties = this.getProperties();
      return dedicatedserverproperties.enforceSecureProfile && dedicatedserverproperties.onlineMode && this.services.canValidateProfileKeys();
   }

   @Override
   public boolean logIPs() {
      return this.getProperties().logIPs;
   }

   protected boolean convertOldUsers() {
      boolean flag = false;

      for (int i = 0; !flag && i <= 2; i++) {
         if (i > 0) {
            LOGGER.warn("Encountered a problem while converting the user banlist, retrying in a few seconds");
            this.waitForRetry();
         }

         flag = OldUsersConverter.convertUserBanlist(this);
      }

      boolean flag1 = false;

      for (int j = 0; !flag1 && j <= 2; j++) {
         if (j > 0) {
            LOGGER.warn("Encountered a problem while converting the ip banlist, retrying in a few seconds");
            this.waitForRetry();
         }

         flag1 = OldUsersConverter.convertIpBanlist(this);
      }

      boolean flag2 = false;

      for (int k = 0; !flag2 && k <= 2; k++) {
         if (k > 0) {
            LOGGER.warn("Encountered a problem while converting the op list, retrying in a few seconds");
            this.waitForRetry();
         }

         flag2 = OldUsersConverter.convertOpsList(this);
      }

      boolean flag3 = false;

      for (int l = 0; !flag3 && l <= 2; l++) {
         if (l > 0) {
            LOGGER.warn("Encountered a problem while converting the whitelist, retrying in a few seconds");
            this.waitForRetry();
         }

         flag3 = OldUsersConverter.convertWhiteList(this);
      }

      boolean flag4 = false;

      for (int i1 = 0; !flag4 && i1 <= 2; i1++) {
         if (i1 > 0) {
            LOGGER.warn("Encountered a problem while converting the player save files, retrying in a few seconds");
            this.waitForRetry();
         }

         flag4 = OldUsersConverter.convertPlayers(this);
      }

      return flag || flag1 || flag2 || flag3 || flag4;
   }

   private void waitForRetry() {
      try {
         Thread.sleep(5000L);
      } catch (InterruptedException var2) {
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
   public String runCommand(String p_139644_) {
      this.rconConsoleSource.prepareForCommand();
      this.executeBlocking(() -> this.getCommands().performPrefixedCommand(this.rconConsoleSource.createCommandSourceStack(), p_139644_));
      return this.rconConsoleSource.getCommandResponse();
   }

   @Override
   public void stopServer() {
      this.notificationManager().serverShuttingDown();
      super.stopServer();
      Util.shutdownExecutors();
   }

   @Override
   public boolean isSingleplayerOwner(NameAndId p_431025_) {
      return false;
   }

   @Override
   public int getScaledTrackingDistance(int p_139659_) {
      return this.entityBroadcastRangePercentage() * p_139659_ / 100;
   }

   public int entityBroadcastRangePercentage() {
      return this.getProperties().entityBroadcastRangePercentage.get();
   }

   public void setEntityBroadcastRangePercentage(int p_428103_) {
      this.settings.update(p_449080_ -> p_449080_.entityBroadcastRangePercentage.update(this.registryAccess(), p_428103_));
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
   public TextFilter createTextFilterForPlayer(ServerPlayer p_139634_) {
      return this.serverTextFilter != null ? this.serverTextFilter.createContext(p_139634_.getGameProfile()) : TextFilter.DUMMY;
   }

   @Override
   public @Nullable GameType getForcedGameType() {
      return this.forceGameMode() ? this.worldData.getGameType() : null;
   }

   public boolean forceGameMode() {
      return this.settings.getProperties().forceGameMode.get();
   }

   public void setForceGameMode(boolean p_428313_) {
      this.settings.update(p_449084_ -> p_449084_.forceGameMode.update(this.registryAccess(), p_428313_));
      this.enforceGameTypeForPlayers(this.getForcedGameType());
   }

   public GameType gameMode() {
      return this.getProperties().gameMode.get();
   }

   public void setGameMode(GameType p_423036_) {
      this.settings.update(p_449092_ -> p_449092_.gameMode.update(this.registryAccess(), p_423036_));
      this.worldData.setGameType(this.gameMode());
      this.enforceGameTypeForPlayers(this.getForcedGameType());
   }

   @Override
   public Optional<MinecraftServer.ServerResourcePackInfo> getServerResourcePack() {
      return this.settings.getProperties().serverResourcePackInfo;
   }

   @Override
   public void endMetricsRecordingTick() {
      super.endMetricsRecordingTick();
      this.isTickTimeLoggingEnabled = this.debugSubscribers().hasAnySubscriberFor(DebugSubscriptions.DEDICATED_SERVER_TICK_TIME);
   }

   @Override
   public SampleLogger getTickTimeLogger() {
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

   public void setAcceptsTransfers(boolean p_139689_) {
      this.settings.update(p_449067_ -> p_449067_.acceptsTransfers.update(this.registryAccess(), p_139689_));
   }

   @Override
   public ServerLinks serverLinks() {
      return this.serverLinks;
   }

   @Override
   public int pauseWhenEmptySeconds() {
      return this.settings.getProperties().pauseWhenEmptySeconds.get();
   }

   public void setPauseWhenEmptySeconds(int p_430638_) {
      this.settings.update(p_449104_ -> p_449104_.pauseWhenEmptySeconds.update(this.registryAccess(), p_430638_));
   }

   private static ServerLinks createServerLinks(DedicatedServerSettings p_343848_) {
      Optional<URI> optional = parseBugReportLink(p_343848_.getProperties());
      return optional.<ServerLinks>map(p_341204_ -> new ServerLinks(List.of(ServerLinks.KnownLinkType.BUG_REPORT.create(p_341204_)))).orElse(ServerLinks.EMPTY);
   }

   private static Optional<URI> parseBugReportLink(DedicatedServerProperties p_342981_) {
      String s = p_342981_.bugReportLink;
      if (s.isEmpty()) {
         return Optional.empty();
      }

      try {
         return Optional.of(Util.parseAndValidateUntrustedUri(s));
      } catch (Exception exception) {
         LOGGER.warn("Failed to parse bug link {}", s, exception);
         return Optional.empty();
      }
   }

   @Override
   public Map<String, String> getCodeOfConducts() {
      return this.codeOfConductTexts;
   }
}
