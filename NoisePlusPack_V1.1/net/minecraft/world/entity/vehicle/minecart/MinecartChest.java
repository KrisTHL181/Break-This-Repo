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

public class MinecartChest extends AbstractMinecartContainer {
   public MinecartChest(EntityType<? extends MinecartChest> p_452861_, Level p_453439_) {
      super(p_452861_, p_453439_);
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
   public AbstractContainerMenu createMenu(int p_459831_, Inventory p_457694_) {
      return ChestMenu.threeRows(p_459831_, p_457694_, this);
   }

   @Override
   public void stopOpen(ContainerUser p_460992_) {
      this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.Context.of(p_460992_.getLivingEntity()));
   }

   @Override
   public InteractionResult interact(Player p_451742_, InteractionHand p_453847_) {
      InteractionResult interactionresult = this.interactWithContainerVehicle(p_451742_);
      if (interactionresult.consumesAction() && p_451742_.level() instanceof ServerLevel serverlevel) {
         this.gameEvent(GameEvent.CONTAINER_OPEN, p_451742_);
         PiglinAi.angerNearbyPiglins(serverlevel, p_451742_, true);
      }

      return interactionresult;
   }
}
