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

   public Fireball(EntityType<? extends Fireball> p_459926_, Level p_458264_) {
      super(p_459926_, p_458264_);
   }

   public Fireball(EntityType<? extends Fireball> p_457068_, double p_451606_, double p_454465_, double p_454830_, Vec3 p_459467_, Level p_450716_) {
      super(p_457068_, p_451606_, p_454465_, p_454830_, p_459467_, p_450716_);
   }

   public Fireball(EntityType<? extends Fireball> p_455647_, LivingEntity p_458182_, Vec3 p_450754_, Level p_459300_) {
      super(p_455647_, p_458182_, p_450754_, p_459300_);
   }

   public void setItem(ItemStack p_452715_) {
      if (p_452715_.isEmpty()) {
         this.getEntityData().set(DATA_ITEM_STACK, this.getDefaultItem());
      } else {
         this.getEntityData().set(DATA_ITEM_STACK, p_452715_.copyWithCount(1));
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
   protected void defineSynchedData(SynchedEntityData.Builder p_454998_) {
      p_454998_.define(DATA_ITEM_STACK, this.getDefaultItem());
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_453648_) {
      super.addAdditionalSaveData(p_453648_);
      p_453648_.store("Item", ItemStack.CODEC, this.getItem());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_453558_) {
      super.readAdditionalSaveData(p_453558_);
      this.setItem(p_453558_.<ItemStack>read("Item", ItemStack.CODEC).orElse(this.getDefaultItem()));
   }

   private ItemStack getDefaultItem() {
      return new ItemStack(Items.FIRE_CHARGE);
   }

   @Override
   public @Nullable SlotAccess getSlot(int p_453667_) {
      return p_453667_ == 0 ? SlotAccess.of(this::getItem, this::setItem) : super.getSlot(p_453667_);
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double p_450963_) {
      return this.tickCount < 2 && p_450963_ < 12.25 ? false : super.shouldRenderAtSqrDistance(p_450963_);
   }
}
