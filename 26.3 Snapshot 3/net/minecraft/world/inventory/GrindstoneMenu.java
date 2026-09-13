package net.minecraft.world.inventory;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class GrindstoneMenu extends AbstractContainerMenu {
   public static final int MAX_NAME_LENGTH = 35;
   public static final int INPUT_SLOT = 0;
   public static final int ADDITIONAL_SLOT = 1;
   public static final int RESULT_SLOT = 2;
   private static final int INV_SLOT_START = 3;
   private static final int INV_SLOT_END = 30;
   private static final int USE_ROW_SLOT_START = 30;
   private static final int USE_ROW_SLOT_END = 39;
   private final Container resultSlots = new ResultContainer();
   private final Container repairSlots = new SimpleContainer(2) {
      @Override
      public void setChanged() {
         super.setChanged();
         GrindstoneMenu.this.slotsChanged(this);
      }
   };
   private final ContainerLevelAccess access;

   public GrindstoneMenu(final int containerId, final Inventory inventory) {
      this(containerId, inventory, ContainerLevelAccess.NULL);
   }

   public GrindstoneMenu(final int containerId, final Inventory inventory, final ContainerLevelAccess access) {
      super(MenuType.GRINDSTONE, containerId);
      this.access = access;
      this.addSlot(new Slot(this.repairSlots, 0, 49, 19) {
         @Override
         public boolean mayPlace(final ItemStack itemStack) {
            return itemStack.isDamageableItem() || EnchantmentHelper.hasAnyEnchantments(itemStack);
         }
      });
      this.addSlot(new Slot(this.repairSlots, 1, 49, 40) {
         @Override
         public boolean mayPlace(final ItemStack itemStack) {
            return itemStack.isDamageableItem() || EnchantmentHelper.hasAnyEnchantments(itemStack);
         }
      });
      this.addSlot(new Slot(this.resultSlots, 2, 129, 34) {
         @Override
         public boolean mayPlace(final ItemStack itemStack) {
            return false;
         }

         @Override
         public void onTake(final Player player, final ItemStack carried) {
            access.execute((level, pos) -> {
               if (level instanceof ServerLevel serverLevel) {
                  ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), this.getExperienceAmount(level));
               }

               level.levelEvent(1042, pos, 0);
            });
            GrindstoneMenu.this.repairSlots.setItem(0, ItemStack.EMPTY);
            GrindstoneMenu.this.repairSlots.setItem(1, ItemStack.EMPTY);
         }

         private int getExperienceAmount(final Level level) {
            int amount = 0;
            amount += this.getExperienceFromItem(GrindstoneMenu.this.repairSlots.getItem(0));
            amount += this.getExperienceFromItem(GrindstoneMenu.this.repairSlots.getItem(1));
            if (amount > 0) {
               int halfAmount = (int)Math.ceil(amount / 2.0);
               return halfAmount + level.getRandom().nextInt(halfAmount);
            } else {
               return 0;
            }
         }

         private int getExperienceFromItem(final ItemStack item) {
            int amount = 0;
            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(item);

            for (Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
               Holder<Enchantment> enchant = (Holder<Enchantment>)entry.getKey();
               int lvl = entry.getIntValue();
               if (!enchant.is(EnchantmentTags.CURSE)) {
                  amount += enchant.value().getMinCost(lvl);
               }
            }

            return amount;
         }
      });
      this.addStandardInventorySlots(inventory, 8, 84);
   }

   @Override
   public void slotsChanged(final Container container) {
      super.slotsChanged(container);
      if (container == this.repairSlots) {
         this.createResult();
      }
   }

   private void createResult() {
      this.resultSlots.setItem(0, this.computeResult(this.repairSlots.getItem(0), this.repairSlots.getItem(1)));
      this.broadcastChanges();
   }

   private ItemStack computeResult(final ItemStack input, final ItemStack additional) {
      boolean hasAnItem = !input.isEmpty() || !additional.isEmpty();
      if (!hasAnItem) {
         return ItemStack.EMPTY;
      }

      if (input.getCount() <= 1 && additional.getCount() <= 1) {
         boolean hasBothItems = !input.isEmpty() && !additional.isEmpty();
         if (!hasBothItems) {
            ItemStack item = !input.isEmpty() ? input : additional;
            return !EnchantmentHelper.hasAnyEnchantments(item) ? ItemStack.EMPTY : this.removeNonCursesFrom(item.copy());
         } else {
            return this.mergeItems(input, additional);
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   private ItemStack mergeItems(final ItemStack input, final ItemStack additional) {
      if (!input.is(additional.getItem())) {
         return ItemStack.EMPTY;
      }

      int durability = Math.max(input.getMaxDamage(), additional.getMaxDamage());
      int remaining1 = input.getMaxDamage() - input.getDamageValue();
      int remaining2 = additional.getMaxDamage() - additional.getDamageValue();
      int remaining = remaining1 + remaining2 + durability * 5 / 100;
      int count = 1;
      if (!input.isDamageableItem()) {
         if (input.getMaxStackSize() < 2 || !ItemStack.matches(input, additional)) {
            return ItemStack.EMPTY;
         }

         count = 2;
      }

      ItemStack newItem = input.copyWithCount(count);
      if (newItem.isDamageableItem()) {
         newItem.set(DataComponents.MAX_DAMAGE, durability);
         newItem.setDamageValue(Math.max(durability - remaining, 0));
      }

      this.mergeEnchantsFrom(newItem, additional);
      return this.removeNonCursesFrom(newItem);
   }

   private void mergeEnchantsFrom(final ItemStack target, final ItemStack source) {
      EnchantmentHelper.updateEnchantments(target, newEnchantments -> {
         ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(source);

         for (Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> enchant = (Holder<Enchantment>)entry.getKey();
            if (!enchant.is(EnchantmentTags.CURSE) || newEnchantments.getLevel(enchant) == 0) {
               newEnchantments.upgrade(enchant, entry.getIntValue());
            }
         }
      });
   }

   private ItemStack removeNonCursesFrom(ItemStack item) {
      ItemEnchantments newEnchantments = EnchantmentHelper.updateEnchantments(
         item, enchantments -> enchantments.removeIf(enchantment -> !enchantment.is(EnchantmentTags.CURSE))
      );
      if (item.is(Items.ENCHANTED_BOOK) && newEnchantments.isEmpty()) {
         item = item.transmuteCopy(Items.BOOK);
      }

      int repairCost = 0;

      for (int i = 0; i < newEnchantments.size(); i++) {
         repairCost = AnvilMenu.calculateIncreasedRepairCost(repairCost);
      }

      item.set(DataComponents.REPAIR_COST, repairCost);
      return item;
   }

   @Override
   public void removed(final Player player) {
      super.removed(player);
      this.access.execute((level, pos) -> this.clearContainer(player, this.repairSlots));
   }

   @Override
   public boolean stillValid(final Player player) {
      return stillValid(this.access, player, Blocks.GRINDSTONE);
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      ItemStack clicked = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack item = slot.getItem();
         clicked = item.copy();
         ItemStack input = this.repairSlots.getItem(0);
         ItemStack additional = this.repairSlots.getItem(1);
         if (slotIndex == 2) {
            if (!this.moveItemStackTo(item, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(item, clicked);
         } else if (slotIndex != 0 && slotIndex != 1) {
            if (!input.isEmpty() && !additional.isEmpty()) {
               if (slotIndex >= 3 && slotIndex < 30) {
                  if (!this.moveItemStackTo(item, 30, 39, false)) {
                     return ItemStack.EMPTY;
                  }
               } else if (slotIndex >= 30 && slotIndex < 39 && !this.moveItemStackTo(item, 3, 30, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(item, 0, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(item, 3, 39, false)) {
            return ItemStack.EMPTY;
         }

         if (item.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (item.getCount() == clicked.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(player, item);
      }

      return clicked;
   }
}
