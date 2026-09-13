package net.minecraft.world.inventory;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class AnvilMenu extends ItemCombinerMenu {
   public static final int INPUT_SLOT = 0;
   public static final int ADDITIONAL_SLOT = 1;
   public static final int RESULT_SLOT = 2;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final boolean DEBUG_COST = false;
   public static final int MAX_NAME_LENGTH = 50;
   private int repairItemCountCost;
   private @Nullable String itemName;
   private final DataSlot cost = DataSlot.standalone();
   private boolean onlyRenaming = false;
   private static final int COST_FAIL = 0;
   private static final int COST_BASE = 1;
   private static final int COST_ADDED_BASE = 1;
   private static final int COST_REPAIR_MATERIAL = 1;
   private static final int COST_REPAIR_SACRIFICE = 2;
   private static final int COST_INCOMPATIBLE_PENALTY = 1;
   private static final int COST_RENAME = 1;
   private static final int INPUT_SLOT_X_PLACEMENT = 27;
   private static final int ADDITIONAL_SLOT_X_PLACEMENT = 76;
   private static final int RESULT_SLOT_X_PLACEMENT = 134;
   private static final int SLOT_Y_PLACEMENT = 47;

   public AnvilMenu(int p_39005_, Inventory p_39006_) {
      this(p_39005_, p_39006_, ContainerLevelAccess.NULL);
   }

   public AnvilMenu(int p_39008_, Inventory p_39009_, ContainerLevelAccess p_39010_) {
      super(MenuType.ANVIL, p_39008_, p_39009_, p_39010_, createInputSlotDefinitions());
      this.addDataSlot(this.cost);
   }

   private static ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
      return ItemCombinerMenuSlotDefinition.create()
         .withSlot(0, 27, 47, p_266635_ -> true)
         .withSlot(1, 76, 47, p_266634_ -> true)
         .withResultSlot(2, 134, 47)
         .build();
   }

   @Override
   protected boolean isValidBlock(BlockState p_39019_) {
      return p_39019_.is(BlockTags.ANVIL);
   }

   @Override
   protected boolean mayPickup(Player p_39023_, boolean p_39024_) {
      return (p_39023_.hasInfiniteMaterials() || p_39023_.experienceLevel >= this.cost.get()) && this.cost.get() > 0;
   }

   @Override
   protected void onTake(Player p_150474_, ItemStack p_150475_) {
      if (!p_150474_.hasInfiniteMaterials()) {
         p_150474_.giveExperienceLevels(-this.cost.get());
      }

      if (this.repairItemCountCost > 0) {
         ItemStack itemstack = this.inputSlots.getItem(1);
         if (!itemstack.isEmpty() && itemstack.getCount() > this.repairItemCountCost) {
            itemstack.shrink(this.repairItemCountCost);
            this.inputSlots.setItem(1, itemstack);
         } else {
            this.inputSlots.setItem(1, ItemStack.EMPTY);
         }
      } else if (!this.onlyRenaming) {
         this.inputSlots.setItem(1, ItemStack.EMPTY);
      }

      this.cost.set(0);
      if (p_150474_ instanceof ServerPlayer serverplayer
         && !StringUtil.isBlank(this.itemName)
         && !this.inputSlots.getItem(0).getHoverName().getString().equals(this.itemName)) {
         serverplayer.getTextFilter().processStreamMessage(this.itemName);
      }

      this.inputSlots.setItem(0, ItemStack.EMPTY);
      this.access.execute((p_150479_, p_150480_) -> {
         BlockState blockstate = p_150479_.getBlockState(p_150480_);
         if (!p_150474_.hasInfiniteMaterials() && blockstate.is(BlockTags.ANVIL) && p_150474_.getRandom().nextFloat() < 0.12F) {
            BlockState blockstate1 = AnvilBlock.damage(blockstate);
            if (blockstate1 == null) {
               p_150479_.removeBlock(p_150480_, false);
               p_150479_.levelEvent(1029, p_150480_, 0);
            } else {
               p_150479_.setBlock(p_150480_, blockstate1, 2);
               p_150479_.levelEvent(1030, p_150480_, 0);
            }
         } else {
            p_150479_.levelEvent(1030, p_150480_, 0);
         }
      });
   }

   @Override
   public void createResult() {
      ItemStack itemstack = this.inputSlots.getItem(0);
      this.onlyRenaming = false;
      this.cost.set(1);
      int i = 0;
      long j = 0L;
      int k = 0;
      if (!itemstack.isEmpty() && EnchantmentHelper.canStoreEnchantments(itemstack)) {
         ItemStack itemstack1 = itemstack.copy();
         ItemStack itemstack2 = this.inputSlots.getItem(1);
         ItemEnchantments.Mutable itemenchantments$mutable = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(itemstack1));
         j += (long)itemstack.getOrDefault(DataComponents.REPAIR_COST, 0).intValue() + itemstack2.getOrDefault(DataComponents.REPAIR_COST, 0).intValue();
         this.repairItemCountCost = 0;
         if (!itemstack2.isEmpty()) {
            boolean flag = itemstack2.has(DataComponents.STORED_ENCHANTMENTS);
            if (itemstack1.isDamageableItem() && itemstack.isValidRepairItem(itemstack2)) {
               int l2 = Math.min(itemstack1.getDamageValue(), itemstack1.getMaxDamage() / 4);
               if (l2 <= 0) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.cost.set(0);
                  return;
               }

               int j3;
               for (j3 = 0; l2 > 0 && j3 < itemstack2.getCount(); j3++) {
                  int k3 = itemstack1.getDamageValue() - l2;
                  itemstack1.setDamageValue(k3);
                  i++;
                  l2 = Math.min(itemstack1.getDamageValue(), itemstack1.getMaxDamage() / 4);
               }

               this.repairItemCountCost = j3;
            } else {
               if (!flag && (!itemstack1.is(itemstack2.getItem()) || !itemstack1.isDamageableItem())) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.cost.set(0);
                  return;
               }

               if (itemstack1.isDamageableItem() && !flag) {
                  int l = itemstack.getMaxDamage() - itemstack.getDamageValue();
                  int i1 = itemstack2.getMaxDamage() - itemstack2.getDamageValue();
                  int j1 = i1 + itemstack1.getMaxDamage() * 12 / 100;
                  int k1 = l + j1;
                  int l1 = itemstack1.getMaxDamage() - k1;
                  if (l1 < 0) {
                     l1 = 0;
                  }

                  if (l1 < itemstack1.getDamageValue()) {
                     itemstack1.setDamageValue(l1);
                     i += 2;
                  }
               }

               ItemEnchantments itemenchantments = EnchantmentHelper.getEnchantmentsForCrafting(itemstack2);
               boolean flag2 = false;
               boolean flag3 = false;

               for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
                  Holder<Enchantment> holder = (Holder<Enchantment>)entry.getKey();
                  int i2 = itemenchantments$mutable.getLevel(holder);
                  int j2 = entry.getIntValue();
                  j2 = i2 == j2 ? j2 + 1 : Math.max(j2, i2);
                  Enchantment enchantment = holder.value();
                  boolean flag1 = enchantment.canEnchant(itemstack);
                  if (this.player.hasInfiniteMaterials() || itemstack.is(Items.ENCHANTED_BOOK)) {
                     flag1 = true;
                  }

                  for (Holder<Enchantment> holder1 : itemenchantments$mutable.keySet()) {
                     if (!holder1.equals(holder) && !Enchantment.areCompatible(holder, holder1)) {
                        flag1 = false;
                        i++;
                     }
                  }

                  if (!flag1) {
                     flag3 = true;
                  } else {
                     flag2 = true;
                     if (j2 > enchantment.getMaxLevel()) {
                        j2 = enchantment.getMaxLevel();
                     }

                     itemenchantments$mutable.set(holder, j2);
                     int l3 = enchantment.getAnvilCost();
                     if (flag) {
                        l3 = Math.max(1, l3 / 2);
                     }

                     i += l3 * j2;
                     if (itemstack.getCount() > 1) {
                        i = 40;
                     }
                  }
               }

               if (flag3 && !flag2) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.cost.set(0);
                  return;
               }
            }
         }

         if (this.itemName != null && !StringUtil.isBlank(this.itemName)) {
            if (!this.itemName.equals(itemstack.getHoverName().getString())) {
               k = 1;
               i += k;
               itemstack1.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName));
            }
         } else if (itemstack.has(DataComponents.CUSTOM_NAME)) {
            k = 1;
            i += k;
            itemstack1.remove(DataComponents.CUSTOM_NAME);
         }

         int k2 = i <= 0 ? 0 : (int)Mth.clamp(j + i, 0L, 2147483647L);
         this.cost.set(k2);
         if (i <= 0) {
            itemstack1 = ItemStack.EMPTY;
         }

         if (k == i && k > 0) {
            if (this.cost.get() >= 40) {
               this.cost.set(39);
            }

            this.onlyRenaming = true;
         }

         if (this.cost.get() >= 40 && !this.player.hasInfiniteMaterials()) {
            itemstack1 = ItemStack.EMPTY;
         }

         if (!itemstack1.isEmpty()) {
            int i3 = itemstack1.getOrDefault(DataComponents.REPAIR_COST, 0);
            if (i3 < itemstack2.getOrDefault(DataComponents.REPAIR_COST, 0)) {
               i3 = itemstack2.getOrDefault(DataComponents.REPAIR_COST, 0);
            }

            if (k != i || k == 0) {
               i3 = calculateIncreasedRepairCost(i3);
            }

            itemstack1.set(DataComponents.REPAIR_COST, i3);
            EnchantmentHelper.setEnchantments(itemstack1, itemenchantments$mutable.toImmutable());
         }

         this.resultSlots.setItem(0, itemstack1);
         this.broadcastChanges();
      } else {
         this.resultSlots.setItem(0, ItemStack.EMPTY);
         this.cost.set(0);
      }
   }

   public static int calculateIncreasedRepairCost(int p_39026_) {
      return (int)Math.min(p_39026_ * 2L + 1L, 2147483647L);
   }

   public boolean setItemName(String p_288970_) {
      String s = validateName(p_288970_);
      if (s != null && !s.equals(this.itemName)) {
         this.itemName = s;
         if (this.getSlot(2).hasItem()) {
            ItemStack itemstack = this.getSlot(2).getItem();
            if (StringUtil.isBlank(s)) {
               itemstack.remove(DataComponents.CUSTOM_NAME);
            } else {
               itemstack.set(DataComponents.CUSTOM_NAME, Component.literal(s));
            }
         }

         this.createResult();
         return true;
      } else {
         return false;
      }
   }

   private static @Nullable String validateName(String p_288995_) {
      String s = StringUtil.filterText(p_288995_);
      return s.length() <= 50 ? s : null;
   }

   public int getCost() {
      return this.cost.get();
   }
}
