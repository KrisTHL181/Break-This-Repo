package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BarrelBlockEntity extends RandomizableContainerBlockEntity {
   private static final Component DEFAULT_NAME = Component.translatable("container.barrel");
   private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
   private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
      @Override
      protected void onOpen(Level p_155062_, BlockPos p_155063_, BlockState p_155064_) {
         BarrelBlockEntity.this.playSound(p_155064_, SoundEvents.BARREL_OPEN);
         BarrelBlockEntity.this.updateBlockState(p_155064_, true);
      }

      @Override
      protected void onClose(Level p_155072_, BlockPos p_155073_, BlockState p_155074_) {
         BarrelBlockEntity.this.playSound(p_155074_, SoundEvents.BARREL_CLOSE);
         BarrelBlockEntity.this.updateBlockState(p_155074_, false);
      }

      @Override
      protected void openerCountChanged(Level p_155066_, BlockPos p_155067_, BlockState p_155068_, int p_155069_, int p_155070_) {
      }

      @Override
      public boolean isOwnContainer(Player p_155060_) {
         if (p_155060_.containerMenu instanceof ChestMenu) {
            Container container = ((ChestMenu)p_155060_.containerMenu).getContainer();
            return container == BarrelBlockEntity.this;
         } else {
            return false;
         }
      }
   };

   public BarrelBlockEntity(BlockPos p_155052_, BlockState p_155053_) {
      super(BlockEntityType.BARREL, p_155052_, p_155053_);
   }

   @Override
   protected void saveAdditional(ValueOutput p_410315_) {
      super.saveAdditional(p_410315_);
      if (!this.trySaveLootTable(p_410315_)) {
         ContainerHelper.saveAllItems(p_410315_, this.items);
      }
   }

   @Override
   protected void loadAdditional(ValueInput p_410699_) {
      super.loadAdditional(p_410699_);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(p_410699_)) {
         ContainerHelper.loadAllItems(p_410699_, this.items);
      }
   }

   @Override
   public int getContainerSize() {
      return 27;
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> p_58610_) {
      this.items = p_58610_;
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   @Override
   protected AbstractContainerMenu createMenu(int p_58598_, Inventory p_58599_) {
      return ChestMenu.threeRows(p_58598_, p_58599_, this);
   }

   @Override
   public void startOpen(ContainerUser p_430862_) {
      if (!this.remove && !p_430862_.getLivingEntity().isSpectator()) {
         this.openersCounter
            .incrementOpeners(p_430862_.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState(), p_430862_.getContainerInteractionRange());
      }
   }

   @Override
   public void stopOpen(ContainerUser p_429340_) {
      if (!this.remove && !p_429340_.getLivingEntity().isSpectator()) {
         this.openersCounter.decrementOpeners(p_429340_.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   @Override
   public List<ContainerUser> getEntitiesWithContainerOpen() {
      return this.openersCounter.getEntitiesWithContainerOpen(this.getLevel(), this.getBlockPos());
   }

   public void recheckOpen() {
      if (!this.remove) {
         this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   void updateBlockState(BlockState p_58607_, boolean p_58608_) {
      this.level.setBlock(this.getBlockPos(), p_58607_.setValue(BarrelBlock.OPEN, p_58608_), 3);
   }

   void playSound(BlockState p_58601_, SoundEvent p_58602_) {
      Vec3i vec3i = p_58601_.getValue(BarrelBlock.FACING).getUnitVec3i();
      double d0 = this.worldPosition.getX() + 0.5 + vec3i.getX() / 2.0;
      double d1 = this.worldPosition.getY() + 0.5 + vec3i.getY() / 2.0;
      double d2 = this.worldPosition.getZ() + 0.5 + vec3i.getZ() / 2.0;
      this.level.playSound(null, d0, d1, d2, p_58602_, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
   }
}
