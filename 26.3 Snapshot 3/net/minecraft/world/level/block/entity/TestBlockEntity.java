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

   public TestBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.TEST_BLOCK, worldPosition, blockState);
      this.mode = blockState.getValue(TestBlock.MODE);
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      output.store("mode", TestBlockMode.CODEC, this.mode);
      output.putString("message", this.message);
      output.putBoolean("powered", this.powered);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      this.mode = input.<TestBlockMode>read("mode", TestBlockMode.CODEC).orElse(TestBlockMode.FAIL);
      this.message = input.getStringOr("message", "");
      this.powered = input.getBooleanOr("powered", false);
   }

   private void updateBlockState() {
      if (this.level != null) {
         BlockPos pos = this.getBlockPos();
         BlockState blockState = this.level.getBlockState(pos);
         if (blockState.is(Blocks.TEST_BLOCK)) {
            this.level.setBlock(pos, blockState.setValue(TestBlock.MODE, this.mode), 2);
         }
      }
   }

   public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public boolean isPowered() {
      return this.powered;
   }

   public void setPowered(final boolean powered) {
      this.powered = powered;
   }

   public TestBlockMode getMode() {
      return this.mode;
   }

   public void setMode(final TestBlockMode mode) {
      this.mode = mode;
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
         BlockPos pos = this.getBlockPos();
         this.level.updateNeighborsAt(pos, this.getBlockType());
         this.level.getBlockTicks().willTickThisTick(pos, this.getBlockType());
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

   public void setMessage(final String message) {
      this.message = message;
   }
}
