package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class DropperBlock extends DispenserBlock {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final DispenseItemBehavior DISPENSE_BEHAVIOUR = new DefaultDispenseItemBehavior();

   public DropperBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   protected DispenseItemBehavior getDispenseMethod(final Level level, final ItemStack itemStack) {
      return DISPENSE_BEHAVIOUR;
   }

   @Override
   public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new DropperBlockEntity(worldPosition, blockState);
   }

   @Override
   protected void dispenseFrom(final ServerLevel level, final BlockState state, final BlockPos pos) {
      DispenserBlockEntity blockEntity = level.getBlockEntity(pos, BlockEntityTypes.DROPPER).orElse(null);
      if (blockEntity == null) {
         LOGGER.warn("Ignoring dispensing attempt for Dropper without matching block entity at {}", pos);
      } else {
         BlockSource source = new BlockSource(level, pos, state, blockEntity);
         int slot = blockEntity.getRandomSlot(level.getRandom());
         if (slot < 0) {
            level.levelEvent(1001, pos, 0);
         } else {
            ItemStack itemStack = blockEntity.getItem(slot);
            if (!itemStack.isEmpty()) {
               Direction direction = level.getBlockState(pos).getValue(FACING);
               Container into = HopperBlockEntity.getContainerAt(level, pos.relative(direction));
               ItemStack remaining;
               if (into == null) {
                  remaining = DISPENSE_BEHAVIOUR.dispense(source, itemStack);
               } else {
                  remaining = HopperBlockEntity.addItem(blockEntity, into, itemStack.copyWithCount(1), direction.getOpposite());
                  if (remaining.isEmpty()) {
                     remaining = itemStack.copy();
                     remaining.shrink(1);
                  } else {
                     remaining = itemStack.copy();
                  }
               }

               blockEntity.setItem(slot, remaining);
            }
         }
      }
   }
}
