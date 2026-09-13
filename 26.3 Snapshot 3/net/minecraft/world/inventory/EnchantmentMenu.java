package net.minecraft.world.inventory;

import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;

public class EnchantmentMenu extends AbstractContainerMenu {
   private static final Identifier EMPTY_SLOT_LAPIS_LAZULI = Identifier.withDefaultNamespace("container/slot/lapis_lazuli");
   private final Container enchantSlots = new SimpleContainer(2) {
      @Override
      public void setChanged() {
         super.setChanged();
         EnchantmentMenu.this.slotsChanged(this);
      }
   };
   private final ContainerLevelAccess access;
   private final RandomSource random = RandomSource.create();
   private final DataSlot enchantmentSeed = DataSlot.standalone();
   public final int[] costs = new int[3];
   public final int[] enchantClue = new int[]{-1, -1, -1};
   public final int[] levelClue = new int[]{-1, -1, -1};

   public EnchantmentMenu(final int containerId, final Inventory inventory) {
      this(containerId, inventory, ContainerLevelAccess.NULL);
   }

   public EnchantmentMenu(final int containerId, final Inventory inventory, final ContainerLevelAccess access) {
      super(MenuType.ENCHANTMENT, containerId);
      this.access = access;
      this.addSlot(new Slot(this.enchantSlots, 0, 15, 47) {
         @Override
         public int getMaxStackSize() {
            return 1;
         }
      });
      this.addSlot(new Slot(this.enchantSlots, 1, 35, 47) {
         @Override
         public boolean mayPlace(final ItemStack itemStack) {
            return itemStack.is(Items.LAPIS_LAZULI);
         }

         @Override
         public Identifier getNoItemIcon() {
            return EnchantmentMenu.EMPTY_SLOT_LAPIS_LAZULI;
         }
      });
      this.addStandardInventorySlots(inventory, 8, 84);
      this.addDataSlot(DataSlot.shared(this.costs, 0));
      this.addDataSlot(DataSlot.shared(this.costs, 1));
      this.addDataSlot(DataSlot.shared(this.costs, 2));
      this.addDataSlot(this.enchantmentSeed).set(inventory.player.getEnchantmentSeed());
      this.addDataSlot(DataSlot.shared(this.enchantClue, 0));
      this.addDataSlot(DataSlot.shared(this.enchantClue, 1));
      this.addDataSlot(DataSlot.shared(this.enchantClue, 2));
      this.addDataSlot(DataSlot.shared(this.levelClue, 0));
      this.addDataSlot(DataSlot.shared(this.levelClue, 1));
      this.addDataSlot(DataSlot.shared(this.levelClue, 2));
   }

