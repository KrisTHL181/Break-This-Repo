package net.minecraft.world.inventory;

import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
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
   static final Identifier EMPTY_SLOT_LAPIS_LAZULI = Identifier.withDefaultNamespace("container/slot/lapis_lazuli");
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

   public EnchantmentMenu(int p_39454_, Inventory p_39455_) {
      this(p_39454_, p_39455_, ContainerLevelAccess.NULL);
   }

   public EnchantmentMenu(int p_39457_, Inventory p_39458_, ContainerLevelAccess p_39459_) {
      super(MenuType.ENCHANTMENT, p_39457_);
      this.access = p_39459_;
      this.addSlot(new Slot(this.enchantSlots, 0, 15, 47) {
         @Override
         public int getMaxStackSize() {
            return 1;
         }
      });
      this.addSlot(new Slot(this.enchantSlots, 1, 35, 47) {
         @Override
         public boolean mayPlace(ItemStack p_39517_) {
            return p_39517_.is(Items.LAPIS_LAZULI);
         }

         @Override
         public Identifier getNoItemIcon() {
            return EnchantmentMenu.EMPTY_SLOT_LAPIS_LAZULI;
         }
      });
      this.addStandardInventorySlots(p_39458_, 8, 84);
      this.addDataSlot(DataSlot.shared(this.costs, 0));
      this.addDataSlot(DataSlot.shared(this.costs, 1));
      this.addDataSlot(DataSlot.shared(this.costs, 2));
      this.addDataSlot(this.enchantmentSeed).set(p_39458_.player.getEnchantmentSeed());
      this.addDataSlot(DataSlot.shared(this.enchantClue, 0));
      this.addDataSlot(DataSlot.shared(this.enchantClue, 1));
      this.addDataSlot(DataSlot.shared(this.enchantClue, 2));
      this.addDataSlot(DataSlot.shared(this.levelClue, 0));
      this.addDataSlot(DataSlot.shared(this.levelClue, 1));
      this.addDataSlot(DataSlot.shared(this.levelClue, 2));
   }

   @Override
   public void slotsChanged(Container p_39461_) {
      if (p_39461_ == this.enchantSlots) {
         ItemStack itemstack = p_39461_.getItem(0);
         if (!itemstack.isEmpty() && itemstack.isEnchantable()) {
            this.access.execute((p_449765_, p_449766_) -> {
               IdMap<Holder<Enchantment>> idmap = p_449765_.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
               int j = 0;

               for (BlockPos blockpos : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
                  if (EnchantingTableBlock.isValidBookShelf(p_449765_, p_449766_, blockpos)) {
                     j++;
                  }
               }

               this.random.setSeed(this.enchantmentSeed.get());

               for (int k = 0; k < 3; k++) {
                  this.costs[k] = EnchantmentHelper.getEnchantmentCost(this.random, k, j, itemstack);
                  this.enchantClue[k] = -1;
                  this.levelClue[k] = -1;
                  if (this.costs[k] < k + 1) {
                     this.costs[k] = 0;
                  }
               }

               for (int l = 0; l < 3; l++) {
                  if (this.costs[l] > 0) {
                     List<EnchantmentInstance> list = this.getEnchantmentList(p_449765_.registryAccess(), itemstack, l, this.costs[l]);
                     if (!list.isEmpty()) {
                        EnchantmentInstance enchantmentinstance = list.get(this.random.nextInt(list.size()));
                        this.enchantClue[l] = idmap.getId(enchantmentinstance.enchantment());
                        this.levelClue[l] = enchantmentinstance.level();
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
   public boolean clickMenuButton(Player p_39465_, int p_39466_) {
      if (p_39466_ >= 0 && p_39466_ < this.costs.length) {
         ItemStack itemstack = this.enchantSlots.getItem(0);
         ItemStack itemstack1 = this.enchantSlots.getItem(1);
         int i = p_39466_ + 1;
         if ((itemstack1.isEmpty() || itemstack1.getCount() < i) && !p_39465_.hasInfiniteMaterials()) {
            return false;
         } else if (this.costs[p_39466_] > 0
            && !itemstack.isEmpty()
            && (p_39465_.experienceLevel >= i && p_39465_.experienceLevel >= this.costs[p_39466_] || p_39465_.hasInfiniteMaterials())) {
            this.access.execute((p_341512_, p_341513_) -> {
               ItemStack itemstack2 = itemstack;
               List<EnchantmentInstance> list = this.getEnchantmentList(p_341512_.registryAccess(), itemstack, p_39466_, this.costs[p_39466_]);
               if (!list.isEmpty()) {
                  p_39465_.onEnchantmentPerformed(itemstack, i);
                  if (itemstack.is(Items.BOOK)) {
                     itemstack2 = itemstack.transmuteCopy(Items.ENCHANTED_BOOK);
                     this.enchantSlots.setItem(0, itemstack2);
                  }

                  for (EnchantmentInstance enchantmentinstance : list) {
                     itemstack2.enchant(enchantmentinstance.enchantment(), enchantmentinstance.level());
                  }

                  itemstack1.consume(i, p_39465_);
                  if (itemstack1.isEmpty()) {
                     this.enchantSlots.setItem(1, ItemStack.EMPTY);
                  }

                  p_39465_.awardStat(Stats.ENCHANT_ITEM);
                  if (p_39465_ instanceof ServerPlayer) {
                     CriteriaTriggers.ENCHANTED_ITEM.trigger((ServerPlayer)p_39465_, itemstack2, i);
                  }

                  this.enchantSlots.setChanged();
                  this.enchantmentSeed.set(p_39465_.getEnchantmentSeed());
                  this.slotsChanged(this.enchantSlots);
                  p_341512_.playSound(null, p_341513_, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, p_341512_.random.nextFloat() * 0.1F + 0.9F);
               }
            });
            return true;
         } else {
            return false;
         }
      } else {
         Util.logAndPauseIfInIde(p_39465_.getPlainTextName() + " pressed invalid button id: " + p_39466_);
         return false;
      }
   }

   private List<EnchantmentInstance> getEnchantmentList(RegistryAccess p_342984_, ItemStack p_39472_, int p_39473_, int p_39474_) {
      this.random.setSeed(this.enchantmentSeed.get() + p_39473_);
      Optional<HolderSet.Named<Enchantment>> optional = p_342984_.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
      if (optional.isEmpty()) {
         return List.of();
      }

      List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(this.random, p_39472_, p_39474_, optional.get().stream());
      if (p_39472_.is(Items.BOOK) && list.size() > 1) {
         list.remove(this.random.nextInt(list.size()));
      }

      return list;
   }

   public int getGoldCount() {
      ItemStack itemstack = this.enchantSlots.getItem(1);
      return itemstack.isEmpty() ? 0 : itemstack.getCount();
   }

   public int getEnchantmentSeed() {
      return this.enchantmentSeed.get();
   }

   @Override
   public void removed(Player p_39488_) {
      super.removed(p_39488_);
      this.access.execute((p_39469_, p_39470_) -> this.clearContainer(p_39488_, this.enchantSlots));
   }

   @Override
   public boolean stillValid(Player p_39463_) {
      return stillValid(this.access, p_39463_, Blocks.ENCHANTING_TABLE);
   }

   @Override
   public ItemStack quickMoveStack(Player p_39490_, int p_39491_) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = this.slots.get(p_39491_);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (p_39491_ == 0) {
            if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
               return ItemStack.EMPTY;
            }
         } else if (p_39491_ == 1) {
            if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
               return ItemStack.EMPTY;
            }
         } else if (itemstack1.is(Items.LAPIS_LAZULI)) {
            if (!this.moveItemStackTo(itemstack1, 1, 2, true)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (this.slots.get(0).hasItem() || !this.slots.get(0).mayPlace(itemstack1)) {
               return ItemStack.EMPTY;
            }

            ItemStack itemstack2 = itemstack1.copyWithCount(1);
            itemstack1.shrink(1);
            this.slots.get(0).setByPlayer(itemstack2);
         }

         if (itemstack1.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(p_39490_, itemstack1);
      }

      return itemstack;
   }
}
