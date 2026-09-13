package net.minecraft;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import java.time.Duration;
import net.minecraft.commands.BrigadierExceptions;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

@SuppressForbidden(reason = "System.out needed before bootstrap")
public class SharedConstants {
    @Deprecated public static final boolean SNAPSHOT = true;
    @Deprecated public static final int WORLD_VERSION = 5001;
    @Deprecated public static final String SERIES = "main";
    @Deprecated public static final int RELEASE_NETWORK_PROTOCOL_VERSION = 777;
    @Deprecated public static final int SNAPSHOT_NETWORK_PROTOCOL_VERSION = 325;
    public static final int SNBT_NAG_VERSION = 4997;
    private static final int SNAPSHOT_PROTOCOL_BIT = 30;
    public static final boolean CRASH_EAGERLY = true;
    @Deprecated public static final int RESOURCE_PACK_FORMAT_MAJOR = 91;
    @Deprecated public static final int RESOURCE_PACK_FORMAT_MINOR = 0;
    @Deprecated public static final int DATA_PACK_FORMAT_MAJOR = 110;
    @Deprecated public static final int DATA_PACK_FORMAT_MINOR = 0;
    public static final String RPC_MANAGEMENT_SERVER_API_VERSION = "3.1.0";
    @Deprecated public static final int LANGUAGE_FORMAT = 1;
    public static final int REPORT_FORMAT_VERSION = 1;
    public static final String DATA_VERSION_TAG = "DataVersion";
    public static final String DEBUG_FLAG_PREFIX = "MC_DEBUG_";
    public static final boolean DEBUG_ENABLED = booleanProperty(prefixDebugFlagName("ENABLED"));
    private static final boolean DEBUG_PRINT_PROPERTIES = booleanProperty(prefixDebugFlagName("PRINT_PROPERTIES"));
    public static final boolean FIX_TNT_DUPE = false;
    public static final boolean FIX_SAND_DUPE = false;
    public static final boolean DEBUG_OPEN_INCOMPATIBLE_WORLDS = debugFlag("OPEN_INCOMPATIBLE_WORLDS");
    public static final boolean DEBUG_ALLOW_LOW_SIM_DISTANCE = debugFlag("ALLOW_LOW_SIM_DISTANCE");
    public static final boolean DEBUG_HOTKEYS = debugFlag("HOTKEYS");
    public static final boolean DEBUG_UI_NARRATION = debugFlag("UI_NARRATION");
    public static final boolean DEBUG_SHUFFLE_UI_RENDERING_ORDER = debugFlag("SHUFFLE_UI_RENDERING_ORDER");
    public static final boolean DEBUG_SHUFFLE_MODELS = debugFlag("SHUFFLE_MODELS");
    public static final boolean DEBUG_RENDER_UI_LAYERING_RECTANGLES = debugFlag("RENDER_UI_LAYERING_RECTANGLES");
    public static final boolean DEBUG_PATHFINDING = debugFlag("PATHFINDING");
    public static final boolean DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES = debugFlag("SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES");
    public static final boolean DEBUG_SHAPES = debugFlag("SHAPES");
    public static final boolean DEBUG_NEIGHBORSUPDATE = debugFlag("NEIGHBORSUPDATE");
    public static final boolean DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER = debugFlag("EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER");
    public static final boolean DEBUG_STRUCTURES = debugFlag("STRUCTURES");
    public static final boolean DEBUG_GAME_EVENT_LISTENERS = debugFlag("GAME_EVENT_LISTENERS");
    public static final boolean DEBUG_DUMP_TEXTURE_ATLAS = debugFlag("DUMP_TEXTURE_ATLAS");
    public static final boolean DEBUG_STRUCTURE_EDIT_MODE = debugFlag("STRUCTURE_EDIT_MODE");
    public static final boolean DEBUG_SAVE_STRUCTURES_AS_SNBT = debugFlag("SAVE_STRUCTURES_AS_SNBT");
    public static final boolean DEBUG_SYNCHRONOUS_GL_LOGS = debugFlag("SYNCHRONOUS_GL_LOGS");
    public static final boolean DEBUG_VERBOSE_SERVER_EVENTS = debugFlag("VERBOSE_SERVER_EVENTS");
    public static final boolean DEBUG_NAMED_RUNNABLES = debugFlag("NAMED_RUNNABLES");
    public static final boolean DEBUG_GOAL_SELECTOR = debugFlag("GOAL_SELECTOR");
    public static final boolean DEBUG_VILLAGE_SECTIONS = debugFlag("VILLAGE_SECTIONS");
    public static final boolean DEBUG_BRAIN = debugFlag("BRAIN");
    public static final boolean DEBUG_POI = debugFlag("POI");
    public static final boolean DEBUG_BEES = debugFlag("BEES");
    public static final boolean DEBUG_RAIDS = debugFlag("RAIDS");
    public static final boolean DEBUG_BLOCK_BREAK = debugFlag("BLOCK_BREAK");
    public static final boolean DEBUG_MONITOR_TICK_TIMES = debugFlag("MONITOR_TICK_TIMES");
    public static final boolean DEBUG_KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN = debugFlag("KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN");
    public static final boolean DEBUG_DONT_SAVE_WORLD = debugFlag("DONT_SAVE_WORLD");
    public static final boolean DEBUG_LARGE_DRIPSTONE = debugFlag("LARGE_DRIPSTONE");
    public static final boolean DEBUG_ORE_VEINS = debugFlag("ORE_VEINS");
    public static final boolean DEBUG_SCULK_CATALYST = debugFlag("SCULK_CATALYST");
    public static final boolean DEBUG_BYPASS_REALMS_VERSION_CHECK = debugFlag("BYPASS_REALMS_VERSION_CHECK");
    public static final boolean DEBUG_SOCIAL_INTERACTIONS = debugFlag("SOCIAL_INTERACTIONS");
    public static final boolean DEBUG_CHAT_DISABLED = debugFlag("CHAT_DISABLED");
    public static final boolean DEBUG_CHAT_FRIENDS_ONLY = debugFlag("CHAT_FRIENDS_ONLY");
    public static final boolean DEBUG_VALIDATE_RESOURCE_PATH_CASE = debugFlag("VALIDATE_RESOURCE_PATH_CASE");
    public static final boolean DEBUG_UNLOCK_ALL_TRADES = debugFlag("UNLOCK_ALL_TRADES");
    public static final boolean DEBUG_BREEZE_MOB = debugFlag("BREEZE_MOB");
    public static final boolean DEBUG_TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS = debugFlag("TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS");
    public static final boolean DEBUG_VAULT_DETECTS_SHEEP_AS_PLAYERS = debugFlag("VAULT_DETECTS_SHEEP_AS_PLAYERS");
    public static final boolean DEBUG_FORCE_ONBOARDING_SCREEN = debugFlag("FORCE_ONBOARDING_SCREEN");
    public static final boolean DEBUG_CURSOR_POS = debugFlag("CURSOR_POS");
    public static final boolean DEBUG_DEFAULT_SKIN_OVERRIDE = debugFlag("DEFAULT_SKIN_OVERRIDE");
    public static final boolean DEBUG_PANORAMA_SCREENSHOT = debugFlag("PANORAMA_SCREENSHOT");
    public static final boolean DEBUG_CHASE_COMMAND = debugFlag("CHASE_COMMAND");
    public static final boolean DEBUG_VERBOSE_COMMAND_ERRORS = debugFlag("VERBOSE_COMMAND_ERRORS");
    public static final boolean DEBUG_DEV_COMMANDS = debugFlag("DEV_COMMANDS");
    public static final boolean DEBUG_ACTIVE_TEXT_AREAS = debugFlag("ACTIVE_TEXT_AREAS");
    public static final boolean DEBUG_PREFER_WAYLAND = debugFlag("PREFER_WAYLAND");
    public static final boolean DEBUG_SIMULATE_LIBRARY_LOAD_FAILURE = debugFlag("SIMULATE_LIBRARY_LOAD_FAILURE");
    public static final boolean DEBUG_IGNORE_LOCAL_MOB_CAP = debugFlag("IGNORE_LOCAL_MOB_CAP");
    public static final boolean DEBUG_DISABLE_LIQUID_SPREADING = debugFlag("DISABLE_LIQUID_SPREADING");
    public static final boolean DEBUG_AQUIFERS = debugFlag("AQUIFERS");
    public static final boolean DEBUG_JFR_PROFILING_ENABLE_LEVEL_LOADING = debugFlag("JFR_PROFILING_ENABLE_LEVEL_LOADING");
    public static final boolean DEBUG_ENTITY_BLOCK_INTERSECTION = debugFlag("ENTITY_BLOCK_INTERSECTION");
    public static boolean debugGenerateSquareTerrainWithoutNoise = debugFlag("GENERATE_SQUARE_TERRAIN_WITHOUT_NOISE");
    public static final boolean DEBUG_ONLY_GENERATE_HALF_THE_WORLD = debugFlag("ONLY_GENERATE_HALF_THE_WORLD");
    public static final boolean DEBUG_DISABLE_FLUID_GENERATION = debugFlag("DISABLE_FLUID_GENERATION");
    public static final boolean DEBUG_DISABLE_AQUIFERS = debugFlag("DISABLE_AQUIFERS");
    public static final boolean DEBUG_DISABLE_SURFACE = debugFlag("DISABLE_SURFACE");
    public static final boolean DEBUG_DISABLE_CARVERS = debugFlag("DISABLE_CARVERS");
    public static final boolean DEBUG_DISABLE_STRUCTURES = debugFlag("DISABLE_STRUCTURES");
    public static final boolean DEBUG_DISABLE_FEATURES = debugFlag("DISABLE_FEATURES");
    public static final boolean DEBUG_DISABLE_ORE_VEINS = debugFlag("DISABLE_ORE_VEINS");
    public static final boolean DEBUG_DISABLE_BLENDING = debugFlag("DISABLE_BLENDING");
    public static final boolean DEBUG_DISABLE_BELOW_ZERO_RETROGENERATION = debugFlag("DISABLE_BELOW_ZERO_RETROGENERATION");
    public static final int DEFAULT_MINECRAFT_PORT = 25565;
    public static final boolean DEBUG_SUBTITLES = debugFlag("SUBTITLES");
    public static final int DEBUG_FAKE_LATENCY_MS = debugIntValue("FAKE_LATENCY_MS");
    public static final int DEBUG_FAKE_JITTER_MS = debugIntValue("FAKE_JITTER_MS");
    public static final Level NETTY_LEAK_DETECTION = Level.DISABLED;
    public static final boolean COMMAND_STACK_TRACES = debugFlag("COMMAND_STACK_TRACES");
    public static final boolean DEBUG_WORLD_RECREATE = debugFlag("WORLD_RECREATE");
    public static final boolean DEBUG_SHOW_SERVER_DEBUG_VALUES = debugFlag("SHOW_SERVER_DEBUG_VALUES");
    public static final boolean DEBUG_FEATURE_COUNT = debugFlag("FEATURE_COUNT");
    public static final boolean DEBUG_CALCULATE_SOLID = debugFlag("CALCULATE_SOLID");
    public static final boolean DEBUG_FORCE_VULKAN_RENDERER = debugFlag("FORCE_VULKAN_RENDERER"); // 666
    public static final boolean DEBUG_FORCE_ENABLED_DEDICATED_SERVER_COMMANDS = debugFlag("FORCE_ENABLED_DEDICATED_SERVER_COMMANDS"); // 666
    public static final boolean DEBUG_FORCE_TELEMETRY = debugFlag("FORCE_TELEMETRY");
    public static final boolean DEBUG_DONT_SEND_TELEMETRY_TO_BACKEND = debugFlag("DONT_SEND_TELEMETRY_TO_BACKEND");
    public static final long MAXIMUM_TICK_TIME_NANOS = Duration.ofMillis(300L).toNanos();
    public static final float MAXIMUM_BLOCK_EXPLOSION_RESISTANCE = 3600000.0F;
    public static final boolean USE_DEVONLY = false;
    public static boolean CHECK_DATA_FIXER_SCHEMA = true;
    public static boolean IS_RUNNING_IN_IDE = debugFlag("FORCE_IDE_ENVIRONMENT"); // 666
    public static final int WORLD_RESOLUTION = 16;
    public static final int MAX_CHAT_LENGTH = 256;
    public static final int MAX_USER_INPUT_COMMAND_LENGTH = 32500;
    public static final int MAX_FUNCTION_COMMAND_LENGTH = 2000000;
    public static final int MAX_PLAYER_NAME_LENGTH = 16;
    public static final int MAX_CHAINED_NEIGHBOR_UPDATES = 1000000;
    public static final int MAX_RENDER_DISTANCE = 32;
    public static final int MAX_CLOUD_DISTANCE = 128;
    public static final char[] ILLEGAL_FILE_CHARACTERS = new char[]{'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'};
    public static final int TICKS_PER_SECOND = 20;
    public static final int MILLIS_PER_TICK = 50;
    public static final int TICKS_PER_MINUTE = 1200;
    public static final int TICKS_PER_GAME_DAY = 24000;
    public static final int DEFAULT_RANDOM_TICK_SPEED = 3;
    public static final float AVERAGE_GAME_TICKS_PER_RANDOM_TICK_PER_BLOCK = 1365.3334F;
    public static final float AVERAGE_RANDOM_TICKS_PER_BLOCK_PER_MINUTE = 0.87890625F;
    public static final float AVERAGE_RANDOM_TICKS_PER_BLOCK_PER_GAME_DAY = 17.578125F;
    public static final int WORLD_ICON_SIZE = 64;
    private static @Nullable WorldVersion CURRENT_VERSION;

