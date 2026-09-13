package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class ItemCombinerMenu extends AbstractContainerMenu {
   private static final int INVENTORY_SLOTS_PER_ROW = 9;
   private static final int INVENTORY_ROWS = 3;
   private static final int INPUT_SLOT_START = 0;
   protected final ContainerLevelAccess access;
   protected final Player player;
   protected final Container inputSlots;
   protected final ResultContainer resultSlots = new ResultContainer() {
      @Override
      public void setChanged() {
         ItemCombinerMenu.this.slotsChanged(this);
      }
   };
   private final int resultSlotIndex;

   protected boolean mayPickup(final Player player, final boolean hasItem) {
      return true;
   }

   protected abstract void onTake(Player player, ItemStack carried);

   protected abstract boolean isValidBlock(BlockState state);

   public ItemCombinerMenu(
      final @Nullable MenuType<?> menuType,
      final int containerId,
      final Inventory inventory,
      final ContainerLevelAccess access,
      final ItemCombinerMenuSlotDefinition itemInputSlots
   ) {
      super(menuType, containerId);
      this.access = access;
      this.player = inventory.player;
      this.inputSlots = this.createContainer(itemInputSlots.getNumOfInputSlots());
      this.resultSlotIndex = itemInputSlots.getResultSlotIndex();
      this.createInputSlots(itemInputSlots);
      this.createResultSlot(itemInputSlots);
      this.addStandardInventorySlots(inventory, 8, 84);
   }

   private void createInputSlots(final ItemCombinerMenuSlotDefinition itemInputSlots) {
      for (final ItemCombinerMenuSlotDefinition.SlotDefinition slot : itemInputSlots.getSlots()) {
         this.addSlot(new Slot(this.inputSlots, slot.slotIndex(), slot.x(), slot.y()) {
            @Override
            public boolean mayPlace(final ItemStack itemStack) {
               return slot.mayPlace().test(itemStack);
            }
         });
      }
   }

   private void createResultSlot(final ItemCombinerMenuSlotDefinition itemInputSlots) {
      this.addSlot(
         new Slot(this.resultSlots, itemInputSlots.getResultSlot().slotIndex(), itemInputSlots.getResultSlot().x(), itemInputSlots.getResultSlot().y()) {
            @Override
            public boolean mayPlace(final ItemStack itemStack) {
               return false;
            }

            @Override
            public boolean mayPickup(final Player player) {
               return ItemCombinerMenu.this.mayPickup(player, this.hasItem());
            }

            @Override
            public void onTake(final Player player, final ItemStack carried) {
               ItemCombinerMenu.this.onTake(player, carried);
            }
         }
      );
   }

   public abstract void createResult();

   private SimpleContainer createContainer(final int size) {
      return new SimpleContainer(size) {
         @Override
         public void setChanged() {
            super.setChanged();
            ItemCombinerMenu.this.slotsChanged(this);
         }
      };
   }

   @Override
   public void slotsChanged(final Container container) {
      super.slotsChanged(container);
      if (container == this.inputSlots) {
         this.createResult();
      }
   }

   @Override
   public void removed(final Player player) {
      super.removed(player);
      this.access.execute((level, pos) -> this.clearContainer(player, this.inputSlots));
   }

   @Override
   public boolean stillValid(final Player player) {
      return this.access.evaluate((level, pos) -> !this.isValidBlock(level.getBlockState(pos)) ? false : player.isWithinBlockInteractionRange(pos, 4.0), true);
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      ItemStack clicked = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         clicked = stack.copy();
         int inventorySlotStart = this.getInventorySlotStart();
         int useRowSlotEnd = this.getUseRowEnd();
         if (slotIndex == this.getResultSlot()) {
            if (!this.moveItemStackTo(stack, inventorySlotStart, useRowSlotEnd, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(stack, clicked);
         } else if (slotIndex >= 0 && slotIndex < this.getResultSlot()) {
            if (!this.moveItemStackTo(stack, inventorySlotStart, useRowSlotEnd, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.canMoveIntoInputSlots(stack) && slotIndex >= this.getInventorySlotStart() && slotIndex < this.getUseRowEnd()) {
            if (!this.moveItemStackTo(stack, 0, this.getResultSlot(), false)) {
               return ItemStack.EMPTY;
            }
         } else if (slotIndex >= this.getInventorySlotStart() && slotIndex < this.getInventorySlotEnd()) {
            if (!this.moveItemStackTo(stack, this.getUseRowStart(), this.getUseRowEnd(), false)) {
               return ItemStack.EMPTY;
            }
         } else if (slotIndex >= this.getUseRowStart()
            && slotIndex < this.getUseRowEnd()
            && !this.moveItemStackTo(stack, this.getInventorySlotStart(), this.getInventorySlotEnd(), false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (stack.getCount() == clicked.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(player, stack);
      }

      return clicked;
   }

   protected boolean canMoveIntoInputSlots(final ItemStack stack) {
      return true;
   }

   public int getResultSlot() {
      return this.resultSlotIndex;
   }

   private int getInventorySlotStart() {
      return this.getResultSlot() + 1;
   }

   private int getInventorySlotEnd() {
      return this.getInventorySlotStart() + 27;
   }

   private int getUseRowStart() {
      return this.getInventorySlotEnd();
   }

   private int getUseRowEnd() {
      return this.getUseRowStart() + 9;
   }
}
