package net.minecraft.world.level.levelgen.structure;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.function.Function;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

public abstract class TemplateStructurePiece extends StructurePiece {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final String templateName;
    protected final StructureTemplate template;
    protected final StructurePlaceSettings placeSettings;
    protected BlockPos templatePosition;

    public TemplateStructurePiece(
        final StructurePieceType type,
        final int genDepth,
        final StructureTemplateManager structureTemplateManager,
        final Identifier templateLocation,
        final String templateName,
        final StructurePlaceSettings placeSettings,
        final BlockPos position
    ) {
        super(type, genDepth, structureTemplateManager.getOrCreate(templateLocation).getBoundingBox(placeSettings, position));
        this.setOrientation(Direction.NORTH);
        this.templateName = templateName;
        this.templatePosition = position;
        this.template = structureTemplateManager.getOrCreate(templateLocation);
        this.placeSettings = placeSettings;
    }

    public TemplateStructurePiece(
        final StructurePieceType type,
        final CompoundTag tag,
        final StructureTemplateManager structureTemplateManager,
        final Function<Identifier, StructurePlaceSettings> structurePlaceSettingsSupplier
    ) {
        super(type, tag);
        this.setOrientation(Direction.NORTH);
        this.templateName = tag.getStringOr("Template", "");
        this.templatePosition = new BlockPos(tag.getIntOr("TPX", 0), tag.getIntOr("TPY", 0), tag.getIntOr("TPZ", 0));
        Identifier templateLocation = this.makeTemplateLocation();
        this.template = structureTemplateManager.getOrCreate(templateLocation);
        this.placeSettings = structurePlaceSettingsSupplier.apply(templateLocation);
        this.boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
    }

    protected Identifier makeTemplateLocation() {
        return Identifier.parse(this.templateName);
    }

    @Override
    protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
        tag.putInt("TPX", this.templatePosition.getX());
        tag.putInt("TPY", this.templatePosition.getY());
        tag.putInt("TPZ", this.templatePosition.getZ());
        tag.putString("Template", this.templateName);
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
        this.placeSettings.setBoundingBox(chunkBB);
        this.boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
        if (this.template.placeInWorld(level, this.templatePosition, referencePos, this.placeSettings, random, 2)) {
            for (StructureTemplate.StructureBlockInfo dataMarker : this.template
                .filterBlocks(this.templatePosition, this.placeSettings, Blocks.STRUCTURE_BLOCK)) {
                if (dataMarker.nbt() != null) {
                    StructureMode mode = dataMarker.nbt().read("mode", StructureMode.LEGACY_CODEC).orElseThrow();
                    if (mode == StructureMode.DATA) {
                        this.handleDataMarker(dataMarker.nbt().getStringOr("metadata", ""), dataMarker.pos(), level, random, chunkBB);
                    }
                }
            }

            for (StructureTemplate.StructureBlockInfo jigsawBlock : this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.JIGSAW)) {
                if (jigsawBlock.nbt() != null) {
                    String stateString = jigsawBlock.nbt().getStringOr("final_state", "minecraft:air");
                    BlockState targetState = Blocks.AIR.defaultBlockState();

                    try {
                        targetState = BlockStateParser.parseForBlock(level.holderLookup(Registries.BLOCK), stateString, true).blockState();
                    } catch (CommandSyntaxException e) {
                        LOGGER.error("Error while parsing blockstate {} in jigsaw block @ {}", stateString, jigsawBlock.pos());
                    }

                    level.setBlock(jigsawBlock.pos(), targetState, 3);
                }
            }
        }
    }

    protected abstract void handleDataMarker(String markerId, BlockPos position, ServerLevelAccessor level, RandomSource random, BoundingBox chunkBB);

    @Deprecated
    @Override
    public void move(final int dx, final int dy, final int dz) {
        super.move(dx, dy, dz);
        this.templatePosition = this.templatePosition.offset(dx, dy, dz);
    }

    @Override
    public Rotation getRotation() {
        return this.placeSettings.getRotation();
    }

    public StructureTemplate template() {
        return this.template;
    }

    public BlockPos templatePosition() {
        return this.templatePosition;
    }

    public StructurePlaceSettings placeSettings() {
        return this.placeSettings;
    }
}