    private static String prefixDebugFlagName(final String name) {
        return "MC_DEBUG_" + name;
    }

    private static boolean booleanProperty(final String name) {
        String value = System.getProperty(name);
        return value != null && (value.isEmpty() || Boolean.parseBoolean(value));
    }

    private static boolean debugFlag(final String name) {
        if (!DEBUG_ENABLED) {
            return false;
        }

        String prefixedName = prefixDebugFlagName(name);
        if (DEBUG_PRINT_PROPERTIES) {
            System.out.println("Debug property available: " + prefixedName + ": bool");
        }

        return booleanProperty(prefixedName);
    }

    private static int debugIntValue(final String name) {
        if (!DEBUG_ENABLED) {
            return 0;
        }

        String prefixedName = prefixDebugFlagName(name);
        if (DEBUG_PRINT_PROPERTIES) {
            System.out.println("Debug property available: " + prefixedName + ": int");
        }

        return Integer.parseInt(System.getProperty(prefixedName, "0"));
    }

    public static void setVersion(final WorldVersion version) {
        if (CURRENT_VERSION == null) {
            CURRENT_VERSION = version;
        } else if (version != CURRENT_VERSION) {
            throw new IllegalStateException("Cannot override the current game version!");
        }
    }

    public static void tryDetectVersion() {
        if (CURRENT_VERSION == null) {
            CURRENT_VERSION = DetectedVersion.tryDetectVersion();
        }
    }

