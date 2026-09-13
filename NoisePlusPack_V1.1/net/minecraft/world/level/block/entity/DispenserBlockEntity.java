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

   protected DispenserBlockEntity(BlockEntityType<?> p_155489_, BlockPos p_155490_, BlockState p_155491_) {
      super(p_155489_, p_155490_, p_155491_);
   }

   public DispenserBlockEntity(BlockPos p_155493_, BlockState p_155494_) {
      this(BlockEntityType.DISPENSER, p_155493_, p_155494_);
   }

   @Override
   public int getContainerSize() {
      return 9;
   }

   public int getRandomSlot(RandomSource p_222762_) {
      this.unpackLootTable(null);
      int i = -1;
      int j = 1;

      for (int k = 0; k < this.items.size(); k++) {
         if (!this.items.get(k).isEmpty() && p_222762_.nextInt(j++) == 0) {
            i = k;
         }
      }

      return i;
   }

   public ItemStack insertItem(ItemStack p_345441_) {
      int i = this.getMaxStackSize(p_345441_);

      for (int j = 0; j < this.items.size(); j++) {
         ItemStack itemstack = this.items.get(j);
         if (itemstack.isEmpty() || ItemStack.isSameItemSameComponents(p_345441_, itemstack)) {
            int k = Math.min(p_345441_.getCount(), i - itemstack.getCount());
            if (k > 0) {
               if (itemstack.isEmpty()) {
                  this.setItem(j, p_345441_.split(k));
               } else {
                  p_345441_.shrink(k);
                  itemstack.grow(k);
               }
            }

            if (p_345441_.isEmpty()) {
               break;
            }
         }
      }

      return p_345441_;
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   @Override
   protected void loadAdditional(ValueInput p_409488_) {
      super.loadAdditional(p_409488_);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(p_409488_)) {
         ContainerHelper.loadAllItems(p_409488_, this.items);
      }
   }

   @Override
   protected void saveAdditional(ValueOutput p_407788_) {
      super.saveAdditional(p_407788_);
      if (!this.trySaveLootTable(p_407788_)) {
         ContainerHelper.saveAllItems(p_407788_, this.items);
      }
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> p_59243_) {
      this.items = p_59243_;
   }

   @Override
   protected AbstractContainerMenu createMenu(int p_59235_, Inventory p_59236_) {
      return new DispenserMenu(p_59235_, p_59236_, this);
   }
}
