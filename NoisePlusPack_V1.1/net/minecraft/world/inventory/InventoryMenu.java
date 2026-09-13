package net.minecraft.world.inventory;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InventoryMenu extends AbstractCraftingMenu {
   public static final int CONTAINER_ID = 0;
   public static final int RESULT_SLOT = 0;
   private static final int CRAFTING_GRID_WIDTH = 2;
   private static final int CRAFTING_GRID_HEIGHT = 2;
   public static final int CRAFT_SLOT_START = 1;
   public static final int CRAFT_SLOT_COUNT = 4;
   public static final int CRAFT_SLOT_END = 5;
   public static final int ARMOR_SLOT_START = 5;
   public static final int ARMOR_SLOT_COUNT = 4;
   public static final int ARMOR_SLOT_END = 9;
   public static final int INV_SLOT_START = 9;
   public static final int INV_SLOT_END = 36;
   public static final int USE_ROW_SLOT_START = 36;
   public static final int USE_ROW_SLOT_END = 45;
   public static final int SHIELD_SLOT = 45;
   public static final Identifier EMPTY_ARMOR_SLOT_HELMET = Identifier.withDefaultNamespace("container/slot/helmet");
   public static final Identifier EMPTY_ARMOR_SLOT_CHESTPLATE = Identifier.withDefaultNamespace("container/slot/chestplate");
   public static final Identifier EMPTY_ARMOR_SLOT_LEGGINGS = Identifier.withDefaultNamespace("container/slot/leggings");
   public static final Identifier EMPTY_ARMOR_SLOT_BOOTS = Identifier.withDefaultNamespace("container/slot/boots");
   public static final Identifier EMPTY_ARMOR_SLOT_SHIELD = Identifier.withDefaultNamespace("container/slot/shield");
   private static final Map<EquipmentSlot, Identifier> TEXTURE_EMPTY_SLOTS = Map.of(
      EquipmentSlot.FEET,
      EMPTY_ARMOR_SLOT_BOOTS,
      EquipmentSlot.LEGS,
      EMPTY_ARMOR_SLOT_LEGGINGS,
      EquipmentSlot.CHEST,
      EMPTY_ARMOR_SLOT_CHESTPLATE,
      EquipmentSlot.HEAD,
      EMPTY_ARMOR_SLOT_HELMET
   );
   private static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   public final boolean active;
   private final Player owner;

   public InventoryMenu(Inventory p_39706_, boolean p_39707_, final Player p_39708_) {
      super(null, 0, 2, 2);
      this.active = p_39707_;
      this.owner = p_39708_;
      this.addResultSlot(p_39708_, 154, 28);
      this.addCraftingGridSlots(98, 18);

      for (int i = 0; i < 4; i++) {
         EquipmentSlot equipmentslot = SLOT_IDS[i];
         Identifier identifier = TEXTURE_EMPTY_SLOTS.get(equipmentslot);
         this.addSlot(new ArmorSlot(p_39706_, p_39708_, equipmentslot, 39 - i, 8, 8 + i * 18, identifier));
      }

      this.addStandardInventorySlots(p_39706_, 8, 84);
      this.addSlot(new Slot(p_39706_, 40, 77, 62) {
         @Override
         public void setByPlayer(ItemStack p_270969_, ItemStack p_299540_) {
            p_39708_.onEquipItem(EquipmentSlot.OFFHAND, p_299540_, p_270969_);
            super.setByPlayer(p_270969_, p_299540_);
         }

         @Override
         public Identifier getNoItemIcon() {
            return InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
         }
      });
   }

   public static boolean isHotbarSlot(int p_150593_) {
      return p_150593_ >= 36 && p_150593_ < 45 || p_150593_ == 45;
   }

   @Override
   public void slotsChanged(Container p_39710_) {
      if (this.owner.level() instanceof ServerLevel serverlevel) {
         CraftingMenu.slotChangedCraftingGrid(this, serverlevel, this.owner, this.craftSlots, this.resultSlots, null);
      }
   }

   @Override
   public void removed(Player p_39721_) {
      super.removed(p_39721_);
      this.resultSlots.clearContent();
      if (!p_39721_.level().isClientSide()) {
         this.clearContainer(p_39721_, this.craftSlots);
      }
   }

   @Override
   public boolean stillValid(Player p_39712_) {
      return true;
   }

   @Override
   public ItemStack quickMoveStack(Player p_39723_, int p_39724_) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = this.slots.get(p_39724_);
      if (slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         EquipmentSlot equipmentslot = p_39723_.getEquipmentSlotForItem(itemstack);
         if (p_39724_ == 0) {
            if (!this.moveItemStackTo(itemstack1, 9, 45, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(itemstack1, itemstack);
         } else if (p_39724_ >= 1 && p_39724_ < 5) {
            if (!this.moveItemStackTo(itemstack1, 9, 45, false)) {
               return ItemStack.EMPTY;
            }
         } else if (p_39724_ >= 5 && p_39724_ < 9) {
            if (!this.moveItemStackTo(itemstack1, 9, 45, false)) {
               return ItemStack.EMPTY;
            }
         } else if (equipmentslot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !this.slots.get(8 - equipmentslot.getIndex()).hasItem()) {
            int i = 8 - equipmentslot.getIndex();
            if (!this.moveItemStackTo(itemstack1, i, i + 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (equipmentslot == EquipmentSlot.OFFHAND && !this.slots.get(45).hasItem()) {
            if (!this.moveItemStackTo(itemstack1, 45, 46, false)) {
               return ItemStack.EMPTY;
            }
         } else if (p_39724_ >= 9 && p_39724_ < 36) {
            if (!this.moveItemStackTo(itemstack1, 36, 45, false)) {
               return ItemStack.EMPTY;
            }
         } else if (p_39724_ >= 36 && p_39724_ < 45) {
            if (!this.moveItemStackTo(itemstack1, 9, 36, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, 9, 45, false)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, itemstack);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(p_39723_, itemstack1);
         if (p_39724_ == 0) {
            p_39723_.drop(itemstack1, false);
         }
      }

      return itemstack;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack p_39716_, Slot p_39717_) {
      return p_39717_.container != this.resultSlots && super.canTakeItemForPickAll(p_39716_, p_39717_);
   }

   @Override
   public Slot getResultSlot() {
      return this.slots.get(0);
   }

   @Override
   public List<Slot> getInputGridSlots() {
      return this.slots.subList(1, 5);
   }

   public CraftingContainer getCraftSlots() {
      return this.craftSlots;
   }

   @Override
   public RecipeBookType getRecipeBookType() {
      return RecipeBookType.CRAFTING;
   }

   @Override
   protected Player owner() {
      return this.owner;
   }
}
