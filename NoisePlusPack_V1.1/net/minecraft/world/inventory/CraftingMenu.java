package net.minecraft.world.inventory;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public class CraftingMenu extends AbstractCraftingMenu {
   private static final int CRAFTING_GRID_WIDTH = 3;
   private static final int CRAFTING_GRID_HEIGHT = 3;
   public static final int RESULT_SLOT = 0;
   private static final int CRAFT_SLOT_START = 1;
   private static final int CRAFT_SLOT_COUNT = 9;
   private static final int CRAFT_SLOT_END = 10;
   private static final int INV_SLOT_START = 10;
   private static final int INV_SLOT_END = 37;
   private static final int USE_ROW_SLOT_START = 37;
   private static final int USE_ROW_SLOT_END = 46;
   private final ContainerLevelAccess access;
   private final Player player;
   private boolean placingRecipe;

   public CraftingMenu(int p_39353_, Inventory p_39354_) {
      this(p_39353_, p_39354_, ContainerLevelAccess.NULL);
   }

   public CraftingMenu(int p_39356_, Inventory p_39357_, ContainerLevelAccess p_39358_) {
      super(MenuType.CRAFTING, p_39356_, 3, 3);
      this.access = p_39358_;
      this.player = p_39357_.player;
      this.addResultSlot(this.player, 124, 35);
      this.addCraftingGridSlots(30, 17);
      this.addStandardInventorySlots(p_39357_, 8, 84);
   }

   protected static void slotChangedCraftingGrid(
      AbstractContainerMenu p_150547_,
      ServerLevel p_362728_,
      Player p_150549_,
      CraftingContainer p_150550_,
      ResultContainer p_150551_,
      @Nullable RecipeHolder<CraftingRecipe> p_344866_
   ) {
      CraftingInput craftinginput = p_150550_.asCraftInput();
      ServerPlayer serverplayer = (ServerPlayer)p_150549_;
      ItemStack itemstack = ItemStack.EMPTY;
      Optional<RecipeHolder<CraftingRecipe>> optional = p_362728_.getServer()
         .getRecipeManager()
         .getRecipeFor(RecipeType.CRAFTING, craftinginput, p_362728_, p_344866_);
      if (optional.isPresent()) {
         RecipeHolder<CraftingRecipe> recipeholder = optional.get();
         CraftingRecipe craftingrecipe = recipeholder.value();
         if (p_150551_.setRecipeUsed(serverplayer, recipeholder)) {
            ItemStack itemstack1 = craftingrecipe.assemble(craftinginput, p_362728_.registryAccess());
            if (itemstack1.isItemEnabled(p_362728_.enabledFeatures())) {
               itemstack = itemstack1;
            }
         }
      }

      p_150551_.setItem(0, itemstack);
      p_150547_.setRemoteSlot(0, itemstack);
      serverplayer.connection.send(new ClientboundContainerSetSlotPacket(p_150547_.containerId, p_150547_.incrementStateId(), 0, itemstack));
   }

   @Override
   public void slotsChanged(Container p_39366_) {
      if (!this.placingRecipe) {
         this.access.execute((p_359372_, p_359373_) -> {
            if (p_359372_ instanceof ServerLevel serverlevel) {
               slotChangedCraftingGrid(this, serverlevel, this.player, this.craftSlots, this.resultSlots, null);
            }
         });
      }
   }

   @Override
   public void beginPlacingRecipe() {
      this.placingRecipe = true;
   }

   @Override
   public void finishPlacingRecipe(ServerLevel p_368369_, RecipeHolder<CraftingRecipe> p_342309_) {
      this.placingRecipe = false;
      slotChangedCraftingGrid(this, p_368369_, this.player, this.craftSlots, this.resultSlots, p_342309_);
   }

   @Override
   public void removed(Player p_39389_) {
      super.removed(p_39389_);
      this.access.execute((p_39371_, p_39372_) -> this.clearContainer(p_39389_, this.craftSlots));
   }

   @Override
   public boolean stillValid(Player p_39368_) {
      return stillValid(this.access, p_39368_, Blocks.CRAFTING_TABLE);
   }

   @Override
   public ItemStack quickMoveStack(Player p_39391_, int p_39392_) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = this.slots.get(p_39392_);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (p_39392_ == 0) {
            itemstack1.getItem().onCraftedBy(itemstack1, p_39391_);
            if (!this.moveItemStackTo(itemstack1, 10, 46, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(itemstack1, itemstack);
         } else if (p_39392_ >= 10 && p_39392_ < 46) {
            if (!this.moveItemStackTo(itemstack1, 1, 10, false)) {
               if (p_39392_ < 37) {
                  if (!this.moveItemStackTo(itemstack1, 37, 46, false)) {
                     return ItemStack.EMPTY;
                  }
               } else if (!this.moveItemStackTo(itemstack1, 10, 37, false)) {
                  return ItemStack.EMPTY;
               }
            }
         } else if (!this.moveItemStackTo(itemstack1, 10, 46, false)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(p_39391_, itemstack1);
         if (p_39392_ == 0) {
            p_39391_.drop(itemstack1, false);
         }
      }

      return itemstack;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack p_39381_, Slot p_39382_) {
      return p_39382_.container != this.resultSlots && super.canTakeItemForPickAll(p_39381_, p_39382_);
   }

   @Override
   public Slot getResultSlot() {
      return this.slots.get(0);
   }

   @Override
   public List<Slot> getInputGridSlots() {
      return this.slots.subList(1, 10);
   }

   @Override
   public RecipeBookType getRecipeBookType() {
      return RecipeBookType.CRAFTING;
   }

   @Override
   protected Player owner() {
      return this.player;
   }
}
