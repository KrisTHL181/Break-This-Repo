package net.minecraft.world.level.block.entity;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.IdentifierException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class StructureBlockEntity extends BlockEntity implements BoundingBoxRenderable {
    private static final int SCAN_CORNER_BLOCKS_RANGE = 5;
    public static final int MAX_OFFSET_PER_AXIS = 48;
    public static final int MAX_SIZE_PER_AXIS = 48;
    public static final String AUTHOR_TAG = "author";
    private static final String DEFAULT_AUTHOR = "";
    private static final String DEFAULT_METADATA = "";
    private static final BlockPos DEFAULT_POS = new BlockPos(0, 1, 0);
    private static final Vec3i DEFAULT_SIZE = Vec3i.ZERO;
    private static final Rotation DEFAULT_ROTATION = Rotation.NONE;
    private static final Mirror DEFAULT_MIRROR = Mirror.NONE;
    private static final boolean DEFAULT_IGNORE_ENTITIES = true;
    private static final boolean DEFAULT_STRICT = false;
    private static final boolean DEFAULT_POWERED = false;
    private static final boolean DEFAULT_SHOW_AIR = false;
    private static final boolean DEFAULT_SHOW_BOUNDING_BOX = true;
    private static final float DEFAULT_INTEGRITY = 1.0F;
    private static final long DEFAULT_SEED = 0L;
    private @Nullable Identifier structureName;
    private String author = "";
    private String metaData = "";
    private BlockPos structurePos = DEFAULT_POS;
    private Vec3i structureSize = DEFAULT_SIZE;
    private Mirror mirror = Mirror.NONE;
    private Rotation rotation = Rotation.NONE;
    private StructureMode mode;
    private boolean ignoreEntities = true;
    private boolean strict = false;
    private boolean powered = false;
    private boolean showAir = false;
    private boolean showBoundingBox = true;
    private float integrity = 1.0F;
    private long seed = 0L;

    public StructureBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        super(BlockEntityTypes.STRUCTURE_BLOCK, worldPosition, blockState);
        this.mode = blockState.getValue(StructureBlock.MODE);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putString("name", this.getStructureName());
        output.putString("author", this.author);
        output.putString("metadata", this.metaData);
        output.putInt("posX", this.structurePos.getX());
        output.putInt("posY", this.structurePos.getY());
        output.putInt("posZ", this.structurePos.getZ());
        output.putInt("sizeX", this.structureSize.getX());
        output.putInt("sizeY", this.structureSize.getY());
        output.putInt("sizeZ", this.structureSize.getZ());
        output.store("rotation", Rotation.LEGACY_CODEC, this.rotation);
        output.store("mirror", Mirror.LEGACY_CODEC, this.mirror);
        output.store("mode", StructureMode.LEGACY_CODEC, this.mode);
        output.putBoolean("ignoreEntities", this.ignoreEntities);
        output.putBoolean("strict", this.strict);
        output.putBoolean("powered", this.powered);
        output.putBoolean("showair", this.showAir);
        output.putBoolean("showboundingbox", this.showBoundingBox);
        output.putFloat("integrity", this.integrity);
        output.putLong("seed", this.seed);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.setStructureName(input.getStringOr("name", ""));
        this.author = input.getStringOr("author", "");
        this.metaData = input.getStringOr("metadata", "");
        int xOffset = Mth.clamp(input.getIntOr("posX", DEFAULT_POS.getX()), -48, 48);
        int yOffset = Mth.clamp(input.getIntOr("posY", DEFAULT_POS.getY()), -48, 48);
        int zOffset = Mth.clamp(input.getIntOr("posZ", DEFAULT_POS.getZ()), -48, 48);
        this.structurePos = new BlockPos(xOffset, yOffset, zOffset);
        int width = Mth.clamp(input.getIntOr("sizeX", DEFAULT_SIZE.getX()), 0, 48);
        int height = Mth.clamp(input.getIntOr("sizeY", DEFAULT_SIZE.getY()), 0, 48);
        int depth = Mth.clamp(input.getIntOr("sizeZ", DEFAULT_SIZE.getZ()), 0, 48);
        this.structureSize = new Vec3i(width, height, depth);
        this.rotation = input.read("rotation", Rotation.LEGACY_CODEC).orElse(DEFAULT_ROTATION);
        this.mirror = input.read("mirror", Mirror.LEGACY_CODEC).orElse(DEFAULT_MIRROR);
        this.mode = input.read("mode", StructureMode.LEGACY_CODEC).orElse(StructureMode.DATA);
        this.ignoreEntities = input.getBooleanOr("ignoreEntities", true);
        this.strict = input.getBooleanOr("strict", false);
        this.powered = input.getBooleanOr("powered", false);
        this.showAir = input.getBooleanOr("showair", false);
        this.showBoundingBox = input.getBooleanOr("showboundingbox", true);
        this.integrity = input.getFloatOr("integrity", 1.0F);
        this.seed = input.getLongOr("seed", 0L);
        this.updateBlockState();
    }

    private void updateBlockState() {
        if (this.level != null) {
            BlockPos pos = this.getBlockPos();
            BlockState blockState = this.level.getBlockState(pos);
            if (blockState.is(Blocks.STRUCTURE_BLOCK)) {
                this.level.setBlock(pos, blockState.setValue(StructureBlock.MODE, this.mode), 2);
            }
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    public boolean usedBy(final Player player) {
        if (!player.canUseGameMasterBlocks()) {
            return false;
        }

        if (player.level().isClientSide()) {
            player.openStructureBlock(this);
        }

        return true;
    }

    public String getStructureName() {
        return this.structureName == null ? "" : this.structureName.toString();
    }

    public boolean hasStructureName() {
        return this.structureName != null;
    }

    public void setStructureName(final @Nullable String structureName) {
        this.setStructureName(StringUtil.isNullOrEmpty(structureName) ? null : Identifier.tryParse(structureName));
    }

    public void setStructureName(final @Nullable Identifier structureName) {
        this.structureName = structureName;
    }

    public void createdBy(final LivingEntity creator) {
        this.author = creator.getPlainTextName();
    }

    public BlockPos getStructurePos() {
        return this.structurePos;
    }

    public void setStructurePos(final BlockPos structurePos) {
        this.structurePos = structurePos;
    }

    public Vec3i getStructureSize() {
        return this.structureSize;
    }

    public void setStructureSize(final Vec3i structureSize) {
        this.structureSize = structureSize;
    }

    public Mirror getMirror() {
        return this.mirror;
    }

    public void setMirror(final Mirror mirror) {
        this.mirror = mirror;
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public void setRotation(final Rotation rotation) {
        this.rotation = rotation;
    }

    public String getMetaData() {
        return this.metaData;
    }

    public void setMetaData(final String metaData) {
        this.metaData = metaData;
    }

    public StructureMode getMode() {
        return this.mode;
    }

    public void setMode(final StructureMode mode) {
        this.mode = mode;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (state.is(Blocks.STRUCTURE_BLOCK)) {
            this.level.setBlock(this.getBlockPos(), state.setValue(StructureBlock.MODE, mode), 2);
        }
    }

    public boolean isIgnoreEntities() {
        return this.ignoreEntities;
    }

    public boolean isStrict() {
        return this.strict;
    }

    public void setIgnoreEntities(final boolean ignoreEntities) {
        this.ignoreEntities = ignoreEntities;
    }

    public void setStrict(final boolean strict) {
        this.strict = strict;
    }

    public float getIntegrity() {
        return this.integrity;
    }

    public void setIntegrity(final float integrity) {
        this.integrity = integrity;
    }

    public long getSeed() {
        return this.seed;
    }

    public void setSeed(final long seed) {
        this.seed = seed;
    }

    public boolean detectSize() {
        if (this.mode != StructureMode.SAVE) {
            return false;
        }

        BlockPos pos = this.getBlockPos();
        int radius = 80;
        BlockPos corner1 = new BlockPos(pos.getX() - 80, this.level.getMinY(), pos.getZ() - 80);
        BlockPos corner2 = new BlockPos(pos.getX() + 80, this.level.getMaxY(), pos.getZ() + 80);
        Stream<BlockPos> relatedCorners = this.getRelatedCorners(corner1, corner2);
        return calculateEnclosingBoundingBox(pos, relatedCorners).filter(bb -> {
            int deltaX = bb.maxX() - bb.minX();
            int deltaY = bb.maxY() - bb.minY();
            int deltaZ = bb.maxZ() - bb.minZ();
            if (deltaX > 1 && deltaY > 1 && deltaZ > 1) {
                this.structurePos = new BlockPos(bb.minX() - pos.getX() + 1, bb.minY() - pos.getY() + 1, bb.minZ() - pos.getZ() + 1);
                this.structureSize = new Vec3i(deltaX - 1, deltaY - 1, deltaZ - 1);
                this.setChanged();
                BlockState state = this.level.getBlockState(pos);
                this.level.sendBlockUpdated(pos, state, state, 3);
                return true;
            } else {
                return false;
            }
        }).isPresent();
    }

    private Stream<BlockPos> getRelatedCorners(final BlockPos corner1, final BlockPos corner2) {
        return BlockPos.betweenClosedStream(corner1, corner2)
            .filter(pos -> this.level.getBlockState(pos).is(Blocks.STRUCTURE_BLOCK))
            .map(this.level::getBlockEntity)
            .filter(e -> e instanceof StructureBlockEntity)
            .map(e -> (StructureBlockEntity)e)
            .filter(input -> input.mode == StructureMode.CORNER && Objects.equals(this.structureName, input.structureName))
            .map(BlockEntity::getBlockPos);
    }

    private static Optional<BoundingBox> calculateEnclosingBoundingBox(final BlockPos pos, final Stream<BlockPos> relatedCorners) {
        Iterator<BlockPos> iterator = relatedCorners.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        }

        BlockPos firstCorner = iterator.next();
        BoundingBox result = new BoundingBox(firstCorner);
        if (iterator.hasNext()) {
            iterator.forEachRemaining(result::encapsulate);
        } else {
            result.encapsulate(pos);
        }

        return Optional.of(result);
    }

    public boolean saveStructure() {
        return this.mode != StructureMode.SAVE ? false : this.saveStructure(true);
    }

    public boolean saveStructure(final boolean saveToDisk) {
        if (this.structureName != null && this.level instanceof ServerLevel serverLevel) {
            BlockPos var4 = this.getBlockPos().offset(this.structurePos);
            return saveStructure(serverLevel, this.structureName, var4, this.structureSize, this.ignoreEntities, this.author, saveToDisk, List.of());
        } else {
            return false;
        }
    }

    public static boolean saveStructure(
        final ServerLevel level,
        final Identifier structureName,
        final BlockPos pos,
        final Vec3i structureSize,
        final boolean ignoreEntities,
        final String author,
        final boolean saveToDisk,
        final List<Block> ignoreBlocks
    ) {
        StructureTemplateManager manager = level.getStructureManager();

        StructureTemplate structureTemplate;
        try {
            structureTemplate = manager.getOrCreate(structureName);
        } catch (IdentifierException e) {
            return false;
        }

        structureTemplate.fillFromWorld(
            level, pos, structureSize, !ignoreEntities, Stream.concat(ignoreBlocks.stream(), Stream.of(Blocks.STRUCTURE_VOID)).toList()
        );
        structureTemplate.setAuthor(author);
        if (saveToDisk) {
            try {
                return manager.save(structureName);
            } catch (IdentifierException e) {
                return false;
            }
        } else {
            return true;
        }
    }

    public static RandomSource createRandom(final long seed) {
        return seed == 0L ? RandomSource.create(Util.getMillis()) : RandomSource.create(seed);
    }

    public boolean placeStructureIfSameSize(final ServerLevel level) {
        if (this.mode == StructureMode.LOAD && this.structureName != null) {
            StructureTemplate template = level.getStructureManager().get(this.structureName).orElse(null);
            if (template == null) {
                return false;
            } else if (template.getSize().equals(this.structureSize)) {
                this.placeStructure(level, template);
                return true;
            } else {
                this.loadStructureInfo(template);
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean loadStructureInfo(final ServerLevel level) {
        StructureTemplate template = this.getStructureTemplate(level);
        if (template == null) {
            return false;
        }

        this.loadStructureInfo(template);
        return true;
    }

    private void loadStructureInfo(final StructureTemplate structureTemplate) {
        this.author = !StringUtil.isNullOrEmpty(structureTemplate.getAuthor()) ? structureTemplate.getAuthor() : "";
        this.structureSize = structureTemplate.getSize();
        this.setChanged();
    }

    public void placeStructure(final ServerLevel level) {
        StructureTemplate template = this.getStructureTemplate(level);
        if (template != null) {
            this.placeStructure(level, template);
        }
    }

    private @Nullable StructureTemplate getStructureTemplate(final ServerLevel level) {
        return this.structureName == null ? null : level.getStructureManager().get(this.structureName).orElse(null);
    }

    private void placeStructure(final ServerLevel level, final StructureTemplate template) {
        this.loadStructureInfo(template);
        StructurePlaceSettings placeSettings = new StructurePlaceSettings()
            .setMirror(this.mirror)
            .setRotation(this.rotation)
            .setIgnoreEntities(this.ignoreEntities)
            .setKnownShape(this.strict);
        if (this.integrity < 1.0F) {
            placeSettings.clearProcessors().addProcessor(new BlockRotProcessor(Mth.clamp(this.integrity, 0.0F, 1.0F))).setRandom(createRandom(this.seed));
        }

        BlockPos pos = this.getBlockPos().offset(this.structurePos);
        if (SharedConstants.DEBUG_STRUCTURE_EDIT_MODE) {
            BlockPos.betweenClosed(pos, pos.offset(this.structureSize)).forEach(p -> level.setBlock(p, Blocks.STRUCTURE_VOID.defaultBlockState(), 2));
        }

        template.placeInWorld(level, pos, pos, placeSettings, createRandom(this.seed), 2 | (this.strict ? 816 : 0));
    }

    public void unloadStructure() {
        if (this.structureName != null) {
            ServerLevel serverLevel = (ServerLevel)this.level;
            StructureTemplateManager manager = serverLevel.getStructureManager();
            manager.remove(this.structureName);
        }
    }

    public boolean isStructureLoadable() {
        if (this.mode == StructureMode.LOAD && !this.level.isClientSide() && this.structureName != null) {
            ServerLevel serverLevel = (ServerLevel)this.level;
            StructureTemplateManager manager = serverLevel.getStructureManager();

            try {
                return manager.get(this.structureName).isPresent();
            } catch (IdentifierException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean isPowered() {
        return this.powered;
    }

    public void setPowered(final boolean powered) {
        this.powered = powered;
    }

    public boolean getShowAir() {
        return this.showAir;
    }

    public void setShowAir(final boolean showAir) {
        this.showAir = showAir;
    }

    public boolean getShowBoundingBox() {
        return this.showBoundingBox;
    }

    public void setShowBoundingBox(final boolean showBoundingBox) {
        this.showBoundingBox = showBoundingBox;
    }

    @Override
    public BoundingBoxRenderable.Mode renderMode() {
        if (this.mode != StructureMode.SAVE && this.mode != StructureMode.LOAD) {
            return BoundingBoxRenderable.Mode.NONE;
        } else if (this.mode == StructureMode.SAVE && this.showAir) {
            return BoundingBoxRenderable.Mode.BOX_AND_INVISIBLE_BLOCKS;
        } else {
            return this.mode != StructureMode.SAVE && !this.showBoundingBox ? BoundingBoxRenderable.Mode.NONE : BoundingBoxRenderable.Mode.BOX;
        }
    }

    @Override
    public BoundingBoxRenderable.RenderableBox getRenderableBox() {
        BlockPos pos = this.getStructurePos();
        Vec3i size = this.getStructureSize();
        int xOrigin = pos.getX();
        int zOrigin = pos.getZ();
        int y0 = pos.getY();
        int y1 = y0 + size.getY();
        int xDiff;
        int zDiff;
        switch (this.mirror) {
            case LEFT_RIGHT:
                xDiff = size.getX();
                zDiff = -size.getZ();
                break;
            case FRONT_BACK:
                xDiff = -size.getX();
                zDiff = size.getZ();
                break;
            default:
                xDiff = size.getX();
                zDiff = size.getZ();
        }

        int x0;
        int z0;
        int x1;
        int z1;
        switch (this.rotation) {
            case CLOCKWISE_90:
                x0 = zDiff < 0 ? xOrigin : xOrigin + 1;
                z0 = xDiff < 0 ? zOrigin + 1 : zOrigin;
                x1 = x0 - zDiff;
                z1 = z0 + xDiff;
                break;
            case CLOCKWISE_180:
                x0 = xDiff < 0 ? xOrigin : xOrigin + 1;
                z0 = zDiff < 0 ? zOrigin : zOrigin + 1;
                x1 = x0 - xDiff;
                z1 = z0 - zDiff;
                break;
            case COUNTERCLOCKWISE_90:
                x0 = zDiff < 0 ? xOrigin + 1 : xOrigin;
                z0 = xDiff < 0 ? zOrigin : zOrigin + 1;
                x1 = x0 + zDiff;
                z1 = z0 - xDiff;
                break;
            default:
                x0 = xDiff < 0 ? xOrigin + 1 : xOrigin;
                z0 = zDiff < 0 ? zOrigin + 1 : zOrigin;
                x1 = x0 + xDiff;
                z1 = z0 + zDiff;
        }

        return BoundingBoxRenderable.RenderableBox.fromCorners(x0, y0, z0, x1, y1, z1);
    }

    public enum UpdateType {
        UPDATE_DATA,
        SAVE_AREA,
        LOAD_AREA,
        SCAN_AREA;
    }
}