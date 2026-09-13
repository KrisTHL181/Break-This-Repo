package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MinecartHopper extends AbstractMinecartContainer implements Hopper {
   private static final boolean DEFAULT_ENABLED = true;
   private boolean enabled = true;
   private boolean consumedItemThisFrame = false;

   public MinecartHopper(EntityType<? extends MinecartHopper> p_454757_, Level p_454039_) {
      super(p_454757_, p_454039_);
   }

   @Override
   public BlockState getDefaultDisplayBlockState() {
      return Blocks.HOPPER.defaultBlockState();
   }

   @Override
   public int getDefaultDisplayOffset() {
      return 1;
   }

   @Override
   public int getContainerSize() {
      return 5;
   }

   @Override
   public void activateMinecart(ServerLevel p_459702_, int p_456027_, int p_455616_, int p_459310_, boolean p_454781_) {
      boolean flag = !p_454781_;
      if (flag != this.isEnabled()) {
         this.setEnabled(flag);
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean p_454855_) {
      this.enabled = p_454855_;
   }

   @Override
   public double getLevelX() {
      return this.getX();
   }

   @Override
   public double getLevelY() {
      return this.getY() + 0.5;
   }

   @Override
   public double getLevelZ() {
      return this.getZ();
   }

   @Override
   public boolean isGridAligned() {
      return false;
   }

   @Override
   public void tick() {
      this.consumedItemThisFrame = false;
      super.tick();
      this.tryConsumeItems();
   }

   @Override
   protected double makeStepAlongTrack(BlockPos p_452157_, RailShape p_459548_, double p_460043_) {
      double d0 = super.makeStepAlongTrack(p_452157_, p_459548_, p_460043_);
      this.tryConsumeItems();
      return d0;
   }

   private void tryConsumeItems() {
      if (!this.level().isClientSide() && this.isAlive() && this.isEnabled() && !this.consumedItemThisFrame && this.suckInItems()) {
         this.consumedItemThisFrame = true;
         this.setChanged();
      }
   }

   public boolean suckInItems() {
      if (HopperBlockEntity.suckInItems(this.level(), this)) {
         return true;
      }

      for (ItemEntity itementity : this.level()
         .getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(0.25, 0.0, 0.25), EntitySelector.ENTITY_STILL_ALIVE)) {
         if (HopperBlockEntity.addItem(this, itementity)) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected Item getDropItem() {
      return Items.HOPPER_MINECART;
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.HOPPER_MINECART);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_452719_) {
      super.addAdditionalSaveData(p_452719_);
      p_452719_.putBoolean("Enabled", this.enabled);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_458421_) {
      super.readAdditionalSaveData(p_458421_);
      this.enabled = p_458421_.getBooleanOr("Enabled", true);
   }

   @Override
   public AbstractContainerMenu createMenu(int p_453362_, Inventory p_456209_) {
      return new HopperMenu(p_453362_, p_456209_, this);
   }
}