    public static WorldVersion getCurrentVersion() {
        if (CURRENT_VERSION == null) {
            throw new IllegalStateException("Game version not set");
        } else {
            return CURRENT_VERSION;
        }
    }

    public static int getProtocolVersion() {
        return 1073742149;
    }

    public static boolean debugVoidTerrain(final ChunkPos pos) {
        int posX = pos.getMinBlockX();
        int posZ = pos.getMinBlockZ();
        if (DEBUG_ONLY_GENERATE_HALF_THE_WORLD) {
            return posZ < 0;
        } else {
            return !debugGenerateSquareTerrainWithoutNoise ? false : posX > 8192 || posX < 0 || posZ > 1024 || posZ < 0;
        }
    }

    static {
        ResourceLeakDetector.setLevel(NETTY_LEAK_DETECTION);
        CommandSyntaxException.ENABLE_COMMAND_STACK_TRACES = COMMAND_STACK_TRACES;
        CommandSyntaxException.BUILT_IN_EXCEPTIONS = new BrigadierExceptions();

        // ===== 新增：调试标志状态检测 =====
        System.out.println("=== SharedConstants Debug Flags ===");
        System.out.println("DEBUG_ENABLED = " + DEBUG_ENABLED);
        System.out.println("DEBUG_FORCE_VULKAN_RENDERER = " + DEBUG_FORCE_VULKAN_RENDERER);
        System.out.println("DEBUG_FORCE_ENABLED_DEDICATED_SERVER_COMMANDS = " + DEBUG_FORCE_ENABLED_DEDICATED_SERVER_COMMANDS);
        System.out.println("IS_RUNNING_IN_IDE (FORCE_IDE_ENVIRONMENT) = " + IS_RUNNING_IN_IDE);
        System.out.println("=====================================");
    }
}
