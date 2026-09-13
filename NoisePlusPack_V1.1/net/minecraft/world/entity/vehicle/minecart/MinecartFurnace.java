package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class MinecartFurnace extends AbstractMinecart {
   private static final EntityDataAccessor<Boolean> DATA_ID_FUEL = SynchedEntityData.defineId(MinecartFurnace.class, EntityDataSerializers.BOOLEAN);
   private static final int FUEL_TICKS_PER_ITEM = 3600;
   private static final int MAX_FUEL_TICKS = 32000;
   private static final short DEFAULT_FUEL = 0;
   private static final Vec3 DEFAULT_PUSH = Vec3.ZERO;
   private int fuel = 0;
   public Vec3 push = DEFAULT_PUSH;

   public MinecartFurnace(EntityType<? extends MinecartFurnace> p_450355_, Level p_452205_) {
      super(p_450355_, p_452205_);
   }

   @Override
   public boolean isFurnace() {
      return true;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_453306_) {
      super.defineSynchedData(p_453306_);
      p_453306_.define(DATA_ID_FUEL, false);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (this.fuel > 0) {
            this.fuel--;
         }

         if (this.fuel <= 0) {
            this.push = Vec3.ZERO;
         }

         this.setHasFuel(this.fuel > 0);
      }

      if (this.hasFuel() && this.random.nextInt(4) == 0) {
         this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.8, this.getZ(), 0.0, 0.0, 0.0);
      }
   }

   @Override
   protected double getMaxSpeed(ServerLevel p_456144_) {
      return this.isInWater() ? super.getMaxSpeed(p_456144_) * 0.75 : super.getMaxSpeed(p_456144_) * 0.5;
   }

   @Override
   protected Item getDropItem() {
      return Items.FURNACE_MINECART;
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.FURNACE_MINECART);
   }

   @Override
   protected Vec3 applyNaturalSlowdown(Vec3 p_451353_) {
      Vec3 vec3;
      if (this.push.lengthSqr() > 1.0E-7) {
         this.push = this.calculateNewPushAlong(p_451353_);
         vec3 = p_451353_.multiply(0.8, 0.0, 0.8).add(this.push);
         if (this.isInWater()) {
            vec3 = vec3.scale(0.1);
         }
      } else {
         vec3 = p_451353_.multiply(0.98, 0.0, 0.98);
      }

      return super.applyNaturalSlowdown(vec3);
   }

   private Vec3 calculateNewPushAlong(Vec3 p_455296_) {
      double d0 = 1.0E-4;
      double d1 = 0.001;
      return this.push.horizontalDistanceSqr() > 1.0E-4 && p_455296_.horizontalDistanceSqr() > 0.001
         ? this.push.projectedOn(p_455296_).normalize().scale(this.push.length())
         : this.push;
   }

   @Override
   public InteractionResult interact(Player p_460532_, InteractionHand p_456760_) {
      ItemStack itemstack = p_460532_.getItemInHand(p_456760_);
      if (this.addFuel(p_460532_.position(), itemstack)) {
         itemstack.consume(1, p_460532_);
      }

      return InteractionResult.SUCCESS;
   }

   public boolean addFuel(Vec3 p_455745_, ItemStack p_457302_) {
      if (p_457302_.is(ItemTags.FURNACE_MINECART_FUEL) && this.fuel + 3600 <= 32000) {
         this.fuel += 3600;
         if (this.fuel > 0) {
            this.push = this.position().subtract(p_455745_).horizontal();
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_456295_) {
      super.addAdditionalSaveData(p_456295_);
      p_456295_.putDouble("PushX", this.push.x);
      p_456295_.putDouble("PushZ", this.push.z);
      p_456295_.putShort("Fuel", (short)this.fuel);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_458192_) {
      super.readAdditionalSaveData(p_458192_);
      double d0 = p_458192_.getDoubleOr("PushX", DEFAULT_PUSH.x);
      double d1 = p_458192_.getDoubleOr("PushZ", DEFAULT_PUSH.z);
      this.push = new Vec3(d0, 0.0, d1);
      this.fuel = p_458192_.getShortOr("Fuel", (short)0);
   }

   protected boolean hasFuel() {
      return this.entityData.get(DATA_ID_FUEL);
   }

   protected void setHasFuel(boolean p_459059_) {
      this.entityData.set(DATA_ID_FUEL, p_459059_);
   }

   @Override
   public BlockState getDefaultDisplayBlockState() {
      return Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH).setValue(FurnaceBlock.LIT, this.hasFuel());
   }
}
