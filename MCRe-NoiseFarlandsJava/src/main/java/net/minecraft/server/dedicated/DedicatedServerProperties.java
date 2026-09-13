package net.minecraft.server.dedicated;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.jsonrpc.security.SecurityConfig;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class DedicatedServerProperties extends Settings<DedicatedServerProperties> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
    private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults();
    public static final String MANAGEMENT_SERVER_TLS_ENABLED_KEY = "management-server-tls-enabled";
    public static final String MANAGEMENT_SERVER_TLS_KEYSTORE_KEY = "management-server-tls-keystore";
    public static final String MANAGEMENT_SERVER_TLS_KEYSTORE_PASSWORD_KEY = "management-server-tls-keystore-password";
    public final boolean onlineMode = this.get("online-mode", true);
    public final boolean preventProxyConnections = this.get("prevent-proxy-connections", false);
    public final String serverIp = this.get("server-ip", "");
    public final Settings<DedicatedServerProperties>.MutableValue<
            Boolean> allowFlight = this.getMutable("allow-flight", false);
    public final Settings<DedicatedServerProperties>.MutableValue<
            String> motd = this.getMutable("motd", "A Minecraft Server");
    public final boolean codeOfConduct = this.get("enable-code-of-conduct", false);
    public final String bugReportLink = this.get("bug-report-link", "");
    public final Settings<DedicatedServerProperties>.MutableValue<
            Boolean> forceGameMode = this.getMutable("force-gamemode", false);
    public final Settings<DedicatedServerProperties>.MutableValue<
            Boolean> enforceWhitelist = this.getMutable("enforce-whitelist", false);
    public final Settings<DedicatedServerProperties>.MutableValue<
            Difficulty> difficulty = this.getMutable(
            "difficulty", dispatchNumberOrString(Difficulty::byId, Difficulty::byName), Difficulty
                    ::getSerializedName, Difficulty.EASY
    );
    public final Settings<DedicatedServerProperties>.MutableValue<
            GameType> gameMode = this.getMutable(
            "gamemode", dispatchNumberOrString(GameType::byId, GameType::byName), GameType
                    ::getName, GameType.SURVIVAL
    );
    public final String levelName = this.get("level-name", "world");
    public final int serverPort = this.get("server-port", 25565);
    public final boolean managementServerEnabled = this.get("management-server-enabled", false);
    public final String managementServerHost = this.get("management-server-host", "localhost");
    public final int managementServerPort = this.get("management-server-port", 0);
    public final String managementServerSecret = this.get("management-server-secret", SecurityConfig.generateSecretKey());
    public final boolean managementServerTlsEnabled = this.get("management-server-tls-enabled", true);
    public final String managementServerTlsKeystore = this.get("management-server-tls-keystore", "");
    public final String managementServerTlsKeystorePassword = this.get("management-server-tls-keystore-password", "");
    public final String managementServerAllowedOrigins = this.get("management-server-allowed-origins", "");
    public final @Nullable
    Boolean announcePlayerAchievements = this.getLegacyBoolean("announce-player-achievements");
    public final boolean enableQuery = this.get("enable-query", false);
    public final int queryPort = this.get("query.port", 25565);
    public final boolean enableRcon = this.get("enable-rcon", false);
    public final int rconPort = this.get("rcon.port", 25575);
    public final String rconPassword = this.get("rcon.password", "");
    public final boolean hardcore = this.get("hardcore", false);
    public final boolean useNativeTransport = this.get("use-native-transport", true);
    public final Settings<DedicatedServerProperties>.MutableValue<
            Integer> spawnProtection = this.getMutable("spawn-protection", 16);
    public final Settings<DedicatedServerProperties>.MutableValue<
            LevelBasedPermissionSet> opPermissions = this.getMutable(
            "op-permission-level", DedicatedServerProperties
                    ::deserializePermission, DedicatedServerProperties
                    ::serializePermission, LevelBasedPermissionSet.OWNER
    );
    public final LevelBasedPermissionSet functionPermissions = this.get(
            "function-permission-level",
            DedicatedServerProperties::deserializePermission,
            DedicatedServerProperties::serializePermission,
            LevelBasedPermissionSet.GAMEMASTER
    );
    public final long maxTickTime = this.get("max-tick-time", (long) TimeUnit.MINUTES.toMillis(1L));
    public final int maxChainedNeighborUpdates = this.get("max-chained-neighbor-updates", 1000000);
    public final int rateLimitPacketsPerSecond = this.get("rate-limit", 0);
    public final int commandSpamThresholdSeconds = this.get("command-spam-threshold-seconds", 10);
    public final int chatSpamThresholdSeconds = this.get("chat-spam-threshold-seconds", 10);
    public final Settings<DedicatedServerProperties>.MutableValue<
            Integer> viewDistance = this.getMutable("view-distance", 10);
    public final Settings<DedicatedServerProperties>.MutableValue<
            Integer> simulationDistance = this.getMutable("simulation-distance", 10);
    public final Settings<DedicatedServerProperties>.MutableValue<
            Integer> maxPlayers = this.getMutable("max-players", 20);
    public final int networkCompressionThreshold = this.get("network-compression-threshold", 256);
    public final boolean broadcastRconToOps = this.get("broadcast-rcon-to-ops", true);
    public final boolean broadcastConsoleToOps = this.get("broadcast-console-to-ops", true);
    public final long maxWorldSize = this.get(
            "max-world-size",
            v -> Mth.clamp(Long.parseLong(v), 1L, Long.MAX_VALUE),
            Long.MAX_VALUE
    );
    public final boolean syncChunkWrites = this.get("sync-chunk-writes", true);
    public final String regionFileComression = this.get("region-file-compression", "deflate");
    public final boolean enableJmxMonitoring = this.get("enable-jmx-monitoring", false);
    public final Settings<DedicatedServerProperties>.MutableValue<
            Boolean> enableStatus = this.getMutable("enable-status", true);
    public final Settings<DedicatedServerProperties>.MutableValue<
            Boolean> hideOnlinePlayers = this.getMutable("hide-online-players", false);
    public final Settings<DedicatedServerProperties>.MutableValue<
            Integer> entityBroadcastRangePercentage = this.getMutable(
            "entity-broadcast-range-percentage", v -> Mth.clamp(Integer.parseInt(v), 10, 1000), 100
    );
    public final String textFilteringConfig = this.get("text-filtering-config", "");
    public final int textFilteringVersion = this.get("text-filtering-version", 0);
    public final Optional<MinecraftServer.ServerResourcePackInfo> serverResourcePackInfo;
    public final DataPackConfig initialDataPackConfiguration;
    public final Settings<DedicatedServerProperties>.MutableValue<
            Integer> playerIdleTimeout = this.getMutable("player-idle-timeout", 0);
    public final Settings<DedicatedServerProperties>.MutableValue<
            Integer> statusHeartbeatInterval = this.getMutable("status-heartbeat-interval", 0);
    public final Settings<DedicatedServerProperties>.MutableValue<
            Boolean> whiteList = this.getMutable("white-list", false);
    public final boolean enforceSecureProfile = this.get("enforce-secure-profile", true);
    public final boolean logIPs = this.get("log-ips", true);
    public final Settings<DedicatedServerProperties>.MutableValue<
            Integer> pauseWhenEmptySeconds = this.getMutable("pause-when-empty-seconds", 60);
    private final DedicatedServerProperties.WorldDimensionData worldDimensionData;
    public final WorldOptions worldOptions;
    public final Settings<DedicatedServerProperties>.MutableValue<
            Boolean> acceptsTransfers = this.getMutable("accepts-transfers", false);

    public DedicatedServerProperties(final Properties settings) {
        super(settings);
        String levelSeed = this.get("level-seed", "");
        boolean generateStructures = this.get("generate-structures", true);
        long seed = WorldOptions.parseSeed(levelSeed).orElse(WorldOptions.randomSeed());
        this.worldOptions = new WorldOptions(seed, generateStructures, false);
        this.worldDimensionData = new DedicatedServerProperties.WorldDimensionData(
        this.get("generator-settings", s -> GsonHelper.parse(!s.isEmpty() ? s : "{}"), new JsonObject()),
        this.get("level-type", v -> v.toLowerCase(Locale.ROOT), WorldPresets.NORMAL.identifier().toString())
        );
        this.serverResourcePackInfo = getServerPackInfo(
        this.get("resource-pack-id", ""),
        this.get("resource-pack", ""),
        this.get("resource-pack-sha1", ""),
        this.getLegacyString("resource-pack-hash"),
        this.get("require-resource-pack", false),
        this.get("resource-pack-prompt", "")
        );
        this.initialDataPackConfiguration = getDatapackConfig(
        this.get("initial-enabled-packs", String.join(",", WorldDataConfiguration.DEFAULT.dataPacks().getEnabled())),
        this.get("initial-disabled-packs", String.join(",", WorldDataConfiguration.DEFAULT.dataPacks().getDisabled()))
        );
    }

    public static DedicatedServerProperties fromFile(final Path file) {
        return new DedicatedServerProperties(loadFromFile(file));
    }

    protected DedicatedServerProperties reload(final RegistryAccess registryAccess, final Properties properties) {
        return new DedicatedServerProperties(properties);
    }

    private static @Nullable Component parseResourcePackPrompt(final String prompt) {
        if (!Strings.isNullOrEmpty(prompt)) {
            try {
                JsonElement element = StrictJsonParser.parse(prompt);
                return ComponentSerialization.CODEC
                        .parse(RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE), element)
                        .resultOrPartial(msg -> LOGGER.warn("Failed to parse resource pack prompt '{}': {}", prompt, msg))
                        .orElse(null);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse resource pack prompt '{}'", prompt, e);
            }
        }

        return null;
    }

    private static Optional<MinecraftServer.ServerResourcePackInfo> getServerPackInfo(
            final String id,
            final String url,
            final String resourcePackSha1,
            final @Nullable String resourcePackHash,
            final boolean requireResourcePack,
            final String resourcePackPrompt) {
        if (url.isEmpty()) {
            return Optional.empty();
        }

        String hash;
        if (!resourcePackSha1.isEmpty()) {
            hash = resourcePackSha1;
            if (!Strings.isNullOrEmpty(resourcePackHash)) {
                LOGGER.warn("resource-pack-hash is deprecated and found along side resource-pack-sha1. resource-pack-hash will be ignored.");
            }
        } else if (!Strings.isNullOrEmpty(resourcePackHash)) {
            LOGGER.warn("resource-pack-hash is deprecated. Please use resource-pack-sha1 instead.");
            hash = resourcePackHash;
        } else {
            hash = "";
        }

        if (hash.isEmpty()) {
            LOGGER.warn(
                    "You specified a resource pack without providing a sha1 hash. Pack will be updated on the client only if you change the name of the pack."
            );
        } else if (!SHA1.matcher(hash).matches()) {
            LOGGER.warn("Invalid sha1 for resource-pack-sha1");
        }

        Component prompt = parseResourcePackPrompt(resourcePackPrompt);
        UUID parsedId;
        if (id.isEmpty()) {
            parsedId = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));
            LOGGER.warn("resource-pack-id missing, using default of {}", parsedId);
        } else {
            try {
                parsedId = UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Failed to parse '{}' into UUID", id);
                return Optional.empty();
            }
        }

        return Optional.of(new MinecraftServer.ServerResourcePackInfo(parsedId, url, hash, requireResourcePack, prompt));
    }

    private static DataPackConfig getDatapackConfig(final String enabledPacks, final String disabledPacks) {
        List<String> enabledPacksIds = COMMA_SPLITTER.splitToList(enabledPacks);
        List<String> disabledPacksIds = COMMA_SPLITTER.splitToList(disabledPacks);
        return new DataPackConfig(enabledPacksIds, disabledPacksIds);
    }

    public static @Nullable LevelBasedPermissionSet deserializePermission(final String value) {
        try {
            PermissionLevel permissionLevel = PermissionLevel.byId(Integer.parseInt(value));
            return LevelBasedPermissionSet.forLevel(permissionLevel);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String serializePermission(final LevelBasedPermissionSet permission) {
        return Integer.toString(permission.level().id());
    }

    public WorldDimensions createDimensions(final HolderLookup.Provider registries) {
        return this.worldDimensionData.create(registries);
    }

    private record WorldDimensionData(JsonObject generatorSettings, String levelType) {
        private static final Map<String, ResourceKey<WorldPreset>> LEGACY_PRESET_NAMES = Map.of(
                "default", WorldPresets.NORMAL, "largebiomes", WorldPresets.LARGE_BIOMES
        );

        public WorldDimensions create(final HolderLookup.Provider registries) {
            HolderLookup<
                    WorldPreset> worldPresets = registries.lookupOrThrow(Registries.WORLD_PRESET);
            Holder.Reference<WorldPreset> defaultHolder = worldPresets.get(WorldPresets.NORMAL)
                    .or(() -> worldPresets.listElements().findAny())
                    .orElseThrow(() -> new IllegalStateException("Invalid datapack contents: can't find default preset"));
            Holder<
                    WorldPreset> worldPreset = Optional.ofNullable(Identifier.tryParse(this.levelType))
                    .map(id -> ResourceKey.create(Registries.WORLD_PRESET, id))
                    .or(() -> Optional.ofNullable(LEGACY_PRESET_NAMES.get(this.levelType)))
                    .flatMap(worldPresets::get)
                    .orElseGet(() -> {
                        DedicatedServerProperties.LOGGER.warn("Failed to parse level-type {}, defaulting to {}", this.levelType, defaultHolder.key().identifier());
                        return defaultHolder;
                    });
            WorldDimensions worldDimensions = worldPreset.value().createWorldDimensions();
            if (worldPreset.is(WorldPresets.FLAT)) {
                RegistryOps<
                        JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
                Optional<
                        FlatLevelGeneratorSettings> parsedSettings = FlatLevelGeneratorSettings.CODEC
                        .parse(new Dynamic<>(ops, this.generatorSettings()))
                        .resultOrPartial(DedicatedServerProperties.LOGGER::error);
                if (parsedSettings.isPresent()) {
                    return worldDimensions.replaceOverworldGenerator(registries, new FlatLevelSource(parsedSettings.get()));
                }
            }

            return worldDimensions;
        }
    }
}