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

   public StructureBlockEntity(BlockPos p_155779_, BlockState p_155780_) {
      super(BlockEntityType.STRUCTURE_BLOCK, p_155779_, p_155780_);
      this.mode = p_155780_.getValue(StructureBlock.MODE);
   }

   @Override
   protected void saveAdditional(ValueOutput p_408669_) {
      super.saveAdditional(p_408669_);
      p_408669_.putString("name", this.getStructureName());
      p_408669_.putString("author", this.author);
      p_408669_.putString("metadata", this.metaData);
      p_408669_.putInt("posX", this.structurePos.getX());
      p_408669_.putInt("posY", this.structurePos.getY());
      p_408669_.putInt("posZ", this.structurePos.getZ());
      p_408669_.putInt("sizeX", this.structureSize.getX());
      p_408669_.putInt("sizeY", this.structureSize.getY());
      p_408669_.putInt("sizeZ", this.structureSize.getZ());
      p_408669_.store("rotation", Rotation.LEGACY_CODEC, this.rotation);
      p_408669_.store("mirror", Mirror.LEGACY_CODEC, this.mirror);
      p_408669_.store("mode", StructureMode.LEGACY_CODEC, this.mode);
      p_408669_.putBoolean("ignoreEntities", this.ignoreEntities);
      p_408669_.putBoolean("strict", this.strict);
      p_408669_.putBoolean("powered", this.powered);
      p_408669_.putBoolean("showair", this.showAir);
      p_408669_.putBoolean("showboundingbox", this.showBoundingBox);
      p_408669_.putFloat("integrity", this.integrity);
      p_408669_.putLong("seed", this.seed);
   }

   @Override
   protected void loadAdditional(ValueInput p_409079_) {
      super.loadAdditional(p_409079_);
      this.setStructureName(p_409079_.getStringOr("name", ""));
      this.author = p_409079_.getStringOr("author", "");
      this.metaData = p_409079_.getStringOr("metadata", "");
      int i = Mth.clamp(p_409079_.getIntOr("posX", DEFAULT_POS.getX()), -48, 48);
      int j = Mth.clamp(p_409079_.getIntOr("posY", DEFAULT_POS.getY()), -48, 48);
      int k = Mth.clamp(p_409079_.getIntOr("posZ", DEFAULT_POS.getZ()), -48, 48);
      this.structurePos = new BlockPos(i, j, k);
      int l = Mth.clamp(p_409079_.getIntOr("sizeX", DEFAULT_SIZE.getX()), 0, 48);
      int i1 = Mth.clamp(p_409079_.getIntOr("sizeY", DEFAULT_SIZE.getY()), 0, 48);
      int j1 = Mth.clamp(p_409079_.getIntOr("sizeZ", DEFAULT_SIZE.getZ()), 0, 48);
      this.structureSize = new Vec3i(l, i1, j1);
      this.rotation = p_409079_.<Rotation>read("rotation", Rotation.LEGACY_CODEC).orElse(DEFAULT_ROTATION);
      this.mirror = p_409079_.<Mirror>read("mirror", Mirror.LEGACY_CODEC).orElse(DEFAULT_MIRROR);
      this.mode = p_409079_.<StructureMode>read("mode", StructureMode.LEGACY_CODEC).orElse(StructureMode.DATA);
      this.ignoreEntities = p_409079_.getBooleanOr("ignoreEntities", true);
      this.strict = p_409079_.getBooleanOr("strict", false);
      this.powered = p_409079_.getBooleanOr("powered", false);
      this.showAir = p_409079_.getBooleanOr("showair", false);
      this.showBoundingBox = p_409079_.getBooleanOr("showboundingbox", true);
      this.integrity = p_409079_.getFloatOr("integrity", 1.0F);
      this.seed = p_409079_.getLongOr("seed", 0L);
      this.updateBlockState();
   }

   private void updateBlockState() {
      if (this.level != null) {
         BlockPos blockpos = this.getBlockPos();
         BlockState blockstate = this.level.getBlockState(blockpos);
         if (blockstate.is(Blocks.STRUCTURE_BLOCK)) {
            this.level.setBlock(blockpos, blockstate.setValue(StructureBlock.MODE, this.mode), 2);
         }
      }
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_327713_) {
      return this.saveCustomOnly(p_327713_);
   }

   public boolean usedBy(Player p_59854_) {
      if (!p_59854_.canUseGameMasterBlocks()) {
         return false;
      }

      if (p_59854_.level().isClientSide()) {
         p_59854_.openStructureBlock(this);
      }

      return true;
   }

   public String getStructureName() {
      return this.structureName == null ? "" : this.structureName.toString();
   }

   public boolean hasStructureName() {
      return this.structureName != null;
   }

   public void setStructureName(@Nullable String p_59869_) {
      this.setStructureName(StringUtil.isNullOrEmpty(p_59869_) ? null : Identifier.tryParse(p_59869_));
   }

   public void setStructureName(@Nullable Identifier p_459570_) {
      this.structureName = p_459570_;
   }

   public void createdBy(LivingEntity p_59852_) {
      this.author = p_59852_.getPlainTextName();
   }

   public BlockPos getStructurePos() {
      return this.structurePos;
   }

   public void setStructurePos(BlockPos p_59886_) {
      this.structurePos = p_59886_;
   }

   public Vec3i getStructureSize() {
      return this.structureSize;
   }

   public void setStructureSize(Vec3i p_155798_) {
      this.structureSize = p_155798_;
   }

   public Mirror getMirror() {
      return this.mirror;
   }

   public void setMirror(Mirror p_59882_) {
      this.mirror = p_59882_;
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public void setRotation(Rotation p_59884_) {
      this.rotation = p_59884_;
   }

   public String getMetaData() {
      return this.metaData;
   }

   public void setMetaData(String p_59888_) {
      this.metaData = p_59888_;
   }

   public StructureMode getMode() {
      return this.mode;
   }

   public void setMode(StructureMode p_59861_) {
      this.mode = p_59861_;
      BlockState blockstate = this.level.getBlockState(this.getBlockPos());
      if (blockstate.is(Blocks.STRUCTURE_BLOCK)) {
         this.level.setBlock(this.getBlockPos(), blockstate.setValue(StructureBlock.MODE, p_59861_), 2);
      }
   }

   public boolean isIgnoreEntities() {
      return this.ignoreEntities;
   }

   public boolean isStrict() {
      return this.strict;
   }

   public void setIgnoreEntities(boolean p_59877_) {
      this.ignoreEntities = p_59877_;
   }

   public void setStrict(boolean p_394300_) {
      this.strict = p_394300_;
   }

   public float getIntegrity() {
      return this.integrity;
   }

   public void setIntegrity(float p_59839_) {
      this.integrity = p_59839_;
   }

   public long getSeed() {
      return this.seed;
   }

   public void setSeed(long p_59841_) {
      this.seed = p_59841_;
   }

   public boolean detectSize() {
      if (this.mode != StructureMode.SAVE) {
         return false;
      }

      BlockPos blockpos = this.getBlockPos();
      int i = 80;
      BlockPos blockpos1 = new BlockPos(blockpos.getX() - 80, this.level.getMinY(), blockpos.getZ() - 80);
      BlockPos blockpos2 = new BlockPos(blockpos.getX() + 80, this.level.getMaxY(), blockpos.getZ() + 80);
      Stream<BlockPos> stream = this.getRelatedCorners(blockpos1, blockpos2);
      return calculateEnclosingBoundingBox(blockpos, stream)
         .filter(
            p_155790_ -> {
               int j = p_155790_.maxX() - p_155790_.minX();
               int k = p_155790_.maxY() - p_155790_.minY();
               int l = p_155790_.maxZ() - p_155790_.minZ();
               if (j > 1 && k > 1 && l > 1) {
                  this.structurePos = new BlockPos(
                     p_155790_.minX() - blockpos.getX() + 1, p_155790_.minY() - blockpos.getY() + 1, p_155790_.minZ() - blockpos.getZ() + 1
                  );
                  this.structureSize = new Vec3i(j - 1, k - 1, l - 1);
                  this.setChanged();
                  BlockState blockstate = this.level.getBlockState(blockpos);
                  this.level.sendBlockUpdated(blockpos, blockstate, blockstate, 3);
                  return true;
               } else {
                  return false;
               }
            }
         )
         .isPresent();
   }

   private Stream<BlockPos> getRelatedCorners(BlockPos p_155792_, BlockPos p_155793_) {
      return BlockPos.betweenClosedStream(p_155792_, p_155793_)
         .filter(p_272561_ -> this.level.getBlockState(p_272561_).is(Blocks.STRUCTURE_BLOCK))
         .map(this.level::getBlockEntity)
         .filter(p_155802_ -> p_155802_ instanceof StructureBlockEntity)
         .map(p_155785_ -> (StructureBlockEntity)p_155785_)
         .filter(p_449928_ -> p_449928_.mode == StructureMode.CORNER && Objects.equals(this.structureName, p_449928_.structureName))
         .map(BlockEntity::getBlockPos);
   }

   private static Optional<BoundingBox> calculateEnclosingBoundingBox(BlockPos p_155795_, Stream<BlockPos> p_155796_) {
      Iterator<BlockPos> iterator = p_155796_.iterator();
      if (!iterator.hasNext()) {
         return Optional.empty();
      }

      BlockPos blockpos = iterator.next();
      BoundingBox boundingbox = new BoundingBox(blockpos);
      if (iterator.hasNext()) {
         iterator.forEachRemaining(boundingbox::encapsulate);
      } else {
         boundingbox.encapsulate(p_155795_);
      }

      return Optional.of(boundingbox);
   }

   public boolean saveStructure() {
      return this.mode != StructureMode.SAVE ? false : this.saveStructure(true);
   }

   public boolean saveStructure(boolean p_59890_) {
      if (this.structureName != null && this.level instanceof ServerLevel serverlevel) {
         BlockPos blockpos = this.getBlockPos().offset(this.structurePos);
         return saveStructure(serverlevel, this.structureName, blockpos, this.structureSize, this.ignoreEntities, this.author, p_59890_, List.of());
      } else {
         return false;
      }
   }

   public static boolean saveStructure(
      ServerLevel p_394863_,
      Identifier p_451746_,
      BlockPos p_397553_,
      Vec3i p_395544_,
      boolean p_397953_,
      String p_394234_,
      boolean p_396341_,
      List<Block> p_408149_
   ) {
      StructureTemplateManager structuretemplatemanager = p_394863_.getStructureManager();

      StructureTemplate structuretemplate;
      try {
         structuretemplate = structuretemplatemanager.getOrCreate(p_451746_);
      } catch (IdentifierException identifierexception1) {
         return false;
      }

      structuretemplate.fillFromWorld(p_394863_, p_397553_, p_395544_, !p_397953_, Stream.concat(p_408149_.stream(), Stream.of(Blocks.STRUCTURE_VOID)).toList());
      structuretemplate.setAuthor(p_394234_);
      if (p_396341_) {
         try {
            return structuretemplatemanager.save(p_451746_);
         } catch (IdentifierException identifierexception) {
            return false;
         }
      } else {
         return true;
      }
   }

   public static RandomSource createRandom(long p_222889_) {
      return p_222889_ == 0L ? RandomSource.create(Util.getMillis()) : RandomSource.create(p_222889_);
   }

   public boolean placeStructureIfSameSize(ServerLevel p_310062_) {
      if (this.mode == StructureMode.LOAD && this.structureName != null) {
         StructureTemplate structuretemplate = p_310062_.getStructureManager().get(this.structureName).orElse(null);
         if (structuretemplate == null) {
            return false;
         } else if (structuretemplate.getSize().equals(this.structureSize)) {
            this.placeStructure(p_310062_, structuretemplate);
            return true;
         } else {
            this.loadStructureInfo(structuretemplate);
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean loadStructureInfo(ServerLevel p_312602_) {
      StructureTemplate structuretemplate = this.getStructureTemplate(p_312602_);
      if (structuretemplate == null) {
         return false;
      }

      this.loadStructureInfo(structuretemplate);
      return true;
   }

   private void loadStructureInfo(StructureTemplate p_311753_) {
      this.author = !StringUtil.isNullOrEmpty(p_311753_.getAuthor()) ? p_311753_.getAuthor() : "";
      this.structureSize = p_311753_.getSize();
      this.setChanged();
   }

   public void placeStructure(ServerLevel p_312292_) {
      StructureTemplate structuretemplate = this.getStructureTemplate(p_312292_);
      if (structuretemplate != null) {
         this.placeStructure(p_312292_, structuretemplate);
      }
   }

   private @Nullable StructureTemplate getStructureTemplate(ServerLevel p_310290_) {
      return this.structureName == null ? null : p_310290_.getStructureManager().get(this.structureName).orElse(null);
   }

   private void placeStructure(ServerLevel p_311121_, StructureTemplate p_312324_) {
      this.loadStructureInfo(p_312324_);
      StructurePlaceSettings structureplacesettings = new StructurePlaceSettings()
         .setMirror(this.mirror)
         .setRotation(this.rotation)
         .setIgnoreEntities(this.ignoreEntities)
         .setKnownShape(this.strict);
      if (this.integrity < 1.0F) {
         structureplacesettings.clearProcessors().addProcessor(new BlockRotProcessor(Mth.clamp(this.integrity, 0.0F, 1.0F))).setRandom(createRandom(this.seed));
      }

      BlockPos blockpos = this.getBlockPos().offset(this.structurePos);
      if (SharedConstants.DEBUG_STRUCTURE_EDIT_MODE) {
         BlockPos.betweenClosed(blockpos, blockpos.offset(this.structureSize))
            .forEach(p_155783_ -> p_311121_.setBlock(p_155783_, Blocks.STRUCTURE_VOID.defaultBlockState(), 2));
      }

      p_312324_.placeInWorld(p_311121_, blockpos, blockpos, structureplacesettings, createRandom(this.seed), 2 | (this.strict ? 816 : 0));
   }

   public void unloadStructure() {
      if (this.structureName != null) {
         ServerLevel serverlevel = (ServerLevel)this.level;
         StructureTemplateManager structuretemplatemanager = serverlevel.getStructureManager();
         structuretemplatemanager.remove(this.structureName);
      }
   }

   public boolean isStructureLoadable() {
      if (this.mode == StructureMode.LOAD && !this.level.isClientSide() && this.structureName != null) {
         ServerLevel serverlevel = (ServerLevel)this.level;
         StructureTemplateManager structuretemplatemanager = serverlevel.getStructureManager();

         try {
            return structuretemplatemanager.get(this.structureName).isPresent();
         } catch (IdentifierException identifierexception) {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean isPowered() {
      return this.powered;
   }

   public void setPowered(boolean p_59894_) {
      this.powered = p_59894_;
   }

   public boolean getShowAir() {
      return this.showAir;
   }

   public void setShowAir(boolean p_59897_) {
      this.showAir = p_59897_;
   }

   public boolean getShowBoundingBox() {
      return this.showBoundingBox;
   }

   public void setShowBoundingBox(boolean p_59899_) {
      this.showBoundingBox = p_59899_;
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
      BlockPos blockpos = this.getStructurePos();
      Vec3i vec3i = this.getStructureSize();
      int i = blockpos.getX();
      int j = blockpos.getZ();
      int j1 = blockpos.getY();
      int i2 = j1 + vec3i.getY();
      int k;
      int l;
      switch (this.mirror) {
         case LEFT_RIGHT:
            k = vec3i.getX();
            l = -vec3i.getZ();
            break;
         case FRONT_BACK:
            k = -vec3i.getX();
            l = vec3i.getZ();
            break;
         default:
            k = vec3i.getX();
            l = vec3i.getZ();
      }

      int i1;
      int k1;
      int l1;
      int j2;
      switch (this.rotation) {
         case CLOCKWISE_90:
            i1 = l < 0 ? i : i + 1;
            k1 = k < 0 ? j + 1 : j;
            l1 = i1 - l;
            j2 = k1 + k;
            break;
         case CLOCKWISE_180:
            i1 = k < 0 ? i : i + 1;
            k1 = l < 0 ? j : j + 1;
            l1 = i1 - k;
            j2 = k1 - l;
            break;
         case COUNTERCLOCKWISE_90:
            i1 = l < 0 ? i + 1 : i;
            k1 = k < 0 ? j : j + 1;
            l1 = i1 + l;
            j2 = k1 - k;
            break;
         default:
            i1 = k < 0 ? i + 1 : i;
            k1 = l < 0 ? j + 1 : j;
            l1 = i1 + k;
            j2 = k1 + l;
      }

      return BoundingBoxRenderable.RenderableBox.fromCorners(i1, j1, k1, l1, i2, j2);
   }

   public enum UpdateType {
      UPDATE_DATA,
      SAVE_AREA,
      LOAD_AREA,
      SCAN_AREA;
   }
}
