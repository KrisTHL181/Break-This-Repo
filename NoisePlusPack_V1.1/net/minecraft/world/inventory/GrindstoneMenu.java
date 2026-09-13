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
   final Container repairSlots = new SimpleContainer(2) {
      @Override
      public void setChanged() {
         super.setChanged();
         GrindstoneMenu.this.slotsChanged(this);
      }
   };
   private final ContainerLevelAccess access;

   public GrindstoneMenu(int p_39563_, Inventory p_39564_) {
      this(p_39563_, p_39564_, ContainerLevelAccess.NULL);
   }

   public GrindstoneMenu(int p_39566_, Inventory p_39567_, final ContainerLevelAccess p_39568_) {
      super(MenuType.GRINDSTONE, p_39566_);
      this.access = p_39568_;
      this.addSlot(new Slot(this.repairSlots, 0, 49, 19) {
         @Override
         public boolean mayPlace(ItemStack p_39607_) {
            return p_39607_.isDamageableItem() || EnchantmentHelper.hasAnyEnchantments(p_39607_);
         }
      });
      this.addSlot(new Slot(this.repairSlots, 1, 49, 40) {
         @Override
         public boolean mayPlace(ItemStack p_39616_) {
            return p_39616_.isDamageableItem() || EnchantmentHelper.hasAnyEnchantments(p_39616_);
         }
      });
      this.addSlot(new Slot(this.resultSlots, 2, 129, 34) {
         @Override
         public boolean mayPlace(ItemStack p_39630_) {
            return false;
         }

         @Override
         public void onTake(Player p_150574_, ItemStack p_150575_) {
            p_39568_.execute((p_39634_, p_39635_) -> {
               if (p_39634_ instanceof ServerLevel) {
                  ExperienceOrb.award((ServerLevel)p_39634_, Vec3.atCenterOf(p_39635_), this.getExperienceAmount(p_39634_));
               }

               p_39634_.levelEvent(1042, p_39635_, 0);
            });
            GrindstoneMenu.this.repairSlots.setItem(0, ItemStack.EMPTY);
            GrindstoneMenu.this.repairSlots.setItem(1, ItemStack.EMPTY);
         }

         private int getExperienceAmount(Level p_39632_) {
            int i = 0;
            i += this.getExperienceFromItem(GrindstoneMenu.this.repairSlots.getItem(0));
            i += this.getExperienceFromItem(GrindstoneMenu.this.repairSlots.getItem(1));
            if (i > 0) {
               int j = (int)Math.ceil(i / 2.0);
               return j + p_39632_.random.nextInt(j);
            } else {
               return 0;
            }
         }

         private int getExperienceFromItem(ItemStack p_39637_) {
            int i = 0;
            ItemEnchantments itemenchantments = EnchantmentHelper.getEnchantmentsForCrafting(p_39637_);

            for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
               Holder<Enchantment> holder = (Holder<Enchantment>)entry.getKey();
               int j = entry.getIntValue();
               if (!holder.is(EnchantmentTags.CURSE)) {
                  i += holder.value().getMinCost(j);
               }
            }

            return i;
         }
      });
      this.addStandardInventorySlots(p_39567_, 8, 84);
   }

   @Override
   public void slotsChanged(Container p_39570_) {
      super.slotsChanged(p_39570_);
      if (p_39570_ == this.repairSlots) {
         this.createResult();
      }
   }

   private void createResult() {
      this.resultSlots.setItem(0, this.computeResult(this.repairSlots.getItem(0), this.repairSlots.getItem(1)));
      this.broadcastChanges();
   }

   private ItemStack computeResult(ItemStack p_335167_, ItemStack p_329934_) {
      boolean flag = !p_335167_.isEmpty() || !p_329934_.isEmpty();
      if (!flag) {
         return ItemStack.EMPTY;
      }

      if (p_335167_.getCount() <= 1 && p_329934_.getCount() <= 1) {
         boolean flag1 = !p_335167_.isEmpty() && !p_329934_.isEmpty();
         if (!flag1) {
            ItemStack itemstack = !p_335167_.isEmpty() ? p_335167_ : p_329934_;
            return !EnchantmentHelper.hasAnyEnchantments(itemstack) ? ItemStack.EMPTY : this.removeNonCursesFrom(itemstack.copy());
         } else {
            return this.mergeItems(p_335167_, p_329934_);
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   private ItemStack mergeItems(ItemStack p_327826_, ItemStack p_328339_) {
      if (!p_327826_.is(p_328339_.getItem())) {
         return ItemStack.EMPTY;
      }

      int i = Math.max(p_327826_.getMaxDamage(), p_328339_.getMaxDamage());
      int j = p_327826_.getMaxDamage() - p_327826_.getDamageValue();
      int k = p_328339_.getMaxDamage() - p_328339_.getDamageValue();
      int l = j + k + i * 5 / 100;
      int i1 = 1;
      if (!p_327826_.isDamageableItem()) {
         if (p_327826_.getMaxStackSize() < 2 || !ItemStack.matches(p_327826_, p_328339_)) {
            return ItemStack.EMPTY;
         }

         i1 = 2;
      }

      ItemStack itemstack = p_327826_.copyWithCount(i1);
      if (itemstack.isDamageableItem()) {
         itemstack.set(DataComponents.MAX_DAMAGE, i);
         itemstack.setDamageValue(Math.max(i - l, 0));
      }

      this.mergeEnchantsFrom(itemstack, p_328339_);
      return this.removeNonCursesFrom(itemstack);
   }

   private void mergeEnchantsFrom(ItemStack p_332353_, ItemStack p_333431_) {
      EnchantmentHelper.updateEnchantments(p_332353_, p_341519_ -> {
         ItemEnchantments itemenchantments = EnchantmentHelper.getEnchantmentsForCrafting(p_333431_);

         for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
            Holder<Enchantment> holder = (Holder<Enchantment>)entry.getKey();
            if (!holder.is(EnchantmentTags.CURSE) || p_341519_.getLevel(holder) == 0) {
               p_341519_.upgrade(holder, entry.getIntValue());
            }
         }
      });
   }

   private ItemStack removeNonCursesFrom(ItemStack p_332592_) {
      ItemEnchantments itemenchantments = EnchantmentHelper.updateEnchantments(
         p_332592_, p_327083_ -> p_327083_.removeIf(p_341517_ -> !p_341517_.is(EnchantmentTags.CURSE))
      );
      if (p_332592_.is(Items.ENCHANTED_BOOK) && itemenchantments.isEmpty()) {
         p_332592_ = p_332592_.transmuteCopy(Items.BOOK);
      }

      int i = 0;

      for (int j = 0; j < itemenchantments.size(); j++) {
         i = AnvilMenu.calculateIncreasedRepairCost(i);
      }

      p_332592_.set(DataComponents.REPAIR_COST, i);
      return p_332592_;
   }

   @Override
   public void removed(Player p_39586_) {
      super.removed(p_39586_);
      this.access.execute((p_39575_, p_39576_) -> this.clearContainer(p_39586_, this.repairSlots));
   }

   @Override
   public boolean stillValid(Player p_39572_) {
      return stillValid(this.access, p_39572_, Blocks.GRINDSTONE);
   }

   @Override
   public ItemStack quickMoveStack(Player p_39588_, int p_39589_) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = this.slots.get(p_39589_);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         ItemStack itemstack2 = this.repairSlots.getItem(0);
         ItemStack itemstack3 = this.repairSlots.getItem(1);
         if (p_39589_ == 2) {
            if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(itemstack1, itemstack);
         } else if (p_39589_ != 0 && p_39589_ != 1) {
            if (!itemstack2.isEmpty() && !itemstack3.isEmpty()) {
               if (p_39589_ >= 3 && p_39589_ < 30) {
                  if (!this.moveItemStackTo(itemstack1, 30, 39, false)) {
                     return ItemStack.EMPTY;
                  }
               } else if (p_39589_ >= 30 && p_39589_ < 39 && !this.moveItemStackTo(itemstack1, 3, 30, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
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

         slot.onTake(p_39588_, itemstack1);
      }

      return itemstack;
   }
}
