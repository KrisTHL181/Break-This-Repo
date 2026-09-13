package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class IglooPieces {
    public static final int GENERATION_HEIGHT = 90;
    private static final Identifier STRUCTURE_LOCATION_IGLOO = Identifier.withDefaultNamespace("igloo/top");
    private static final Identifier STRUCTURE_LOCATION_LADDER = Identifier.withDefaultNamespace("igloo/middle");
    private static final Identifier STRUCTURE_LOCATION_LABORATORY = Identifier.withDefaultNamespace("igloo/bottom");
    private static final Map<Identifier, BlockPos> PIVOTS = ImmutableMap.of(
        STRUCTURE_LOCATION_IGLOO, new BlockPos(3, 5, 5), STRUCTURE_LOCATION_LADDER, new BlockPos(1, 3, 1), STRUCTURE_LOCATION_LABORATORY, new BlockPos(3, 6, 7)
    );
    private static final Map<Identifier, BlockPos> OFFSETS = ImmutableMap.of(
        STRUCTURE_LOCATION_IGLOO, BlockPos.ZERO, STRUCTURE_LOCATION_LADDER, new BlockPos(2, -3, 4), STRUCTURE_LOCATION_LABORATORY, new BlockPos(0, -3, -2)
    );

    public static void addPieces(
        final StructureTemplateManager structureTemplateManager,
        final BlockPos position,
        final Rotation rotation,
        final StructurePieceAccessor structurePieceAccessor,
        final RandomSource random
    ) {
        if (random.nextDouble() < 0.5) {
            int depth = random.nextInt(8) + 4;
            structurePieceAccessor.addPiece(new IglooPieces.IglooPiece(structureTemplateManager, STRUCTURE_LOCATION_LABORATORY, position, rotation, depth * 3));

            for (int i = 0; i < depth - 1; i++) {
                structurePieceAccessor.addPiece(new IglooPieces.IglooPiece(structureTemplateManager, STRUCTURE_LOCATION_LADDER, position, rotation, i * 3));
            }
        }

        structurePieceAccessor.addPiece(new IglooPieces.IglooPiece(structureTemplateManager, STRUCTURE_LOCATION_IGLOO, position, rotation, 0));
    }

    public static class IglooPiece extends TemplateStructurePiece {
        public IglooPiece(
            final StructureTemplateManager structureTemplateManager,
            final Identifier templateLocation,
            final BlockPos position,
            final Rotation rotation,
            final int depth
        ) {
            super(
                StructurePieceType.IGLOO,
                0,
                structureTemplateManager,
                templateLocation,
                templateLocation.toString(),
                makeSettings(rotation, templateLocation),
                makePosition(templateLocation, position, depth)
            );
        }

        public IglooPiece(final StructureTemplateManager structureTemplateManager, final CompoundTag tag) {
            super(
                StructurePieceType.IGLOO,
                tag,
                structureTemplateManager,
                location -> makeSettings(tag.read("Rot", Rotation.LEGACY_CODEC).orElseThrow(), location)
            );
        }

        private static StructurePlaceSettings makeSettings(final Rotation rotation, final Identifier templateLocation) {
            return new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setRotationPivot(IglooPieces.PIVOTS.get(templateLocation))
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        }

        private static BlockPos makePosition(final Identifier templateLocation, final BlockPos position, final int depth) {
            return position.offset(IglooPieces.OFFSETS.get(templateLocation)).below(depth);
        }

        @Override
        protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
        }

        @Override
        protected void handleDataMarker(
            final String markerId, final BlockPos position, final ServerLevelAccessor level, final RandomSource random, final BoundingBox chunkBB
        ) {
            if ("chest".equals(markerId)) {
                level.setBlock(position, Blocks.AIR.defaultBlockState(), 3);
                if (level.getBlockEntity(position.below()) instanceof ChestBlockEntity chestBlockEntity) {
                    chestBlockEntity.setLootTable(BuiltInLootTables.IGLOO_CHEST, random.nextLong());
                }
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
            Identifier templateLocation = Identifier.parse(this.templateName);
            StructurePlaceSettings settings = makeSettings(this.placeSettings.getRotation(), templateLocation);
            BlockPos offset = IglooPieces.OFFSETS.get(templateLocation);
            BlockPos entrancePos = this.templatePosition
                .offset(StructureTemplate.calculateRelativePosition(settings, new BlockPos(3 - offset.getX(), 0, -offset.getZ())));
            int height = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, entrancePos.getX(), entrancePos.getZ());
            BlockPos oldTemplatePos = this.templatePosition;
            this.templatePosition = this.templatePosition.offset(0, height - 90 - 1, 0);
            super.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);
            if (templateLocation.equals(IglooPieces.STRUCTURE_LOCATION_IGLOO)) {
                BlockPos trapDoorPos = this.templatePosition.offset(StructureTemplate.calculateRelativePosition(settings, new BlockPos(3, 0, 5)));
                BlockState belowState = level.getBlockState(trapDoorPos.below());
                if (!belowState.isAir() && !belowState.is(Blocks.LADDER)) {
                    level.setBlock(trapDoorPos, Blocks.SNOW_BLOCK.defaultBlockState(), 3);
                }
            }

            this.templatePosition = oldTemplatePos;
        }
    }
}