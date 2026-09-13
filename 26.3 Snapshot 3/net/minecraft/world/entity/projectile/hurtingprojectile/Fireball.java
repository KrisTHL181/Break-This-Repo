package net.minecraft.world.entity.projectile.hurtingprojectile;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class Fireball extends AbstractHurtingProjectile implements ItemSupplier {
   private static final float MIN_CAMERA_DISTANCE_SQUARED = 12.25F;
   private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(Fireball.class, EntityDataSerializers.ITEM_STACK);

   public Fireball(final EntityType<? extends Fireball> type, final Level level) {
      super(type, level);
   }

   public Fireball(final EntityType<? extends Fireball> type, final double x, final double y, final double z, final Vec3 direction, final Level level) {
      super(type, x, y, z, direction, level);
   }

   public Fireball(final EntityType<? extends Fireball> type, final LivingEntity mob, final Vec3 direction, final Level level) {
      super(type, mob, direction, level);
   }

   public void setItem(final ItemStack source) {
      if (source.isEmpty()) {
         this.getEntityData().set(DATA_ITEM_STACK, this.getDefaultItem());
      } else {
         this.getEntityData().set(DATA_ITEM_STACK, source.copyWithCount(1));
      }
   }

   @Override
   protected void playEntityOnFireExtinguishedSound() {
   }

   @Override
   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM_STACK);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      entityData.define(DATA_ITEM_STACK, this.getDefaultItem());
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.store("Item", ItemStack.CODEC, this.getItem());
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setItem(input.<ItemStack>read("Item", ItemStack.CODEC).orElse(this.getDefaultItem()));
   }

   private ItemStack getDefaultItem() {
      return new ItemStack(Items.FIRE_CHARGE);
   }

   @Override
   public @Nullable SlotAccess getSlot(final int slot) {
      return slot == 0 ? SlotAccess.of(this::getItem, this::setItem) : super.getSlot(slot);
   }

   @Override
   public boolean shouldRenderAtSqrDistance(final double distance) {
      return this.tickCount < 2 && distance < 12.25 ? false : super.shouldRenderAtSqrDistance(distance);
   }
}
