package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Minecart extends AbstractMinecart {
   private float rotationOffset;
   private float playerRotationOffset;

   public Minecart(EntityType<?> p_451702_, Level p_450217_) {
      super(p_451702_, p_450217_);
   }

   @Override
   public InteractionResult interact(Player p_456891_, InteractionHand p_453487_) {
      if (!p_456891_.isSecondaryUseActive() && !this.isVehicle() && (this.level().isClientSide() || p_456891_.startRiding(this))) {
         this.playerRotationOffset = this.rotationOffset;
         return !this.level().isClientSide()
            ? (InteractionResult)(p_456891_.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS)
            : InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected Item getDropItem() {
      return Items.MINECART;
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.MINECART);
   }

   @Override
   public void activateMinecart(ServerLevel p_459247_, int p_459846_, int p_458535_, int p_451691_, boolean p_454007_) {
      if (p_454007_) {
         if (this.isVehicle()) {
            this.ejectPassengers();
         }

         if (this.getHurtTime() == 0) {
            this.setHurtDir(-this.getHurtDir());
            this.setHurtTime(10);
            this.setDamage(50.0F);
            this.markHurt();
         }
      }
   }

   @Override
   public boolean isRideable() {
      return true;
   }

   @Override
   public void tick() {
      double d0 = this.getYRot();
      Vec3 vec3 = this.position();
      super.tick();
      double d1 = (this.getYRot() - d0) % 360.0;
      if (this.level().isClientSide() && vec3.distanceTo(this.position()) > 0.01) {
         this.rotationOffset += (float)d1;
         this.rotationOffset %= 360.0F;
      }
   }

   @Override
   protected void positionRider(Entity p_459741_, Entity.MoveFunction p_451854_) {
      super.positionRider(p_459741_, p_451854_);
      if (this.level().isClientSide() && p_459741_ instanceof Player player && player.shouldRotateWithMinecart() && useExperimentalMovement(this.level())) {
         float f = (float)Mth.rotLerp(0.5, this.playerRotationOffset, this.rotationOffset);
         player.setYRot(player.getYRot() - (f - this.playerRotationOffset));
         this.playerRotationOffset = f;
      }
   }
}
