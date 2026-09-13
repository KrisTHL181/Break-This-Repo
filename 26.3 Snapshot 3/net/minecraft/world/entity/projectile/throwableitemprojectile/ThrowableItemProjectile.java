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

   public ThrowableItemProjectile(final EntityType<? extends ThrowableItemProjectile> type, final Level level) {
      super(type, level);
   }

   public ThrowableItemProjectile(
      final EntityType<? extends ThrowableItemProjectile> type, final double x, final double y, final double z, final Level level, final ItemStack itemStack
   ) {
      super(type, x, y, z, level);
      this.setItem(itemStack);
   }

   public ThrowableItemProjectile(
      final EntityType<? extends ThrowableItemProjectile> type, final LivingEntity owner, final Level level, final ItemStack itemStack
   ) {
      this(type, owner.getX(), owner.getEyeY() - 0.1F, owner.getZ(), level, itemStack);
      this.setOwner(owner);
   }

   public void setItem(final ItemStack source) {
      this.getEntityData().set(DATA_ITEM_STACK, source.copyWithCount(1));
   }

   protected abstract Item getDefaultItem();

   @Override
   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM_STACK);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      entityData.define(DATA_ITEM_STACK, new ItemStack(this.getDefaultItem()));
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.store("Item", ItemStack.CODEC, this.getItem());
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setItem(input.<ItemStack>read("Item", ItemStack.CODEC).orElseGet(() -> new ItemStack(this.getDefaultItem())));
   }
}
