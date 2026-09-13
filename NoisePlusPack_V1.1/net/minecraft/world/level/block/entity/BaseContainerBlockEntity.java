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

   protected BaseContainerBlockEntity(BlockEntityType<?> p_155076_, BlockPos p_155077_, BlockState p_155078_) {
      super(p_155076_, p_155077_, p_155078_);
   }

   @Override
   protected void loadAdditional(ValueInput p_406372_) {
      super.loadAdditional(p_406372_);
      this.lockKey = LockCode.fromTag(p_406372_);
      this.name = parseCustomNameSafe(p_406372_, "CustomName");
   }

   @Override
   protected void saveAdditional(ValueOutput p_406929_) {
      super.saveAdditional(p_406929_);
      this.lockKey.addToTag(p_406929_);
      p_406929_.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
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

   public boolean canOpen(Player p_58645_) {
      return this.lockKey.canUnlock(p_58645_);
   }

   public static void sendChestLockedNotifications(Vec3 p_456313_, Player p_454268_, Component p_454684_) {
      Level level = p_454268_.level();
      p_454268_.displayClientMessage(Component.translatable("container.isLocked", p_454684_), true);
      if (!level.isClientSide()) {
         level.playSound(null, p_456313_.x(), p_456313_.y(), p_456313_.z(), SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   public boolean isLocked() {
      return !this.lockKey.equals(LockCode.NO_LOCK);
   }

   protected abstract NonNullList<ItemStack> getItems();

   protected abstract void setItems(NonNullList<ItemStack> var1);

   @Override
   public boolean isEmpty() {
      for (ItemStack itemstack : this.getItems()) {
         if (!itemstack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getItem(int p_334660_) {
      return this.getItems().get(p_334660_);
   }

   @Override
   public ItemStack removeItem(int p_333934_, int p_332088_) {
      ItemStack itemstack = ContainerHelper.removeItem(this.getItems(), p_333934_, p_332088_);
      if (!itemstack.isEmpty()) {
         this.setChanged();
      }

      return itemstack;
   }

   @Override
   public ItemStack removeItemNoUpdate(int p_329940_) {
      return ContainerHelper.takeItem(this.getItems(), p_329940_);
   }

   @Override
   public void setItem(int p_331067_, ItemStack p_333112_) {
      this.getItems().set(p_331067_, p_333112_);
      p_333112_.limitSize(this.getMaxStackSize(p_333112_));
      this.setChanged();
   }

   @Override
   public boolean stillValid(Player p_330935_) {
      return Container.stillValidBlockEntity(this, p_330935_);
   }

   @Override
   public void clearContent() {
      this.getItems().clear();
   }

   @Override
   public @Nullable AbstractContainerMenu createMenu(int p_58641_, Inventory p_58642_, Player p_58643_) {
      if (this.canOpen(p_58643_)) {
         return this.createMenu(p_58641_, p_58642_);
      }

      sendChestLockedNotifications(this.getBlockPos().getCenter(), p_58643_, this.getDisplayName());
      return null;
   }

   protected abstract AbstractContainerMenu createMenu(int var1, Inventory var2);

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_392615_) {
      super.applyImplicitComponents(p_392615_);
      this.name = p_392615_.get(DataComponents.CUSTOM_NAME);
      this.lockKey = p_392615_.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
      p_392615_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder p_336292_) {
      super.collectImplicitComponents(p_336292_);
      p_336292_.set(DataComponents.CUSTOM_NAME, this.name);
      if (this.isLocked()) {
         p_336292_.set(DataComponents.LOCK, this.lockKey);
      }

      p_336292_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
   }

   @Override
   public void removeComponentsFromTag(ValueOutput p_407613_) {
      p_407613_.discard("CustomName");
      p_407613_.discard("lock");
      p_407613_.discard("Items");
   }
}
