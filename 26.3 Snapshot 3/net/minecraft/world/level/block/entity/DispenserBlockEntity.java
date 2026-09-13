package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DispenserBlockEntity extends RandomizableContainerBlockEntity {
   public static final int CONTAINER_SIZE = 9;
   private static final Component DEFAULT_NAME = Component.translatable("container.dispenser");
   private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

   protected DispenserBlockEntity(final BlockEntityType<?> type, final BlockPos worldPosition, final BlockState blockState) {
      super(type, worldPosition, blockState);
   }

   public DispenserBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      this(BlockEntityTypes.DISPENSER, worldPosition, blockState);
   }

   @Override
   public int getContainerSize() {
      return 9;
   }

   public int getRandomSlot(final RandomSource random) {
      this.unpackLootTable(null);
      int replaceSlot = -1;
      int replaceOdds = 1;

      for (int i = 0; i < this.items.size(); i++) {
         if (!this.items.get(i).isEmpty() && random.nextInt(replaceOdds++) == 0) {
            replaceSlot = i;
         }
      }

      return replaceSlot;
   }

   public ItemStack insertItem(final ItemStack itemStack) {
      int maxStackSize = this.getMaxStackSize(itemStack);

      for (int i = 0; i < this.items.size(); i++) {
         ItemStack targetStack = this.items.get(i);
         if (targetStack.isEmpty() || ItemStack.isSameItemSameComponents(itemStack, targetStack)) {
            int transferCount = Math.min(itemStack.getCount(), maxStackSize - targetStack.getCount());
            if (transferCount > 0) {
               if (targetStack.isEmpty()) {
                  this.setItem(i, itemStack.split(transferCount));
               } else {
                  itemStack.shrink(transferCount);
                  targetStack.grow(transferCount);
               }
            }

            if (itemStack.isEmpty()) {
               break;
            }
         }
      }

      return itemStack;
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(input)) {
         ContainerHelper.loadAllItems(input, this.items);
      }
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      if (!this.trySaveLootTable(output)) {
         ContainerHelper.saveAllItems(output, this.items);
      }
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void setItems(final NonNullList<ItemStack> items) {
      this.items = items;
   }

   @Override
   protected AbstractContainerMenu createMenu(final int containerId, final Inventory inventory) {
      return new DispenserMenu(containerId, inventory, this);
   }
}
