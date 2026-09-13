package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MinecartSpawner extends AbstractMinecart {
   private final BaseSpawner spawner = new BaseSpawner() {
      @Override
      public void broadcastEvent(final Level level, final BlockPos pos, final int id) {
         level.broadcastEntityEvent(MinecartSpawner.this, (byte)id);
      }
   };
   private final Runnable ticker;

   public MinecartSpawner(final EntityType<? extends MinecartSpawner> type, final Level level) {
      super(type, level);
      this.ticker = this.createTicker(level);
   }

   @Override
   protected Item getDropItem() {
      return Items.MINECART;
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.MINECART);
   }

   private Runnable createTicker(final Level level) {
      return level instanceof ServerLevel serverLevel
         ? () -> this.spawner.serverTick(serverLevel, this.blockPosition())
         : () -> this.spawner.clientTick(level, this.blockPosition());
   }

   @Override
   public BlockState getDefaultDisplayBlockState() {
      return Blocks.SPAWNER.defaultBlockState();
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.spawner.load(this.level(), this.blockPosition(), input);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      this.spawner.save(output);
   }

   @Override
   public void handleEntityEvent(final byte id) {
      this.spawner.onEventTriggered(this.level(), id);
   }

   @Override
   public void tick() {
      super.tick();
      this.ticker.run();
   }

   public BaseSpawner getSpawner() {
      return this.spawner;
   }
}
