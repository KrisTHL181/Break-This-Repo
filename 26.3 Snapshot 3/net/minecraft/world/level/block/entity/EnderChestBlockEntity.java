package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class EnderChestBlockEntity extends BlockEntity implements LidBlockEntity {
   private final ChestLidController chestLidController = new ChestLidController();
   private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
      @Override
      protected void onOpen(final Level level, final BlockPos pos, final BlockState blockState) {
         level.playSound(
            null,
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            SoundEvents.ENDER_CHEST_OPEN,
            SoundSource.BLOCKS,
            0.5F,
            level.getRandom().nextFloat() * 0.1F + 0.9F
         );
      }

      @Override
      protected void onClose(final Level level, final BlockPos pos, final BlockState blockState) {
         level.playSound(
            null,
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            SoundEvents.ENDER_CHEST_CLOSE,
            SoundSource.BLOCKS,
            0.5F,
            level.getRandom().nextFloat() * 0.1F + 0.9F
         );
      }

      @Override
      protected void openerCountChanged(final Level level, final BlockPos pos, final BlockState blockState, final int previous, final int current) {
         level.blockEvent(EnderChestBlockEntity.this.worldPosition, Blocks.ENDER_CHEST, 1, current);
      }

      @Override
      public boolean isOwnContainer(final Player player) {
         return player.getEnderChestInventory().isActiveChest(EnderChestBlockEntity.this);
      }
   };

   public EnderChestBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.ENDER_CHEST, worldPosition, blockState);
   }

   public static void lidAnimateTick(final Level level, final BlockPos pos, final BlockState state, final EnderChestBlockEntity entity) {
      entity.chestLidController.tickLid();
   }

   @Override
   public boolean triggerEvent(final int b0, final int b1) {
      if (b0 == 1) {
         this.chestLidController.shouldBeOpen(b1 > 0);
         return true;
      } else {
         return super.triggerEvent(b0, b1);
      }
   }

   public void startOpen(final ContainerUser containerUser) {
      if (!this.remove && !containerUser.getLivingEntity().isSpectator()) {
         this.openersCounter
            .incrementOpeners(
               containerUser.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState(), containerUser.getContainerInteractionRange()
            );
      }
   }

   public void stopOpen(final ContainerUser containerUser) {
      if (!this.remove && !containerUser.getLivingEntity().isSpectator()) {
         this.openersCounter.decrementOpeners(containerUser.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   public boolean stillValid(final Player player) {
      return Container.stillValidBlockEntity(this, player);
   }

   public void recheckOpen() {
      if (!this.remove) {
         this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   @Override
   public float getOpenNess(final float a) {
      return this.chestLidController.getOpenness(a);
   }
}