   @Override
   public void slotsChanged(final Container container) {
      if (container == this.enchantSlots) {
         ItemStack itemStack = container.getItem(0);
         if (!itemStack.isEmpty() && itemStack.isEnchantable()) {
            this.access.execute((level, pos) -> {
               IdMap<Holder<Enchantment>> holders = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
               int bookcases = 0;

               for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
                  if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                     bookcases++;
                  }
               }

               this.random.setSeed(this.enchantmentSeed.get());

               for (int ixx = 0; ixx < 3; ixx++) {
                  this.costs[ixx] = EnchantmentHelper.getEnchantmentCost(this.random, ixx, bookcases, itemStack);
                  this.enchantClue[ixx] = -1;
                  this.levelClue[ixx] = -1;
                  if (this.costs[ixx] < ixx + 1) {
                     this.costs[ixx] = 0;
                  }
               }

               for (int ix = 0; ix < 3; ix++) {
                  if (this.costs[ix] > 0) {
                     List<EnchantmentInstance> list = this.getEnchantmentList(level.registryAccess(), itemStack, ix, this.costs[ix]);
                     if (!list.isEmpty()) {
                        EnchantmentInstance ench = list.get(this.random.nextInt(list.size()));
                        this.enchantClue[ix] = holders.getId(ench.enchantment());
                        this.levelClue[ix] = ench.level();
                     }
                  }
               }

               this.broadcastChanges();
            });
         } else {
            for (int i = 0; i < 3; i++) {
               this.costs[i] = 0;
               this.enchantClue[i] = -1;
               this.levelClue[i] = -1;
            }
         }
      }
   }

   @Override
   public boolean clickMenuButton(final Player player, final int buttonId) {
      if (buttonId >= 0 && buttonId < this.costs.length) {
         ItemStack itemStack = this.enchantSlots.getItem(0);
         ItemStack currency = this.enchantSlots.getItem(1);
         int enchantmentCost = buttonId + 1;
         if ((currency.isEmpty() || currency.getCount() < enchantmentCost) && !player.hasInfiniteMaterials()) {
            return false;
         }

         if (this.costs[buttonId] <= 0
            || itemStack.isEmpty()
            || (player.experienceLevel < enchantmentCost || player.experienceLevel < this.costs[buttonId]) && !player.hasInfiniteMaterials()) {
            return false;
         }

         this.access.execute((level, pos) -> {
            ItemStack enchantmentItem = itemStack;
            List<EnchantmentInstance> newEnchantment = this.getEnchantmentList(level.registryAccess(), enchantmentItem, buttonId, this.costs[buttonId]);
            if (!newEnchantment.isEmpty()) {
               player.onEnchantmentPerformed(enchantmentItem, enchantmentCost);
               if (enchantmentItem.is(Items.BOOK)) {
                  enchantmentItem = itemStack.transmuteCopy(Items.ENCHANTED_BOOK);
                  this.enchantSlots.setItem(0, enchantmentItem);
               }

               for (EnchantmentInstance enchantment : newEnchantment) {
                  enchantmentItem.enchant(enchantment.enchantment(), enchantment.level());
               }

               currency.consume(enchantmentCost, player);
               if (currency.isEmpty()) {
                  this.enchantSlots.setItem(1, ItemStack.EMPTY);
               }

               player.awardStat(Stats.ENCHANT_ITEM);
               if (player instanceof ServerPlayer serverPlayer) {
                  CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, enchantmentItem, enchantmentCost);
               }

               this.enchantSlots.setChanged();
               this.enchantmentSeed.set(player.getEnchantmentSeed());
               this.slotsChanged(this.enchantSlots);
               level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            }
         });
         return true;
      } else {
         Util.logAndPauseIfInIde(player.getPlainTextName() + " pressed invalid button id: " + buttonId);
         return false;
      }
   }

   private List<EnchantmentInstance> getEnchantmentList(final RegistryAccess access, final ItemStack itemStack, final int slot, final int enchantmentCost) {
      this.random.setSeed(this.enchantmentSeed.get() + slot);
      Optional<HolderSet.Named<Enchantment>> tag = access.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
      if (tag.isEmpty()) {
         return List.of();
      }

      List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(this.random, itemStack, enchantmentCost, tag.get().stream());
      if (itemStack.is(Items.BOOK) && list.size() > 1) {
         list.remove(this.random.nextInt(list.size()));
      }

      return list;
   }

   public int getGoldCount() {
      ItemStack goldStack = this.enchantSlots.getItem(1);
      return goldStack.isEmpty() ? 0 : goldStack.getCount();
   }

   public int getEnchantmentSeed() {
      return this.enchantmentSeed.get();
   }

   @Override
   public void removed(final Player player) {
      super.removed(player);
      this.access.execute((level, pos) -> this.clearContainer(player, this.enchantSlots));
   }

   @Override
   public boolean stillValid(final Player player) {
      return stillValid(this.access, player, Blocks.ENCHANTING_TABLE);
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      ItemStack clicked = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         clicked = stack.copy();
         if (slotIndex == 0) {
            if (!this.moveItemStackTo(stack, 2, 38, true)) {
               return ItemStack.EMPTY;
            }
         } else if (slotIndex == 1) {
            if (!this.moveItemStackTo(stack, 2, 38, true)) {
               return ItemStack.EMPTY;
            }
         } else if (stack.is(Items.LAPIS_LAZULI)) {
            if (!this.moveItemStackTo(stack, 1, 2, true)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (this.slots.get(0).hasItem() || !this.slots.get(0).mayPlace(stack)) {
               return ItemStack.EMPTY;
            }

            ItemStack singleItem = stack.copyWithCount(1);
            stack.shrink(1);
            this.slots.get(0).setByPlayer(singleItem);
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
}
