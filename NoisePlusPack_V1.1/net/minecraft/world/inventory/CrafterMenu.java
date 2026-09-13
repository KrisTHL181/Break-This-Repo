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

   public CrafterMenu(int p_310742_, Inventory p_312080_) {
      super(MenuType.CRAFTER_3x3, p_310742_);
      this.player = p_312080_.player;
      this.containerData = new SimpleContainerData(10);
      this.container = new TransientCraftingContainer(this, 3, 3);
      this.addSlots(p_312080_);
   }

   public CrafterMenu(int p_312262_, Inventory p_309729_, CraftingContainer p_309543_, ContainerData p_312942_) {
      super(MenuType.CRAFTER_3x3, p_312262_);
      this.player = p_309729_.player;
      this.containerData = p_312942_;
      this.container = p_309543_;
      checkContainerSize(p_309543_, 9);
      p_309543_.startOpen(p_309729_.player);
      this.addSlots(p_309729_);
      this.addSlotListener(this);
   }

   private void addSlots(Inventory p_312143_) {
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) {
            int k = j + i * 3;
            this.addSlot(new CrafterSlot(this.container, k, 26 + j * 18, 17 + i * 18, this));
         }
      }

      this.addStandardInventorySlots(p_312143_, 8, 84);
      this.addSlot(new NonInteractiveResultSlot(this.resultContainer, 0, 134, 35));
      this.addDataSlots(this.containerData);
      this.refreshRecipeResult();
   }

   public void setSlotState(int p_312148_, boolean p_312187_) {
      CrafterSlot crafterslot = (CrafterSlot)this.getSlot(p_312148_);
      this.containerData.set(crafterslot.index, p_312187_ ? 0 : 1);
      this.broadcastChanges();
   }

   public boolean isSlotDisabled(int p_311661_) {
      return p_311661_ > -1 && p_311661_ < 9 ? this.containerData.get(p_311661_) == 1 : false;
   }

   public boolean isPowered() {
      return this.containerData.get(9) == 1;
   }

   @Override
   public ItemStack quickMoveStack(Player p_313133_, int p_309724_) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = this.slots.get(p_309724_);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (p_309724_ < 9) {
            if (!this.moveItemStackTo(itemstack1, 9, 45, true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, 0, 9, false)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(p_313133_, itemstack1);
      }

      return itemstack;
   }

   @Override
   public boolean stillValid(Player p_309546_) {
      return this.container.stillValid(p_309546_);
   }

   private void refreshRecipeResult() {
      if (this.player instanceof ServerPlayer serverplayer) {
         ServerLevel serverlevel = serverplayer.level();
         CraftingInput craftinginput = this.container.asCraftInput();
         ItemStack itemstack = CrafterBlock.getPotentialResults(serverlevel, craftinginput)
            .map(p_449763_ -> p_449763_.value().assemble(craftinginput, serverlevel.registryAccess()))
            .orElse(ItemStack.EMPTY);
         this.resultContainer.setItem(0, itemstack);
      }
   }

   public Container getContainer() {
      return this.container;
   }

   @Override
   public void slotChanged(AbstractContainerMenu p_313164_, int p_310604_, ItemStack p_312680_) {
      this.refreshRecipeResult();
   }

   @Override
   public void dataChanged(AbstractContainerMenu p_312122_, int p_310028_, int p_310424_) {
   }
}
