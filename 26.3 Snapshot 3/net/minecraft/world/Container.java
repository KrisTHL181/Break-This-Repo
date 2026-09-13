package net.minecraft.world;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public interface Container extends Clearable, Iterable<ItemStack>, SlotProvider {
   float DEFAULT_DISTANCE_BUFFER = 4.0F;

   int getContainerSize();

   boolean isEmpty();

   ItemStack getItem(int slot);

   ItemStack removeItem(int slot, int count);

   ItemStack removeItemNoUpdate(int slot);

   void setItem(int slot, ItemStack itemStack);

   default int getMaxStackSize() {
      return 99;
   }

   default int getMaxStackSize(final ItemStack itemStack) {
      return Math.min(this.getMaxStackSize(), itemStack.getMaxStackSize());
   }

   void setChanged();

   boolean stillValid(Player player);

   default void startOpen(final ContainerUser containerUser) {
   }

   default void stopOpen(final ContainerUser containerUser) {
   }

   default List<ContainerUser> getEntitiesWithContainerOpen() {
      return List.of();
   }

   default boolean canPlaceItem(final int slot, final ItemStack itemStack) {
      return true;
   }

   default boolean canTakeItem(final Container into, final int slot, final ItemStack itemStack) {
      return true;
   }

   default int countItem(final Item item) {
      int count = 0;

      for (ItemStack slotItem : this) {
         if (slotItem.getItem().equals(item)) {
            count += slotItem.getCount();
         }
      }

      return count;
   }

   default boolean hasAnyOf(final Set<Item> item) {
      return this.hasAnyMatching(stack -> !stack.isEmpty() && item.contains(stack.getItem()));
   }

   default boolean hasAnyMatching(final Predicate<ItemStack> predicate) {
      for (ItemStack slotItem : this) {
         if (predicate.test(slotItem)) {
            return true;
         }
      }

      return false;
   }

   static boolean stillValidBlockEntity(final BlockEntity blockEntity, final Player player) {
      return stillValidBlockEntity(blockEntity, player, 4.0F);
   }

   static boolean stillValidBlockEntity(final BlockEntity blockEntity, final Player player, final float distanceBuffer) {
      Level level = blockEntity.getLevel();
      BlockPos worldPosition = blockEntity.getBlockPos();
      if (level == null) {
         return false;
      } else {
         return level.getBlockEntity(worldPosition) != blockEntity ? false : player.isWithinBlockInteractionRange(worldPosition, distanceBuffer);
      }
   }

   @Override
   default @Nullable SlotAccess getSlot(final int slot) {
      return slot >= 0 && slot < this.getContainerSize() ? new SlotAccess() {
         @Override
         public ItemStack get() {
            return Container.this.getItem(slot);
         }

         @Override
         public boolean set(final ItemStack itemStack) {
            Container.this.setItem(slot, itemStack);
            return true;
         }
      } : null;
   }

   @Override
   default Iterator<ItemStack> iterator() {
      return new Container.ContainerIterator(this);
   }

   class ContainerIterator implements Iterator<ItemStack> {
      private final Container container;
      private int index;
      private final int size;

      public ContainerIterator(final Container container) {
         this.container = container;
         this.size = container.getContainerSize();
      }

      @Override
      public boolean hasNext() {
         return this.index < this.size;
      }

      public ItemStack next() {
         if (!this.hasNext()) {
            throw new NoSuchElementException();
         } else {
            return this.container.getItem(this.index++);
         }
      }
   }
}
