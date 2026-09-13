package net.minecraft.commands.arguments.blocks;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class BlockInput implements Predicate<BlockInWorld> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final BlockState state;
   private final Set<Property<?>> properties;
   private final @Nullable CompoundTag tag;

   public BlockInput(BlockState p_114666_, Set<Property<?>> p_114667_, @Nullable CompoundTag p_114668_) {
      this.state = p_114666_;
      this.properties = p_114667_;
      this.tag = p_114668_;
   }

   public BlockState getState() {
      return this.state;
   }

   public Set<Property<?>> getDefinedProperties() {
      return this.properties;
   }

   public boolean test(BlockInWorld p_114675_) {
      BlockState blockstate = p_114675_.getState();
      if (!blockstate.is(this.state.getBlock())) {
         return false;
      }

      for (Property<?> property : this.properties) {
         if (blockstate.getValue(property) != this.state.getValue(property)) {
            return false;
         }
      }

      if (this.tag == null) {
         return true;
      }

      BlockEntity blockentity = p_114675_.getEntity();
      return blockentity != null && NbtUtils.compareNbt(this.tag, blockentity.saveWithFullMetadata(p_114675_.getLevel().registryAccess()), true);
   }

   public boolean test(ServerLevel p_173524_, BlockPos p_173525_) {
      return this.test(new BlockInWorld(p_173524_, p_173525_, false));
   }

   public boolean place(ServerLevel p_114671_, BlockPos p_114672_, @Block.UpdateFlags int p_114673_) {
      BlockState blockstate = (p_114673_ & 16) != 0 ? this.state : Block.updateFromNeighbourShapes(this.state, p_114671_, p_114672_);
      if (blockstate.isAir()) {
         blockstate = this.state;
      }

      blockstate = this.overwriteWithDefinedProperties(blockstate);
      boolean flag = false;
      if (p_114671_.setBlock(p_114672_, blockstate, p_114673_)) {
         flag = true;
      }

      if (this.tag != null) {
         BlockEntity blockentity = p_114671_.getBlockEntity(p_114672_);
         if (blockentity != null) {
            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
               HolderLookup.Provider holderlookup$provider = p_114671_.registryAccess();
               ProblemReporter problemreporter = problemreporter$scopedcollector.forChild(blockentity.problemPath());
               TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter.forChild(() -> "(before)"), holderlookup$provider);
               blockentity.saveWithoutMetadata(tagvalueoutput);
               CompoundTag compoundtag = tagvalueoutput.buildResult();
               blockentity.loadWithComponents(TagValueInput.create(problemreporter$scopedcollector, holderlookup$provider, this.tag));
               TagValueOutput tagvalueoutput1 = TagValueOutput.createWithContext(problemreporter.forChild(() -> "(after)"), holderlookup$provider);
               blockentity.saveWithoutMetadata(tagvalueoutput1);
               CompoundTag compoundtag1 = tagvalueoutput1.buildResult();
               if (!compoundtag1.equals(compoundtag)) {
                  flag = true;
                  blockentity.setChanged();
                  p_114671_.getChunkSource().blockChanged(p_114672_);
               }
            }
         }
      }

      return flag;
   }

   private BlockState overwriteWithDefinedProperties(BlockState p_376464_) {
      if (p_376464_ == this.state) {
         return p_376464_;
      }

      for (Property<?> property : this.properties) {
         p_376464_ = copyProperty(p_376464_, this.state, property);
      }

      return p_376464_;
   }

   private static <T extends Comparable<T>> BlockState copyProperty(BlockState p_377223_, BlockState p_377871_, Property<T> p_378516_) {
      return p_377223_.trySetValue(p_378516_, p_377871_.getValue(p_378516_));
   }
}
