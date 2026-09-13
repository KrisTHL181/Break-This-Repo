package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class MinecartChest extends AbstractMinecartContainer {
   public MinecartChest(final EntityType<? extends MinecartChest> type, final Level level) {
      super(type, level);
   }

   @Override
   protected Item getDropItem() {
      return Items.CHEST_MINECART;
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.CHEST_MINECART);
   }

   @Override
   public int getContainerSize() {
      return 27;
   }

   @Override
   public BlockState getDefaultDisplayBlockState() {
      return Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
   }

   @Override
   public int getDefaultDisplayOffset() {
      return 8;
   }

   @Override
   public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory) {
      return ChestMenu.threeRows(containerId, inventory, this);
   }

   @Override
   public void stopOpen(final ContainerUser containerUser) {
      this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.Context.of(containerUser.getLivingEntity()));
   }

   @Override
   public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
      InteractionResult result = this.interactWithContainerVehicle(player);
      if (result.consumesAction() && player.level() instanceof ServerLevel serverLevel) {
         this.gameEvent(GameEvent.CONTAINER_OPEN, player);
         PiglinAi.angerNearbyPiglins(serverLevel, player, true);
      }

      return result;
   }
}
