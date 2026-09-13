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

   public AnvilMenu(final int containerId, final Inventory inventory) {
      this(containerId, inventory, ContainerLevelAccess.NULL);
   }

   public AnvilMenu(final int containerId, final Inventory inventory, final ContainerLevelAccess access) {
      super(MenuType.ANVIL, containerId, inventory, access, createInputSlotDefinitions());
      this.addDataSlot(this.cost);
   }

   private static ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
      return ItemCombinerMenuSlotDefinition.create()
         .withSlot(0, 27, 47, itemStack -> true)
         .withSlot(1, 76, 47, itemStack -> true)
         .withResultSlot(2, 134, 47)
         .build();
   }

   @Override
   protected boolean isValidBlock(final BlockState state) {
      return state.is(BlockTags.ANVIL);
   }

   @Override
   protected boolean mayPickup(final Player player, final boolean hasItem) {
      return (player.hasInfiniteMaterials() || player.experienceLevel >= this.cost.get()) && this.cost.get() > 0;
   }

   @Override
   protected void onTake(final Player player, final ItemStack carried) {
      if (!player.hasInfiniteMaterials()) {
         player.giveExperienceLevels(-this.cost.get());
      }

      if (this.repairItemCountCost > 0) {
         ItemStack addition = this.inputSlots.getItem(1);
         if (!addition.isEmpty() && addition.getCount() > this.repairItemCountCost) {
            addition.shrink(this.repairItemCountCost);
            this.inputSlots.setItem(1, addition);
         } else {
            this.inputSlots.setItem(1, ItemStack.EMPTY);
         }
      } else if (!this.onlyRenaming) {
         this.inputSlots.setItem(1, ItemStack.EMPTY);
      }

      this.cost.set(0);
      if (player instanceof ServerPlayer serverPlayer
         && !StringUtil.isBlank(this.itemName)
         && !this.inputSlots.getItem(0).getHoverName().getString().equals(this.itemName)) {
         serverPlayer.getTextFilter().processStreamMessage(this.itemName);
      }

      this.inputSlots.setItem(0, ItemStack.EMPTY);
      this.access.execute((level, pos) -> {
         BlockState state = level.getBlockState(pos);
         if (!player.hasInfiniteMaterials() && state.is(BlockTags.ANVIL) && player.getRandom().nextFloat() < 0.12F) {
            BlockState newBlockState = AnvilBlock.damage(state);
            if (newBlockState == null) {
               level.removeBlock(pos, false);
               level.levelEvent(1029, pos, 0);
            } else {
               level.setBlock(pos, newBlockState, 2);
               level.levelEvent(1030, pos, 0);
            }
         } else {
            level.levelEvent(1030, pos, 0);
         }
      });
   }

   @Override
   public void createResult() {
      ItemStack input = this.inputSlots.getItem(0);
      this.onlyRenaming = false;
      this.cost.set(1);
      int price = 0;
      long tax = 0L;
      int namingCost = 0;
      if (!input.isEmpty() && EnchantmentHelper.canStoreEnchantments(input)) {
         ItemStack result = input.copy();
         ItemStack addition = this.inputSlots.getItem(1);
         ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(result));
         tax += (long)input.getOrDefault(DataComponents.REPAIR_COST, 0).intValue() + addition.getOrDefault(DataComponents.REPAIR_COST, 0).intValue();
         this.repairItemCountCost = 0;
         if (!addition.isEmpty()) {
            boolean usingBook = addition.has(DataComponents.STORED_ENCHANTMENTS);
            if (result.isDamageableItem() && input.isValidRepairItem(addition)) {
               int repairAmount = Math.min(result.getDamageValue(), result.getMaxDamage() / 4);
               if (repairAmount <= 0) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.cost.set(0);
                  return;
               }

               int count;
               for (count = 0; repairAmount > 0 && count < addition.getCount(); count++) {
                  int resultDamage = result.getDamageValue() - repairAmount;
                  result.setDamageValue(resultDamage);
                  price++;
                  repairAmount = Math.min(result.getDamageValue(), result.getMaxDamage() / 4);
               }

               this.repairItemCountCost = count;
            } else {
               if (!usingBook && (!result.is(addition.getItem()) || !result.isDamageableItem())) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.cost.set(0);
                  return;
               }

               if (result.isDamageableItem() && !usingBook) {
                  int remaining1 = input.getMaxDamage() - input.getDamageValue();
                  int remaining2 = addition.getMaxDamage() - addition.getDamageValue();
                  int additional = remaining2 + result.getMaxDamage() * 12 / 100;
                  int remaining = remaining1 + additional;
                  int resultDamage = result.getMaxDamage() - remaining;
                  if (resultDamage < 0) {
                     resultDamage = 0;
                  }

                  if (resultDamage < result.getDamageValue()) {
                     result.setDamageValue(resultDamage);
                     price += 2;
                  }
               }

               ItemEnchantments additionalEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(addition);
               boolean isAnyEnchantmentCompatible = false;
               boolean isAnyEnchantmentNotCompatible = false;

               for (Entry<Holder<Enchantment>> entry : additionalEnchantments.entrySet()) {
                  Holder<Enchantment> enchantmentHolder = (Holder<Enchantment>)entry.getKey();
                  int current = enchantments.getLevel(enchantmentHolder);
                  int level = entry.getIntValue();
                  level = current == level ? level + 1 : Math.max(level, current);
                  Enchantment enchantment = enchantmentHolder.value();
                  boolean compatible = enchantment.canEnchant(input);
                  if (this.player.hasInfiniteMaterials() || input.is(Items.ENCHANTED_BOOK)) {
                     compatible = true;
                  }

                  for (Holder<Enchantment> other : enchantments.keySet()) {
                     if (!other.equals(enchantmentHolder) && !Enchantment.areCompatible(enchantmentHolder, other)) {
                        compatible = false;
                        price++;
                     }
                  }

                  if (!compatible) {
                     isAnyEnchantmentNotCompatible = true;
                  } else {
                     isAnyEnchantmentCompatible = true;
                     if (level > enchantment.getMaxLevel()) {
                        level = enchantment.getMaxLevel();
                     }

                     enchantments.set(enchantmentHolder, level);
                     int fee = enchantment.getAnvilCost();
                     if (usingBook) {
                        fee = Math.max(1, fee / 2);
                     }

                     price += fee * level;
                     if (input.getCount() > 1) {
                        price = 40;
                     }
                  }
               }

               if (isAnyEnchantmentNotCompatible && !isAnyEnchantmentCompatible) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.cost.set(0);
                  return;
               }
            }
         }

         if (this.itemName != null && !StringUtil.isBlank(this.itemName)) {
            if (!this.itemName.equals(input.getHoverName().getString())) {
               namingCost = 1;
               price += namingCost;
               result.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName));
            }
         } else if (input.has(DataComponents.CUSTOM_NAME)) {
            namingCost = 1;
            price += namingCost;
            result.remove(DataComponents.CUSTOM_NAME);
         }

         int finalPrice = price <= 0 ? 0 : (int)Mth.clamp(tax + price, 0L, 2147483647L);
         this.cost.set(finalPrice);
         if (price <= 0) {
            result = ItemStack.EMPTY;
         }

         if (namingCost == price && namingCost > 0) {
            if (this.cost.get() >= 40) {
               this.cost.set(39);
            }

            this.onlyRenaming = true;
         }

         if (this.cost.get() >= 40 && !this.player.hasInfiniteMaterials()) {
            result = ItemStack.EMPTY;
         }

         if (!result.isEmpty()) {
            int baseCost = result.getOrDefault(DataComponents.REPAIR_COST, 0);
            if (baseCost < addition.getOrDefault(DataComponents.REPAIR_COST, 0)) {
               baseCost = addition.getOrDefault(DataComponents.REPAIR_COST, 0);
            }

            if (namingCost != price || namingCost == 0) {
               baseCost = calculateIncreasedRepairCost(baseCost);
            }

            result.set(DataComponents.REPAIR_COST, baseCost);
            EnchantmentHelper.setEnchantments(result, enchantments.toImmutable());
         }

         this.resultSlots.setItem(0, result);
         this.broadcastChanges();
      } else {
         this.resultSlots.setItem(0, ItemStack.EMPTY);
         this.cost.set(0);
      }
   }

   public static int calculateIncreasedRepairCost(final int baseCost) {
      return (int)Math.min(baseCost * 2L + 1L, 2147483647L);
   }

   public boolean setItemName(final String name) {
      String validatedName = validateName(name);
      if (validatedName != null && !validatedName.equals(this.itemName)) {
         this.itemName = validatedName;
         if (this.getSlot(2).hasItem()) {
            ItemStack itemStack = this.getSlot(2).getItem();
            if (StringUtil.isBlank(validatedName)) {
               itemStack.remove(DataComponents.CUSTOM_NAME);
            } else {
               itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(validatedName));
            }
         }

         this.createResult();
         return true;
      } else {
         return false;
      }
   }

   private static @Nullable String validateName(final String name) {
      String filteredName = StringUtil.filterText(name);
      return filteredName.length() <= 50 ? filteredName : null;
   }

   public int getCost() {
      return this.cost.get();
   }
}
