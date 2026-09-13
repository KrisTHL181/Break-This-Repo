package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

public final class JigsawStructure extends Structure {
    public static final DimensionPadding DEFAULT_DIMENSION_PADDING = DimensionPadding.ZERO;
    public static final LiquidSettings DEFAULT_LIQUID_SETTINGS = LiquidSettings.APPLY_WATERLOGGING;
    public static final int MAX_TOTAL_STRUCTURE_RANGE = 128;
    public static final int MIN_DEPTH = 0;
    public static final int MAX_DEPTH = 20;
    public static final MapCodec<JigsawStructure> CODEC = RecordCodecBuilder.<JigsawStructure>mapCodec(
            i -> i.group(
                    settingsCodec(i),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(j -> j.startPool),
                    Identifier.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(j -> j.startJigsawName),
                    Codec.intRange(0, 20).fieldOf("size").forGetter(j -> j.maxDepth),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(j -> j.startHeight),
                    Codec.BOOL.fieldOf("use_expansion_hack").forGetter(j -> j.useExpansionHack),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(j -> j.projectStartToHeightmap),
                    JigsawStructure.MaxDistance.CODEC.fieldOf("max_distance_from_center").forGetter(j -> j.maxDistanceFromCenter),
                    Codec.list(PoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(j -> j.poolAliases),
                    DimensionPadding.CODEC.optionalFieldOf("dimension_padding", DEFAULT_DIMENSION_PADDING).forGetter(j -> j.dimensionPadding),
                    LiquidSettings.CODEC.optionalFieldOf("liquid_settings", DEFAULT_LIQUID_SETTINGS).forGetter(j -> j.liquidSettings)
                )
                .apply(i, JigsawStructure::new)
        )
        .validate(JigsawStructure::verifyRange);
    private final Holder<StructureTemplatePool> startPool;
    private final Optional<Identifier> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final JigsawStructure.MaxDistance maxDistanceFromCenter;
    private final List<PoolAliasBinding> poolAliases;
    private final DimensionPadding dimensionPadding;
    private final LiquidSettings liquidSettings;

    private static DataResult<JigsawStructure> verifyRange(final JigsawStructure structure) {
        int edgeNeeded = switch (structure.terrainAdaptation()) {
            case NONE -> 0;
            case BURY, BEARD_THIN, BEARD_BOX, ENCAPSULATE -> 12;
        };
        return structure.maxDistanceFromCenter.horizontal() + edgeNeeded > 128
            ? DataResult.error(() -> "Horizontal structure size including terrain adaptation must not exceed 128")
            : DataResult.success(structure);
    }

    public JigsawStructure(
        final Structure.StructureSettings settings,
        final Holder<StructureTemplatePool> startPool,
        final Optional<Identifier> startJigsawName,
        final int maxDepth,
        final HeightProvider startHeight,
        final boolean useExpansionHack,
        final Optional<Heightmap.Types> projectStartToHeightmap,
        final JigsawStructure.MaxDistance maxDistanceFromCenter,
        final List<PoolAliasBinding> poolAliases,
        final DimensionPadding dimensionPadding,
        final LiquidSettings liquidSettings
    ) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.poolAliases = poolAliases;
        this.dimensionPadding = dimensionPadding;
        this.liquidSettings = liquidSettings;
    }

    public JigsawStructure(
        final Structure.StructureSettings settings,
        final Holder<StructureTemplatePool> startPool,
        final int maxDepth,
        final HeightProvider startHeight,
        final boolean useExpansionHack,
        final Heightmap.Types projectStartToHeightmap
    ) {
        this(
            settings,
            startPool,
            Optional.empty(),
            maxDepth,
            startHeight,
            useExpansionHack,
            Optional.of(projectStartToHeightmap),
            new JigsawStructure.MaxDistance(80),
            List.of(),
            DEFAULT_DIMENSION_PADDING,
            DEFAULT_LIQUID_SETTINGS
        );
    }

    public JigsawStructure(
        final Structure.StructureSettings settings,
        final Holder<StructureTemplatePool> startPool,
        final int maxDepth,
        final HeightProvider startHeight,
        final boolean useExpansionHack
    ) {
        this(
            settings,
            startPool,
            Optional.empty(),
            maxDepth,
            startHeight,
            useExpansionHack,
            Optional.empty(),
            new JigsawStructure.MaxDistance(80),
            List.of(),
            DEFAULT_DIMENSION_PADDING,
            DEFAULT_LIQUID_SETTINGS
        );
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(final Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int height = this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        BlockPos startPos = new BlockPos((int)chunkPos.getMinBlockX(), height, (int)chunkPos.getMinBlockZ());
        return JigsawPlacement.addPieces(
            context,
            this.startPool,
            this.startJigsawName,
            this.maxDepth,
            startPos,
            this.useExpansionHack,
            this.projectStartToHeightmap,
            this.maxDistanceFromCenter,
            PoolAliasLookup.create(this.poolAliases, startPos, context.seed()),
            this.dimensionPadding,
            this.liquidSettings
        );
    }

    @Override
    public StructureType<?> type() {
        return StructureType.JIGSAW;
    }

    @VisibleForTesting
    public Holder<StructureTemplatePool> getStartPool() {
        return this.startPool;
    }

    @VisibleForTesting
    public List<PoolAliasBinding> getPoolAliases() {
        return this.poolAliases;
    }

    public record MaxDistance(int horizontal, int vertical) {
        private static final Codec<Integer> HORIZONTAL_VALUE_CODEC = Codec.intRange(1, 128);
        private static final Codec<JigsawStructure.MaxDistance> FULL_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    HORIZONTAL_VALUE_CODEC.fieldOf("horizontal").forGetter(JigsawStructure.MaxDistance::horizontal),
                    ExtraCodecs.intRange(1, DimensionType.Y_SIZE)
                        .optionalFieldOf("vertical", DimensionType.Y_SIZE)
                        .forGetter(JigsawStructure.MaxDistance::vertical)
                )
                .apply(i, JigsawStructure.MaxDistance::new)
        );
        public static final Codec<JigsawStructure.MaxDistance> CODEC = Codec.either(FULL_CODEC, HORIZONTAL_VALUE_CODEC)
            .xmap(
                either -> either.map(Function.identity(), JigsawStructure.MaxDistance::new),
                distance -> distance.horizontal == distance.vertical ? Either.right(distance.horizontal) : Either.left(distance)
            );

        public MaxDistance(final int value) {
            this(value, value);
        }
    }
}