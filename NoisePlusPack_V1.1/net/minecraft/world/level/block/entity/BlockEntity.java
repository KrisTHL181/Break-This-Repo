package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class BlockEntity implements DebugValueSource {
   private static final Codec<BlockEntityType<?>> TYPE_CODEC = BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec();
   private static final Logger LOGGER = LogUtils.getLogger();
   private final BlockEntityType<?> type;
   protected @Nullable Level level;
   protected final BlockPos worldPosition;
   protected boolean remove;
   private BlockState blockState;
   private DataComponentMap components = DataComponentMap.EMPTY;

   public BlockEntity(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
      this.type = p_155228_;
      this.worldPosition = p_155229_.immutable();
      this.validateBlockState(p_155230_);
      this.blockState = p_155230_;
   }

   private void validateBlockState(BlockState p_345558_) {
      if (!this.isValidBlockState(p_345558_)) {
         throw new IllegalStateException("Invalid block entity " + this.getNameForReporting() + " state at " + this.worldPosition + ", got " + p_345558_);
      }
   }

   public boolean isValidBlockState(BlockState p_345570_) {
      return this.type.isValid(p_345570_);
   }

   public static BlockPos getPosFromTag(ChunkPos p_396083_, CompoundTag p_187473_) {
      int i = p_187473_.getIntOr("x", 0);
      int j = p_187473_.getIntOr("y", 0);
      int k = p_187473_.getIntOr("z", 0);
      int l = SectionPos.blockToSectionCoord(i);
      int i1 = SectionPos.blockToSectionCoord(k);
      if (l != p_396083_.x || i1 != p_396083_.z) {
         LOGGER.warn("Block entity {} found in a wrong chunk, expected position from chunk {}", p_187473_, p_396083_);
         i = p_396083_.getBlockX(SectionPos.sectionRelative(i));
         k = p_396083_.getBlockZ(SectionPos.sectionRelative(k));
      }

      return new BlockPos(i, j, k);
   }

   public @Nullable Level getLevel() {
      return this.level;
   }

   public void setLevel(Level p_155231_) {
      this.level = p_155231_;
   }

   public boolean hasLevel() {
      return this.level != null;
   }

   protected void loadAdditional(ValueInput p_409136_) {
   }

   public final void loadWithComponents(ValueInput p_409893_) {
      this.loadAdditional(p_409893_);
      this.components = p_409893_.<DataComponentMap>read("components", DataComponentMap.CODEC).orElse(DataComponentMap.EMPTY);
   }

   public final void loadCustomOnly(ValueInput p_408306_) {
      this.loadAdditional(p_408306_);
   }

   protected void saveAdditional(ValueOutput p_407573_) {
   }

   public final CompoundTag saveWithFullMetadata(HolderLookup.Provider p_331193_) {
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_331193_);
         this.saveWithFullMetadata(tagvalueoutput);
         return tagvalueoutput.buildResult();
      }
   }

   public void saveWithFullMetadata(ValueOutput p_406851_) {
      this.saveWithoutMetadata(p_406851_);
      this.saveMetadata(p_406851_);
   }

   public void saveWithId(ValueOutput p_406411_) {
      this.saveWithoutMetadata(p_406411_);
      this.saveId(p_406411_);
   }

   public final CompoundTag saveWithoutMetadata(HolderLookup.Provider p_332372_) {
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_332372_);
         this.saveWithoutMetadata(tagvalueoutput);
         return tagvalueoutput.buildResult();
      }
   }

   public void saveWithoutMetadata(ValueOutput p_406453_) {
      this.saveAdditional(p_406453_);
      p_406453_.store("components", DataComponentMap.CODEC, this.components);
   }

   public final CompoundTag saveCustomOnly(HolderLookup.Provider p_333091_) {
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_333091_);
         this.saveCustomOnly(tagvalueoutput);
         return tagvalueoutput.buildResult();
      }
   }

   public void saveCustomOnly(ValueOutput p_406911_) {
      this.saveAdditional(p_406911_);
   }

   private void saveId(ValueOutput p_409674_) {
      addEntityType(p_409674_, this.getType());
   }

   public static void addEntityType(ValueOutput p_409334_, BlockEntityType<?> p_187470_) {
      p_409334_.store("id", TYPE_CODEC, p_187470_);
   }

   private void saveMetadata(ValueOutput p_410055_) {
      this.saveId(p_410055_);
      p_410055_.putInt("x", this.worldPosition.getX());
      p_410055_.putInt("y", this.worldPosition.getY());
      p_410055_.putInt("z", this.worldPosition.getZ());
   }

   public static @Nullable BlockEntity loadStatic(BlockPos p_155242_, BlockState p_155243_, CompoundTag p_155244_, HolderLookup.Provider p_336084_) {
      BlockEntityType<?> blockentitytype = p_155244_.<BlockEntityType<?>>read("id", TYPE_CODEC).orElse(null);
      if (blockentitytype == null) {
         LOGGER.error("Skipping block entity with invalid type: {}", p_155244_.get("id"));
         return null;
      }

      BlockEntity blockentity;
      try {
         blockentity = blockentitytype.create(p_155242_, p_155243_);
      } catch (Throwable throwable2) {
         LOGGER.error("Failed to create block entity {} for block {} at position {} ", new Object[]{blockentitytype, p_155242_, p_155243_, throwable2});
         return null;
      }

      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(blockentity.problemPath(), LOGGER)) {
         blockentity.loadWithComponents(TagValueInput.create(problemreporter$scopedcollector, p_336084_, p_155244_));
         return blockentity;
      } catch (Throwable throwable1) {
         LOGGER.error("Failed to load data for block entity {} for block {} at position {}", new Object[]{blockentitytype, p_155242_, p_155243_, throwable1});
         return null;
      }
   }

   public void setChanged() {
      if (this.level != null) {
         setChanged(this.level, this.worldPosition, this.blockState);
      }
   }

   protected static void setChanged(Level p_155233_, BlockPos p_155234_, BlockState p_155235_) {
      p_155233_.blockEntityChanged(p_155234_);
      if (!p_155235_.isAir()) {
         p_155233_.updateNeighbourForOutputSignal(p_155234_, p_155235_.getBlock());
      }
   }

   public BlockPos getBlockPos() {
      return this.worldPosition;
   }

   public BlockState getBlockState() {
      return this.blockState;
   }

   public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
      return null;
   }

   public CompoundTag getUpdateTag(HolderLookup.Provider p_329179_) {
      return new CompoundTag();
   }

   public boolean isRemoved() {
      return this.remove;
   }

   public void setRemoved() {
      this.remove = true;
   }

   public void clearRemoved() {
      this.remove = false;
   }

   public void preRemoveSideEffects(BlockPos p_397404_, BlockState p_395805_) {
      if (this instanceof Container container && this.level != null) {
         Containers.dropContents(this.level, p_397404_, container);
      }
   }

   public boolean triggerEvent(int p_58889_, int p_58890_) {
      return false;
   }

   public void fillCrashReportCategory(CrashReportCategory p_58887_) {
      p_58887_.setDetail("Name", this::getNameForReporting);
      p_58887_.setDetail("Cached block", this.getBlockState()::toString);
      if (this.level == null) {
         p_58887_.setDetail("Block location", () -> this.worldPosition + " (world missing)");
      } else {
         p_58887_.setDetail("Actual block", this.level.getBlockState(this.worldPosition)::toString);
         CrashReportCategory.populateBlockLocationDetails(p_58887_, this.level, this.worldPosition);
      }
   }

   public String getNameForReporting() {
      return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.getType()) + " // " + this.getClass().getCanonicalName();
   }

   public BlockEntityType<?> getType() {
      return this.type;
   }

   @Deprecated
   public void setBlockState(BlockState p_155251_) {
      this.validateBlockState(p_155251_);
      this.blockState = p_155251_;
   }

   protected void applyImplicitComponents(DataComponentGetter p_391290_) {
   }

   public final void applyComponentsFromItemStack(ItemStack p_328941_) {
      this.applyComponents(p_328941_.getPrototype(), p_328941_.getComponentsPatch());
   }

   public final void applyComponents(DataComponentMap p_335232_, DataComponentPatch p_331646_) {
      final Set<DataComponentType<?>> set = new HashSet<>();
      set.add(DataComponents.BLOCK_ENTITY_DATA);
      set.add(DataComponents.BLOCK_STATE);
      final DataComponentMap datacomponentmap = PatchedDataComponentMap.fromPatch(p_335232_, p_331646_);
      this.applyImplicitComponents(new DataComponentGetter() {
         @Override
         public <T> @Nullable T get(DataComponentType<? extends T> p_335233_) {
            set.add(p_335233_);
            return datacomponentmap.get(p_335233_);
         }

         @Override
         public <T> T getOrDefault(DataComponentType<? extends T> p_334887_, T p_333244_) {
            set.add(p_334887_);
            return datacomponentmap.getOrDefault(p_334887_, p_333244_);
         }
      });
      DataComponentPatch datacomponentpatch = p_331646_.forget(set::contains);
      this.components = datacomponentpatch.split().added();
   }

   protected void collectImplicitComponents(DataComponentMap.Builder p_328216_) {
   }

   @Deprecated
   public void removeComponentsFromTag(ValueOutput p_408661_) {
   }

   public final DataComponentMap collectComponents() {
      DataComponentMap.Builder datacomponentmap$builder = DataComponentMap.builder();
      datacomponentmap$builder.addAll(this.components);
      this.collectImplicitComponents(datacomponentmap$builder);
      return datacomponentmap$builder.build();
   }

   public DataComponentMap components() {
      return this.components;
   }

   public void setComponents(DataComponentMap p_335672_) {
      this.components = p_335672_;
   }

   public static @Nullable Component parseCustomNameSafe(ValueInput p_408442_, String p_410488_) {
      return p_408442_.<Component>read(p_410488_, ComponentSerialization.CODEC).orElse(null);
   }

   public ProblemReporter.PathElement problemPath() {
      return new BlockEntity.BlockEntityPathElement(this);
   }

   @Override
   public void registerDebugValues(ServerLevel p_427460_, DebugValueSource.Registration p_424498_) {
   }

   record BlockEntityPathElement(BlockEntity blockEntity) implements ProblemReporter.PathElement {
      @Override
      public String get() {
         return this.blockEntity.getNameForReporting() + "@" + this.blockEntity.getBlockPos();
      }
   }
}
