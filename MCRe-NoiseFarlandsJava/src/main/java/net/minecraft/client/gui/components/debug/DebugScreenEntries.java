package net.minecraft.client.gui.components.debug;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugScreenEntries {
    private static final Map<Identifier, DebugScreenEntry> ENTRIES_BY_ID = new HashMap<>();
    public static final Identifier GAME_VERSION = register("game_version", new DebugEntryVersion());
    public static final Identifier FPS = register("fps", new DebugEntryFps());
    public static final Identifier TPS = register("tps", new DebugEntryTps());
    public static final Identifier MEMORY = register("memory", new DebugEntryMemory());
    public static final Identifier DETAILED_MEMORY = register("detailed_memory", new DebugEntryDetailedMemory());
    public static final Identifier SYSTEM_SPECS = register("system_specs", new DebugEntrySystemSpecs());
    public static final Identifier LOOKING_AT_BLOCK_STATE = register("looking_at_block_state", new DebugEntryLookingAt.BlockStateInfo());
    public static final Identifier LOOKING_AT_BLOCK_TAGS = register("looking_at_block_tags", new DebugEntryLookingAt.BlockTagInfo());
    public static final Identifier LOOKING_AT_FLUID_STATE = register("looking_at_fluid_state", new DebugEntryLookingAt.FluidStateInfo());
    public static final Identifier LOOKING_AT_FLUID_TAGS = register("looking_at_fluid_tags", new DebugEntryLookingAt.FluidTagInfo());
    public static final Identifier LOOKING_AT_ENTITY = register("looking_at_entity", new DebugEntryLookingAtEntity());
    public static final Identifier LOOKING_AT_ENTITY_TAGS = register("looking_at_entity_tags", new DebugEntryLookingAtEntityTags());
    public static final Identifier CHUNK_RENDER_STATS = register("chunk_render_stats", new DebugEntryChunkRenderStats());
    public static final Identifier CHUNK_GENERATION_STATS = register("chunk_generation_stats", new DebugEntryChunkGeneration());
    public static final Identifier ENTITY_RENDER_STATS = register("entity_render_stats", new DebugEntryEntityRenderStats());
    public static final Identifier PARTICLE_RENDER_STATS = register("particle_render_stats", new DebugEntryParticleRenderStats());
    public static final Identifier CHUNK_SOURCE_STATS = register("chunk_source_stats", new DebugEntryChunkSourceStats());
    public static final Identifier PLAYER_POSITION = register("player_position", new DebugEntryPosition());
    public static final Identifier PLAYER_SECTION_POSITION = register("player_section_position", new DebugEntrySectionPosition());
    public static final Identifier LIGHT_LEVELS = register("light_levels", new DebugEntryLight());
    public static final Identifier HEIGHTMAP = register("heightmap", new DebugEntryHeightmap());
    public static final Identifier BIOME = register("biome", new DebugEntryBiome());
    public static final Identifier LOCAL_DIFFICULTY = register("local_difficulty", new DebugEntryLocalDifficulty());
    public static final Identifier DAY_COUNT = register("day_count", new DebugEntryDayCount());
    public static final Identifier ENTITY_SPAWN_COUNTS = register("entity_spawn_counts", new DebugEntrySpawnCounts());
    public static final Identifier SOUND_MOOD = register("sound_mood", new DebugEntrySoundMood());
    public static final Identifier SOUND_CACHE = register("sound_cache", new DebugEntrySoundCache());
    public static final Identifier POST_EFFECT = register("post_effect", new DebugEntryPostEffect());
    public static final Identifier ENTITY_HITBOXES = register("entity_hitboxes", new DebugEntryNoop());
    public static final Identifier CHUNK_BORDERS = register("chunk_borders", new DebugEntryNoop());
    public static final Identifier THREE_DIMENSIONAL_CROSSHAIR = register("3d_crosshair", new DebugEntryNoop());
    public static final Identifier CHUNK_SECTION_PATHS = register("chunk_section_paths", new DebugEntryNoop());
    public static final Identifier GPU_UTILIZATION = register("gpu_utilization", new DebugEntryGpuUtilization());
    public static final Identifier SIMPLE_PERFORMANCE_IMPACTORS = register("simple_performance_impactors", new DebugEntrySimplePerformanceImpactors());
    public static final Identifier CHUNK_SECTION_OCTREE = register("chunk_section_octree", new DebugEntryNoop());
    public static final Identifier VISUALIZE_WATER_LEVELS = register("visualize_water_levels", new DebugEntryNoop());
    public static final Identifier VISUALIZE_HEIGHTMAP = register("visualize_heightmap", new DebugEntryNoop());
    public static final Identifier VISUALIZE_COLLISION_BOXES = register("visualize_collision_boxes", new DebugEntryNoop());
    public static final Identifier VISUALIZE_ENTITY_SUPPORTING_BLOCKS = register("visualize_entity_supporting_blocks", new DebugEntryNoop());
    public static final Identifier VISUALIZE_BLOCK_LIGHT_LEVELS = register("visualize_block_light_levels", new DebugEntryNoop());
    public static final Identifier VISUALIZE_SKY_LIGHT_LEVELS = register("visualize_sky_light_levels", new DebugEntryNoop());
    public static final Identifier VISUALIZE_SOLID_FACES = register("visualize_solid_faces", new DebugEntryNoop());
    public static final Identifier VISUALIZE_CHUNKS_ON_SERVER = register("visualize_chunks_on_server", new DebugEntryNoop());
    public static final Identifier VISUALIZE_SKY_LIGHT_SECTIONS = register("visualize_sky_light_sections", new DebugEntryNoop());
    public static final Identifier CHUNK_SECTION_VISIBILITY = register("chunk_section_visibility", new DebugEntryNoop());
    public static final Map<DebugScreenProfile, Map<Identifier, DebugScreenEntryStatus>> PROFILES;

    private static Identifier register(final String id, final DebugScreenEntry entry) {
        return register(Identifier.withDefaultNamespace(id), entry);
    }

    private static Identifier register(final Identifier identifier, final DebugScreenEntry entry) {
        ENTRIES_BY_ID.put(identifier, entry);
        return identifier;
    }

    public static Map<Identifier, DebugScreenEntry> allEntries() {
        return Map.copyOf(ENTRIES_BY_ID);
    }

    public static @Nullable DebugScreenEntry getEntry(final Identifier id) {
        return ENTRIES_BY_ID.get(id);
    }

    static {
        Map<Identifier, DebugScreenEntryStatus> defaultProfile = Map.of(
            THREE_DIMENSIONAL_CROSSHAIR,
            DebugScreenEntryStatus.IN_OVERLAY,
            GAME_VERSION,
            DebugScreenEntryStatus.IN_OVERLAY,
            TPS,
            DebugScreenEntryStatus.IN_OVERLAY,
            FPS,
            DebugScreenEntryStatus.IN_OVERLAY,
            MEMORY,
            DebugScreenEntryStatus.IN_OVERLAY,
            SYSTEM_SPECS,
            DebugScreenEntryStatus.IN_OVERLAY,
            PLAYER_POSITION,
            DebugScreenEntryStatus.IN_OVERLAY,
            PLAYER_SECTION_POSITION,
            DebugScreenEntryStatus.IN_OVERLAY,
            SIMPLE_PERFORMANCE_IMPACTORS,
            DebugScreenEntryStatus.IN_OVERLAY
        );
        Map<Identifier, DebugScreenEntryStatus> performance = Map.of(
            TPS,
            DebugScreenEntryStatus.IN_OVERLAY,
            FPS,
            DebugScreenEntryStatus.ALWAYS_ON,
            GPU_UTILIZATION,
            DebugScreenEntryStatus.IN_OVERLAY,
            MEMORY,
            DebugScreenEntryStatus.IN_OVERLAY,
            SIMPLE_PERFORMANCE_IMPACTORS,
            DebugScreenEntryStatus.IN_OVERLAY
        );
        PROFILES = Map.of(DebugScreenProfile.DEFAULT, defaultProfile, DebugScreenProfile.PERFORMANCE, performance);
    }
}