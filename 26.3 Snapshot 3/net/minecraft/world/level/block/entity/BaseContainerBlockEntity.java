package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class BaseContainerBlockEntity extends BlockEntity implements Container, MenuProvider, Nameable {
   private LockCode lockKey = LockCode.NO_LOCK;
   private @Nullable Component name;

   protected BaseContainerBlockEntity(final BlockEntityType<?> type, final BlockPos worldPosition, final BlockState blockState) {
      super(type, worldPosition, blockState);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.lockKey = LockCode.fromTag(input);
      this.name = parseCustomNameSafe(input, "CustomName");
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      this.lockKey.addToTag(output);
      output.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
   }

   @Override
   public Component getName() {
      return this.name != null ? this.name : this.getDefaultName();
   }

   @Override
   public Component getDisplayName() {
      return this.getName();
   }

   @Override
   public @Nullable Component getCustomName() {
      return this.name;
   }

   protected abstract Component getDefaultName();

   public boolean canOpen(final Player player) {
      return this.lockKey.canUnlock(player);
   }

   public static void sendChestLockedNotifications(final Vec3 pos, final Player player, final Component displayName) {
      Level level = player.level();
      player.sendOverlayMessage(Component.translatable("container.isLocked", displayName));
      if (!level.isClientSide()) {
         level.playSound(null, pos.x(), pos.y(), pos.z(), SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   public boolean isLocked() {
      return !this.lockKey.equals(LockCode.NO_LOCK);
   }

   protected abstract NonNullList<ItemStack> getItems();

   protected abstract void setItems(NonNullList<ItemStack> items);

   @Override
   public boolean isEmpty() {
      for (ItemStack itemStack : this.getItems()) {
         if (!itemStack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getItem(final int slot) {
      return this.getItems().get(slot);
   }

   @Override
   public ItemStack removeItem(final int slot, final int count) {
      ItemStack result = ContainerHelper.removeItem(this.getItems(), slot, count);
      if (!result.isEmpty()) {
         this.setChanged();
      }

      return result;
   }

   @Override
   public ItemStack removeItemNoUpdate(final int slot) {
      return ContainerHelper.takeItem(this.getItems(), slot);
   }

   @Override
   public void setItem(final int slot, final ItemStack itemStack) {
      this.getItems().set(slot, itemStack);
      itemStack.limitSize(this.getMaxStackSize(itemStack));
      this.setChanged();
   }

   @Override
   public boolean stillValid(final Player player) {
      return Container.stillValidBlockEntity(this, player);
   }

   @Override
   public void clearContent() {
      this.getItems().clear();
   }

   @Override
   public @Nullable AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
      if (this.canOpen(player)) {
         return this.createMenu(containerId, inventory);
      }

      sendChestLockedNotifications(Vec3.atCenterOf(this.getBlockPos()), player, this.getDisplayName());
      return null;
   }

   protected abstract AbstractContainerMenu createMenu(final int containerId, final Inventory inventory);

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      super.applyImplicitComponents(components);
      this.name = components.get(DataComponents.CUSTOM_NAME);
      this.lockKey = components.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
      components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
   }

   @Override
   protected void collectImplicitComponents(final DataComponentMap.Builder components) {
      super.collectImplicitComponents(components);
      components.set(DataComponents.CUSTOM_NAME, this.name);
      if (this.isLocked()) {
         components.set(DataComponents.LOCK, this.lockKey);
      }

      components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
   }

   @Override
   public void removeComponentsFromTag(final ValueOutput output) {
      output.discard("CustomName");
      output.discard("lock");
      output.discard("Items");
   }
}
