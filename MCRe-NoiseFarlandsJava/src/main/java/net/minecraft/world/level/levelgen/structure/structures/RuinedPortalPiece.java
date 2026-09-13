package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlackstoneReplaceProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockAgeProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.LavaSubmergedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProtectedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class RuinedPortalPiece extends TemplateStructurePiece {
    private static final float PROBABILITY_OF_GOLD_GONE = 0.3F;
    private static final float PROBABILITY_OF_MAGMA_INSTEAD_OF_NETHERRACK = 0.07F;
    private static final float PROBABILITY_OF_MAGMA_INSTEAD_OF_LAVA = 0.2F;
    private final RuinedPortalPiece.VerticalPlacement verticalPlacement;
    private final RuinedPortalPiece.Properties properties;

    public RuinedPortalPiece(
        final HolderLookup.Provider registries,
        final StructureTemplateManager structureTemplateManager,
        final BlockPos templatePosition,
        final RuinedPortalPiece.VerticalPlacement verticalPlacement,
        final RuinedPortalPiece.Properties properties,
        final Identifier templateLocation,
        final StructureTemplate template,
        final Rotation rotation,
        final Mirror mirror,
        final BlockPos pivot
    ) {
        super(
            StructurePieceType.RUINED_PORTAL,
            0,
            structureTemplateManager,
            templateLocation,
            templateLocation.toString(),
            makeSettings(registries, mirror, rotation, verticalPlacement, pivot, properties),
            templatePosition
        );
        this.verticalPlacement = verticalPlacement;
        this.properties = properties;
    }

    public RuinedPortalPiece(final StructurePieceSerializationContext context, final CompoundTag tag) {
        super(
            StructurePieceType.RUINED_PORTAL,
            tag,
            context.structureTemplateManager(),
            location -> makeSettings(context.registryAccess(), context.structureTemplateManager(), tag, location)
        );
        this.verticalPlacement = tag.read("VerticalPlacement", RuinedPortalPiece.VerticalPlacement.CODEC).orElseThrow();
        this.properties = tag.read("Properties", RuinedPortalPiece.Properties.CODEC).orElseThrow();
    }

    @Override
    protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.store("Rotation", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
        tag.store("Mirror", Mirror.LEGACY_CODEC, this.placeSettings.getMirror());
        tag.store("VerticalPlacement", RuinedPortalPiece.VerticalPlacement.CODEC, this.verticalPlacement);
        tag.store("Properties", RuinedPortalPiece.Properties.CODEC, this.properties);
    }

    private static StructurePlaceSettings makeSettings(
        final HolderLookup.Provider registries, final StructureTemplateManager structureTemplateManager, final CompoundTag tag, final Identifier location
    ) {
        StructureTemplate template = structureTemplateManager.getOrCreate(location);
        BlockPos pivot = new BlockPos(template.getSize().getX() / 2, 0, template.getSize().getZ() / 2);
        return makeSettings(
            registries,
            tag.read("Mirror", Mirror.LEGACY_CODEC).orElseThrow(),
            tag.read("Rotation", Rotation.LEGACY_CODEC).orElseThrow(),
            tag.read("VerticalPlacement", RuinedPortalPiece.VerticalPlacement.CODEC).orElseThrow(),
            pivot,
            RuinedPortalPiece.Properties.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, tag.get("Properties"))).getPartialOrThrow()
        );
    }

    private static StructurePlaceSettings makeSettings(
        final HolderLookup.Provider registries,
        final Mirror mirror,
        final Rotation rotation,
        final RuinedPortalPiece.VerticalPlacement verticalPlacement,
        final BlockPos pivot,
        final RuinedPortalPiece.Properties properties
    ) {
        HolderLookup.RegistryLookup<Block> blocks = registries.lookupOrThrow(Registries.BLOCK);
        BlockIgnoreProcessor ignoreProcessor = properties.airPocket ? BlockIgnoreProcessor.STRUCTURE_BLOCK : BlockIgnoreProcessor.STRUCTURE_AND_AIR;
        List<ProcessorRule> rules = Lists.newArrayList();
        rules.add(getBlockReplaceRule(Blocks.GOLD_BLOCK, 0.3F, Blocks.AIR));
        rules.add(getLavaProcessorRule(verticalPlacement, properties));
        if (!properties.cold) {
            rules.add(getBlockReplaceRule(Blocks.NETHERRACK, 0.07F, Blocks.MAGMA_BLOCK));
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
            .setRotation(rotation)
            .setMirror(mirror)
            .setRotationPivot(pivot)
            .addProcessor(ignoreProcessor)
            .addProcessor(new RuleProcessor(rules))
            .addProcessor(new BlockAgeProcessor(properties.mossiness))
            .addProcessor(new ProtectedBlockProcessor(blocks.getOrThrow(BlockTags.FEATURES_CANNOT_REPLACE)))
            .addProcessor(new LavaSubmergedBlockProcessor());
        if (properties.replaceWithBlackstone) {
            settings.addProcessor(BlackstoneReplaceProcessor.INSTANCE);
        }

        return settings;
    }

    private static ProcessorRule getLavaProcessorRule(
        final RuinedPortalPiece.VerticalPlacement verticalPlacement, final RuinedPortalPiece.Properties properties
    ) {
        if (verticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR) {
            return getBlockReplaceRule(Blocks.LAVA, Blocks.MAGMA_BLOCK);
        } else {
            return properties.cold ? getBlockReplaceRule(Blocks.LAVA, Blocks.NETHERRACK) : getBlockReplaceRule(Blocks.LAVA, 0.2F, Blocks.MAGMA_BLOCK);
        }
    }

    @Override
    public void postProcess(
        final WorldGenLevel level,
        final StructureManager structureManager,
        final ChunkGenerator generator,
        final RandomSource random,
        final BoundingBox chunkBB,
        final ChunkPos chunkPos,
        final BlockPos referencePos
    ) {
        BoundingBox boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
        if (chunkBB.isInside(boundingBox.getCenter())) {
            chunkBB.encapsulate(boundingBox);
            super.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);
            this.spreadNetherrack(random, level);
            this.addNetherrackDripColumnsBelowPortal(random, level);
            if (this.properties.vines || this.properties.overgrown) {
                BlockPos.betweenClosedStream(this.getBoundingBox()).forEach(pos -> {
                    if (this.properties.vines) {
                        this.maybeAddVines(random, level, pos);
                    }

                    if (this.properties.overgrown) {
                        this.maybeAddLeavesAbove(random, level, pos);
                    }
                });
            }
        }
    }

    @Override
    protected void handleDataMarker(
        final String markerId, final BlockPos pos, final ServerLevelAccessor level, final RandomSource random, final BoundingBox chunkBB
    ) {
    }

    private void maybeAddVines(final RandomSource random, final LevelAccessor level, final BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !state.is(Blocks.VINE)) {
            Direction direction = getRandomHorizontalDirection(random);
            BlockPos neighbourPos = pos.relative(direction);
            BlockState neighourState = level.getBlockState(neighbourPos);
            if (neighourState.isAir()) {
                if (Block.isFaceFull(state.getCollisionShape(level, pos), direction)) {
                    BooleanProperty vineDir = VineBlock.getPropertyForFace(direction.getOpposite());
                    level.setBlock(neighbourPos, Blocks.VINE.defaultBlockState().setValue(vineDir, true), 3);
                }
            }
        }
    }

    private void maybeAddLeavesAbove(final RandomSource random, final LevelAccessor level, final BlockPos pos) {
        if (random.nextFloat() < 0.5F && level.getBlockState(pos).is(Blocks.NETHERRACK) && level.getBlockState(pos.above()).isAir()) {
            level.setBlock(pos.above(), Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 3);
        }
    }

    private void addNetherrackDripColumnsBelowPortal(final RandomSource random, final LevelAccessor level) {
        for (int x = this.boundingBox.minX() + 1; x < this.boundingBox.maxX(); x++) {
            for (int z = this.boundingBox.minZ() + 1; z < this.boundingBox.maxZ(); z++) {
                BlockPos pos = new BlockPos(x, this.boundingBox.minY(), z);
                if (level.getBlockState(pos).is(Blocks.NETHERRACK)) {
                    this.addNetherrackDripColumn(random, level, pos.below());
                }
            }
        }
    }

    private void addNetherrackDripColumn(final RandomSource random, final LevelAccessor level, final BlockPos pos) {
        BlockPos.MutableBlockPos currentPos = pos.mutable();
        this.placeNetherrackOrMagma(random, level, currentPos);
        int remainingCap = 8;

        while (remainingCap > 0 && random.nextFloat() < 0.5F) {
            currentPos.move(Direction.DOWN);
            remainingCap--;
            this.placeNetherrackOrMagma(random, level, currentPos);
        }
    }

    private void spreadNetherrack(final RandomSource random, final LevelAccessor level) {
        boolean followGroundSurface = this.verticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_LAND_SURFACE
            || this.verticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR;
        BlockPos center = this.boundingBox.getCenter();
        int centerX = center.getX();
        int centerZ = center.getZ();
        float[] netherrackProbabilityByDistance = new float[]{1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.9F, 0.9F, 0.8F, 0.7F, 0.6F, 0.4F, 0.2F};
        int maxDistance = netherrackProbabilityByDistance.length;
        int averageWidth = (this.boundingBox.getXSpan() + this.boundingBox.getZSpan()) / 2;
        int distanceAdjustment = random.nextInt(Math.max(1, 8 - averageWidth / 2));
        int maxYDiff = 3;
        BlockPos.MutableBlockPos pos = BlockPos.ZERO.mutable();

        for (int x = centerX - maxDistance; x <= centerX + maxDistance; x++) {
            for (int z = centerZ - maxDistance; z <= centerZ + maxDistance; z++) {
                int distance = Math.abs(x - centerX) + Math.abs(z - centerZ);
                int adjustedDistance = Math.max(0, distance + distanceAdjustment);
                if (adjustedDistance < maxDistance) {
                    float probabilityOfNetherrack = netherrackProbabilityByDistance[adjustedDistance];
                    if (random.nextDouble() < probabilityOfNetherrack) {
                        int surfaceY = getSurfaceY(level, x, z, this.verticalPlacement);
                        int y = followGroundSurface ? surfaceY : Math.min(this.boundingBox.minY(), surfaceY);
                        pos.set(x, y, z);
                        if (Math.abs(y - this.boundingBox.minY()) <= 3 && this.canBlockBeReplacedByNetherrackOrMagma(level, pos)) {
                            this.placeNetherrackOrMagma(random, level, pos);
                            if (this.properties.overgrown) {
                                this.maybeAddLeavesAbove(random, level, pos);
                            }

                            this.addNetherrackDripColumn(random, level, pos.below());
                        }
                    }
                }
            }
        }
    }

    private boolean canBlockBeReplacedByNetherrackOrMagma(final LevelAccessor level, final BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.is(Blocks.AIR)
            && !state.is(Blocks.OBSIDIAN)
            && !state.is(BlockTags.FEATURES_CANNOT_REPLACE)
            && (this.verticalPlacement == RuinedPortalPiece.VerticalPlacement.IN_NETHER || !state.is(Blocks.LAVA));
    }

    private void placeNetherrackOrMagma(final RandomSource random, final LevelAccessor level, final BlockPos pos) {
        if (!this.properties.cold && random.nextFloat() < 0.07F) {
            level.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
        } else {
            level.setBlock(pos, Blocks.NETHERRACK.defaultBlockState(), 3);
        }
    }

    private static int getSurfaceY(final LevelAccessor level, final int x, final int z, final RuinedPortalPiece.VerticalPlacement verticalPlacement) {
        return level.getHeight(getHeightMapType(verticalPlacement), x, z) - 1;
    }

    public static Heightmap.Types getHeightMapType(final RuinedPortalPiece.VerticalPlacement verticalPlacement) {
        return verticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG;
    }

    private static ProcessorRule getBlockReplaceRule(final Block source, final float probability, final Block target) {
        return new ProcessorRule(new RandomBlockMatchTest(source, probability), AlwaysTrueTest.INSTANCE, target.defaultBlockState());
    }

    private static ProcessorRule getBlockReplaceRule(final Block source, final Block target) {
        return new ProcessorRule(new BlockMatchTest(source), AlwaysTrueTest.INSTANCE, target.defaultBlockState());
    }

    public record Properties(boolean cold, float mossiness, boolean airPocket, boolean overgrown, boolean vines, boolean replaceWithBlackstone) {
        public static final Codec<RuinedPortalPiece.Properties> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Codec.BOOL.fieldOf("cold").forGetter(RuinedPortalPiece.Properties::cold),
                    Codec.FLOAT.fieldOf("mossiness").forGetter(RuinedPortalPiece.Properties::mossiness),
                    Codec.BOOL.fieldOf("air_pocket").forGetter(RuinedPortalPiece.Properties::airPocket),
                    Codec.BOOL.fieldOf("overgrown").forGetter(RuinedPortalPiece.Properties::overgrown),
                    Codec.BOOL.fieldOf("vines").forGetter(RuinedPortalPiece.Properties::vines),
                    Codec.BOOL.fieldOf("replace_with_blackstone").forGetter(RuinedPortalPiece.Properties::replaceWithBlackstone)
                )
                .apply(i, RuinedPortalPiece.Properties::new)
        );
    }

    public enum VerticalPlacement implements StringRepresentable {
        ON_LAND_SURFACE("on_land_surface"),
        PARTLY_BURIED("partly_buried"),
        ON_OCEAN_FLOOR("on_ocean_floor"),
        IN_MOUNTAIN("in_mountain"),
        UNDERGROUND("underground"),
        IN_NETHER("in_nether");

        public static final Codec<RuinedPortalPiece.VerticalPlacement> CODEC = StringRepresentable.fromEnum(RuinedPortalPiece.VerticalPlacement::values);
        private final String name;

        VerticalPlacement(final String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}