package net.minecraft.world.entity.projectile.throwableitemprojectile;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class ThrowableItemProjectile extends ThrowableProjectile implements ItemSupplier {
   private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(
      ThrowableItemProjectile.class, EntityDataSerializers.ITEM_STACK
   );

   public ThrowableItemProjectile(EntityType<? extends ThrowableItemProjectile> p_453788_, Level p_454624_) {
      super(p_453788_, p_454624_);
   }

   public ThrowableItemProjectile(
      EntityType<? extends ThrowableItemProjectile> p_455172_, double p_459678_, double p_458459_, double p_456224_, Level p_460643_, ItemStack p_450579_
   ) {
      super(p_455172_, p_459678_, p_458459_, p_456224_, p_460643_);
      this.setItem(p_450579_);
   }

   public ThrowableItemProjectile(EntityType<? extends ThrowableItemProjectile> p_460374_, LivingEntity p_455900_, Level p_454434_, ItemStack p_455736_) {
      this(p_460374_, p_455900_.getX(), p_455900_.getEyeY() - 0.1F, p_455900_.getZ(), p_454434_, p_455736_);
      this.setOwner(p_455900_);
   }

   public void setItem(ItemStack p_453937_) {
      this.getEntityData().set(DATA_ITEM_STACK, p_453937_.copyWithCount(1));
   }

   protected abstract Item getDefaultItem();

   @Override
   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM_STACK);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_454750_) {
      p_454750_.define(DATA_ITEM_STACK, new ItemStack(this.getDefaultItem()));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_460760_) {
      super.addAdditionalSaveData(p_460760_);
      p_460760_.store("Item", ItemStack.CODEC, this.getItem());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_453133_) {
      super.readAdditionalSaveData(p_453133_);
      this.setItem(p_453133_.<ItemStack>read("Item", ItemStack.CODEC).orElseGet(() -> new ItemStack(this.getDefaultItem())));
   }
}
