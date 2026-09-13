package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ComparatorBlockEntity extends BlockEntity {
   private static final int DEFAULT_OUTPUT = 0;
   private int output = 0;

   public ComparatorBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.COMPARATOR, worldPosition, blockState);
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      output.putInt("OutputSignal", this.output);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.output = input.getIntOr("OutputSignal", 0);
   }

   public int getOutputSignal() {
      return this.output;
   }

   public void setOutputSignal(final int value) {
      this.output = value;
   }
}
