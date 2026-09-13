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
      public void broadcastEvent(Level p_451201_, BlockPos p_454356_, int p_454162_) {
         p_451201_.broadcastEntityEvent(MinecartSpawner.this, (byte)p_454162_);
      }
   };
   private final Runnable ticker;

   public MinecartSpawner(EntityType<? extends MinecartSpawner> p_457492_, Level p_458146_) {
      super(p_457492_, p_458146_);
      this.ticker = this.createTicker(p_458146_);
   }

   @Override
   protected Item getDropItem() {
      return Items.MINECART;
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.MINECART);
   }

   private Runnable createTicker(Level p_451990_) {
      return p_451990_ instanceof ServerLevel
         ? () -> this.spawner.serverTick((ServerLevel)p_451990_, this.blockPosition())
         : () -> this.spawner.clientTick(p_451990_, this.blockPosition());
   }

   @Override
   public BlockState getDefaultDisplayBlockState() {
      return Blocks.SPAWNER.defaultBlockState();
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_455377_) {
      super.readAdditionalSaveData(p_455377_);
      this.spawner.load(this.level(), this.blockPosition(), p_455377_);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_459218_) {
      super.addAdditionalSaveData(p_459218_);
      this.spawner.save(p_459218_);
   }

   @Override
   public void handleEntityEvent(byte p_450874_) {
      this.spawner.onEventTriggered(this.level(), p_450874_);
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
