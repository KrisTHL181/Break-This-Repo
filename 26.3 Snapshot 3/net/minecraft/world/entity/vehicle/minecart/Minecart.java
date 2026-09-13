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

   public Minecart(final EntityType<?> type, final Level level) {
      super(type, level);
   }

   @Override
   public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
      if (!player.isSecondaryUseActive() && !this.isVehicle() && (this.level().isClientSide() || player.startRiding(this))) {
         this.playerRotationOffset = this.rotationOffset;
         if (!this.level().isClientSide()) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
         } else {
            return InteractionResult.SUCCESS;
         }
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
   public void activateMinecart(final ServerLevel level, final int xt, final int yt, final int zt, final boolean state) {
      if (state) {
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
      double lastKnownYRot = this.getYRot();
      Vec3 lastKnownPos = this.position();
      super.tick();
      double tickDiff = (this.getYRot() - lastKnownYRot) % 360.0;
      if (this.level().isClientSide() && lastKnownPos.distanceTo(this.position()) > 0.01) {
         this.rotationOffset += (float)tickDiff;
         this.rotationOffset %= 360.0F;
      }
   }

   @Override
   protected void positionRider(final Entity passenger, final Entity.MoveFunction moveFunction) {
      super.positionRider(passenger, moveFunction);
      if (this.level().isClientSide() && passenger instanceof Player player && player.shouldRotateWithMinecart() && useExperimentalMovement(this.level())) {
         float yRot = (float)Mth.rotLerp(0.5, this.playerRotationOffset, this.rotationOffset);
         player.setYRot(player.getYRot() - (yRot - this.playerRotationOffset));
         this.playerRotationOffset = yRot;
      }
   }
}
