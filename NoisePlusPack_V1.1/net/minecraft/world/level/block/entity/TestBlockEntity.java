package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.TestBlockMode;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TestBlockEntity extends BlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String DEFAULT_MESSAGE = "";
   private static final boolean DEFAULT_POWERED = false;
   private TestBlockMode mode;
   private String message = "";
   private boolean powered = false;
   private boolean triggered;

   public TestBlockEntity(BlockPos p_394476_, BlockState p_394354_) {
      super(BlockEntityType.TEST_BLOCK, p_394476_, p_394354_);
      this.mode = p_394354_.getValue(TestBlock.MODE);
   }

   @Override
   protected void saveAdditional(ValueOutput p_407256_) {
      p_407256_.store("mode", TestBlockMode.CODEC, this.mode);
      p_407256_.putString("message", this.message);
      p_407256_.putBoolean("powered", this.powered);
   }

   @Override
   protected void loadAdditional(ValueInput p_410586_) {
      this.mode = p_410586_.<TestBlockMode>read("mode", TestBlockMode.CODEC).orElse(TestBlockMode.FAIL);
      this.message = p_410586_.getStringOr("message", "");
      this.powered = p_410586_.getBooleanOr("powered", false);
   }

   private void updateBlockState() {
      if (this.level != null) {
         BlockPos blockpos = this.getBlockPos();
         BlockState blockstate = this.level.getBlockState(blockpos);
         if (blockstate.is(Blocks.TEST_BLOCK)) {
            this.level.setBlock(blockpos, blockstate.setValue(TestBlock.MODE, this.mode), 2);
         }
      }
   }

   public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_395613_) {
      return this.saveCustomOnly(p_395613_);
   }

   public boolean isPowered() {
      return this.powered;
   }

   public void setPowered(boolean p_392131_) {
      this.powered = p_392131_;
   }

   public TestBlockMode getMode() {
      return this.mode;
   }

   public void setMode(TestBlockMode p_394469_) {
      this.mode = p_394469_;
      this.updateBlockState();
   }

   private Block getBlockType() {
      return this.getBlockState().getBlock();
   }

   public void reset() {
      this.triggered = false;
      if (this.mode == TestBlockMode.START && this.level != null) {
         this.setPowered(false);
         this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockType());
      }
   }

   public void trigger() {
      if (this.mode == TestBlockMode.START && this.level != null) {
         this.setPowered(true);
         BlockPos blockpos = this.getBlockPos();
         this.level.updateNeighborsAt(blockpos, this.getBlockType());
         this.level.getBlockTicks().willTickThisTick(blockpos, this.getBlockType());
         this.log();
      } else {
         if (this.mode == TestBlockMode.LOG) {
            this.log();
         }

         this.triggered = true;
      }
   }

   public void log() {
      if (!this.message.isBlank()) {
         LOGGER.info("Test {} (at {}): {}", new Object[]{this.mode.getSerializedName(), this.getBlockPos(), this.message});
      }
   }

   public boolean hasTriggered() {
      return this.triggered;
   }

   public String getMessage() {
      return this.message;
   }

   public void setMessage(String p_394794_) {
      this.message = p_394794_;
   }
}
