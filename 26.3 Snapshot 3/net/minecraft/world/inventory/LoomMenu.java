package net.minecraft.world.inventory;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class LoomMenu extends AbstractContainerMenu {
   private static final int PATTERN_NOT_SET = -1;
   private static final int INV_SLOT_START = 4;
   private static final int INV_SLOT_END = 31;
   private static final int USE_ROW_SLOT_START = 31;
   private static final int USE_ROW_SLOT_END = 40;
   private final ContainerLevelAccess access;
   private final DataSlot selectedBannerPatternIndex = DataSlot.standalone();
   private List<Holder<BannerPattern>> selectablePatterns = List.of();
   private Runnable slotUpdateListener = () -> {};
   private final HolderGetter<BannerPattern> patternGetter;
   private final Slot bannerSlot;
   private final Slot dyeSlot;
   private final Slot patternSlot;
   private final Slot resultSlot;
   private long lastSoundTime;
   private final Container inputContainer = new SimpleContainer(3) {
      @Override
      public void setChanged() {
         super.setChanged();
         LoomMenu.this.slotsChanged(this);
         LoomMenu.this.slotUpdateListener.run();
      }
   };
   private final Container outputContainer = new SimpleContainer(1) {
      @Override
      public void setChanged() {
         super.setChanged();
         LoomMenu.this.slotUpdateListener.run();
      }
   };

   public LoomMenu(final int containerId, final Inventory inventory) {
      this(containerId, inventory, ContainerLevelAccess.NULL);
   }

   public LoomMenu(final int containerId, final Inventory inventory, final ContainerLevelAccess access) {
      super(MenuType.LOOM, containerId);
      this.access = access;
      this.bannerSlot = this.addSlot(new Slot(this.inputContainer, 0, 13, 26) {
         @Override
         public boolean mayPlace(final ItemStack itemStack) {
            return itemStack.getItem() instanceof BannerItem;
         }
      });
      this.dyeSlot = this.addSlot(new Slot(this.inputContainer, 1, 33, 26) {
         @Override
         public boolean mayPlace(final ItemStack itemStack) {
            return LoomMenu.isDyeItem(itemStack);
         }
      });
      this.patternSlot = this.addSlot(new Slot(this.inputContainer, 2, 23, 45) {
         @Override
         public boolean mayPlace(final ItemStack itemStack) {
            return LoomMenu.isPatternItem(itemStack);
         }
      });
      this.resultSlot = this.addSlot(new Slot(this.outputContainer, 0, 143, 57) {
         @Override
         public boolean mayPlace(final ItemStack itemStack) {
            return false;
         }

         @Override
         public void onTake(final Player player, final ItemStack carried) {
            LoomMenu.this.bannerSlot.remove(1);
            LoomMenu.this.dyeSlot.remove(1);
            if (!LoomMenu.this.bannerSlot.hasItem() || !LoomMenu.this.dyeSlot.hasItem()) {
               LoomMenu.this.selectedBannerPatternIndex.set(-1);
            }

            access.execute((level, pos) -> {
               long gameTime = level.getGameTime();
               if (LoomMenu.this.lastSoundTime != gameTime) {
                  level.playSound(null, pos, SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                  LoomMenu.this.lastSoundTime = gameTime;
               }
            });
            super.onTake(player, carried);
         }
      });
      this.addStandardInventorySlots(inventory, 8, 84);
      this.addDataSlot(this.selectedBannerPatternIndex);
      this.patternGetter = inventory.player.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
   }

   private static boolean isPatternItem(final ItemStack itemStack) {
      return itemStack.is(ItemTags.LOOM_PATTERNS) && itemStack.has(DataComponents.PROVIDES_BANNER_PATTERNS);
   }

   private static boolean isDyeItem(final ItemStack itemStack) {
      return itemStack.is(ItemTags.LOOM_DYES) && itemStack.has(DataComponents.DYE);
   }

   @Override
   public boolean stillValid(final Player player) {
      return stillValid(this.access, player, Blocks.LOOM);
   }

   @Override
   public boolean clickMenuButton(final Player player, final int buttonId) {
      if (buttonId >= 0 && buttonId < this.selectablePatterns.size()) {
         this.selectedBannerPatternIndex.set(buttonId);
         this.setupResultSlot(this.selectablePatterns.get(buttonId));
         return true;
      } else {
         return false;
      }
   }

   private List<Holder<BannerPattern>> getSelectablePatterns(final ItemStack patternStack) {
      if (patternStack.isEmpty()) {
         return this.patternGetter.get(BannerPatternTags.NO_ITEM_REQUIRED).<List<Holder<BannerPattern>>>map(ImmutableList::copyOf).orElse(ImmutableList.of());
      }

      HolderSet<BannerPattern> itemPatterns = patternStack.get(DataComponents.PROVIDES_BANNER_PATTERNS);
      return itemPatterns != null ? ImmutableList.copyOf(itemPatterns) : ImmutableList.of();
   }

   private boolean isValidPatternIndex(final int selectedPattern) {
      return selectedPattern >= 0 && selectedPattern < this.selectablePatterns.size();
   }

   @Override
   public void slotsChanged(final Container container) {
      ItemStack bannerStack = this.bannerSlot.getItem();
      ItemStack dyeStack = this.dyeSlot.getItem();
      ItemStack patternStack = this.patternSlot.getItem();
      if (!bannerStack.isEmpty() && !dyeStack.isEmpty()) {
         int selectedPattern = this.selectedBannerPatternIndex.get();
         boolean validPatternIndex = this.isValidPatternIndex(selectedPattern);
         List<Holder<BannerPattern>> previousSelectablePatterns = this.selectablePatterns;
         this.selectablePatterns = this.getSelectablePatterns(patternStack);
         Holder<BannerPattern> patternToDisplay;
         if (this.selectablePatterns.size() == 1) {
            this.selectedBannerPatternIndex.set(0);
            patternToDisplay = this.selectablePatterns.get(0);
         } else if (!validPatternIndex) {
            this.selectedBannerPatternIndex.set(-1);
            patternToDisplay = null;
         } else {
            Holder<BannerPattern> selectedValue = previousSelectablePatterns.get(selectedPattern);
            int newSelectedIndex = this.selectablePatterns.indexOf(selectedValue);
            if (newSelectedIndex != -1) {
               patternToDisplay = selectedValue;
               this.selectedBannerPatternIndex.set(newSelectedIndex);
            } else {
               patternToDisplay = null;
               this.selectedBannerPatternIndex.set(-1);
            }
         }

         if (patternToDisplay != null) {
            BannerPatternLayers patterns = bannerStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
            boolean hasMaxPatterns = patterns.layers().size() >= 6;
            if (hasMaxPatterns) {
               this.selectedBannerPatternIndex.set(-1);
               this.resultSlot.set(ItemStack.EMPTY);
            } else {
               this.setupResultSlot(patternToDisplay);
            }
         } else {
            this.resultSlot.set(ItemStack.EMPTY);
         }

         this.broadcastChanges();
      } else {
         this.resultSlot.set(ItemStack.EMPTY);
         this.selectablePatterns = List.of();
         this.selectedBannerPatternIndex.set(-1);
      }
   }

   public List<Holder<BannerPattern>> getSelectablePatterns() {
      return this.selectablePatterns;
   }

   public int getSelectedBannerPatternIndex() {
      return this.selectedBannerPatternIndex.get();
   }

   public void registerUpdateListener(final Runnable slotUpdateListener) {
      this.slotUpdateListener = slotUpdateListener;
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      ItemStack clicked = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         clicked = stack.copy();
         if (slotIndex == this.resultSlot.index) {
            if (!this.moveItemStackTo(stack, 4, 40, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(stack, clicked);
         } else if (slotIndex != this.dyeSlot.index && slotIndex != this.bannerSlot.index && slotIndex != this.patternSlot.index) {
            if (stack.getItem() instanceof BannerItem) {
               if (!this.moveItemStackTo(stack, this.bannerSlot.index, this.bannerSlot.index + 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (isDyeItem(stack)) {
               if (!this.moveItemStackTo(stack, this.dyeSlot.index, this.dyeSlot.index + 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (isPatternItem(stack)) {
               if (!this.moveItemStackTo(stack, this.patternSlot.index, this.patternSlot.index + 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (slotIndex >= 4 && slotIndex < 31) {
               if (!this.moveItemStackTo(stack, 31, 40, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (slotIndex >= 31 && slotIndex < 40 && !this.moveItemStackTo(stack, 4, 31, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, 4, 40, false)) {
            return ItemStack.EMPTY;
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

   @Override
   public void removed(final Player player) {
      super.removed(player);
      this.access.execute((level, pos) -> this.clearContainer(player, this.inputContainer));
   }

   private void setupResultSlot(final Holder<BannerPattern> pattern) {
      ItemStack bannerStack = this.bannerSlot.getItem();
      ItemStack dyeStack = this.dyeSlot.getItem();
      ItemStack result = ItemStack.EMPTY;
      if (!bannerStack.isEmpty() && !dyeStack.isEmpty()) {
         DyeColor patternColor = dyeStack.get(DataComponents.DYE);
         if (patternColor != null) {
            result = bannerStack.copyWithCount(1);
            result.update(
               DataComponents.BANNER_PATTERNS,
               BannerPatternLayers.EMPTY,
               layers -> new BannerPatternLayers.Builder().addAll(layers).add(pattern, patternColor).build()
            );
         }
      }

      if (!ItemStack.matches(result, this.resultSlot.getItem())) {
         this.resultSlot.set(result);
      }
   }

   public Slot getBannerSlot() {
      return this.bannerSlot;
   }

   public Slot getDyeSlot() {
      return this.dyeSlot;
   }

   public Slot getPatternSlot() {
      return this.patternSlot;
   }

   public Slot getResultSlot() {
      return this.resultSlot;
   }
}
