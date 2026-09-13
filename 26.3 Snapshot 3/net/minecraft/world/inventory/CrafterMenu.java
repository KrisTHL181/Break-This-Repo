package net.minecraft.world.inventory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.CrafterBlock;

public class CrafterMenu extends AbstractContainerMenu implements ContainerListener {
   protected static final int SLOT_COUNT = 9;
   private static final int INV_SLOT_START = 9;
   private static final int INV_SLOT_END = 36;
   private static final int USE_ROW_SLOT_START = 36;
   private static final int USE_ROW_SLOT_END = 45;
   private final ResultContainer resultContainer = new ResultContainer();
   private final ContainerData containerData;
   private final Player player;
   private final CraftingContainer container;

   public CrafterMenu(final int containerId, final Inventory inventory) {
      super(MenuType.CRAFTER_3x3, containerId);
      this.player = inventory.player;
      this.containerData = new SimpleContainerData(10);
      this.container = new TransientCraftingContainer(this, 3, 3);
      this.addSlots(inventory);
   }

   public CrafterMenu(final int containerId, final Inventory inventory, final CraftingContainer container, final ContainerData containerData) {
      super(MenuType.CRAFTER_3x3, containerId);
      this.player = inventory.player;
      this.containerData = containerData;
      this.container = container;
      checkContainerSize(container, 9);
      container.startOpen(inventory.player);
      this.addSlots(inventory);
      this.addSlotListener(this);
   }

   private void addSlots(final Inventory inventory) {
      for (int y = 0; y < 3; y++) {
         for (int x = 0; x < 3; x++) {
            int slot = x + y * 3;
            this.addSlot(new CrafterSlot(this.container, slot, 26 + x * 18, 17 + y * 18, this));
         }
      }

      this.addStandardInventorySlots(inventory, 8, 84);
      this.addSlot(new NonInteractiveResultSlot(this.resultContainer, 0, 134, 35));
      this.addDataSlots(this.containerData);
      this.refreshRecipeResult();
   }

   public void setSlotState(final int slotId, final boolean isEnabled) {
      CrafterSlot slot = (CrafterSlot)this.getSlot(slotId);
      this.containerData.set(slot.index, isEnabled ? 0 : 1);
      this.broadcastChanges();
   }

   public boolean isSlotDisabled(final int slotId) {
      return slotId > -1 && slotId < 9 ? this.containerData.get(slotId) == 1 : false;
   }

   public boolean isPowered() {
      return this.containerData.get(9) == 1;
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      ItemStack clicked = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         clicked = stack.copy();
         if (slotIndex < 9) {
            if (!this.moveItemStackTo(stack, 9, 45, true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, 0, 9, false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
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

   @Override
   public boolean stillValid(final Player player) {
      return this.container.stillValid(player);
   }

   private void refreshRecipeResult() {
      if (this.player instanceof ServerPlayer serverPlayer) {
         ServerLevel level = serverPlayer.level();
         CraftingInput craftInput = this.container.asCraftInput();
         ItemStack result = CrafterBlock.getPotentialResults(level, craftInput).map(recipe -> recipe.value().assemble(craftInput)).orElse(ItemStack.EMPTY);
         this.resultContainer.setItem(0, result);
      }
   }

   public Container getContainer() {
      return this.container;
   }

   @Override
   public void slotChanged(final AbstractContainerMenu container, final int slotIndex, final ItemStack itemStack) {
      this.refreshRecipeResult();
   }

   @Override
   public void dataChanged(final AbstractContainerMenu container, final int id, final int value) {
   }
}
