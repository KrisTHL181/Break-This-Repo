package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RidingMinecartSoundInstance extends RidingEntitySoundInstance {
   private final Player player;
   private final AbstractMinecart minecart;
   private final boolean underwaterSound;

   public RidingMinecartSoundInstance(
      Player p_174940_, AbstractMinecart p_459402_, boolean p_174942_, SoundEvent p_452359_, float p_452785_, float p_453561_, float p_460435_
   ) {
      super(p_174940_, p_459402_, p_174942_, p_452359_, SoundSource.NEUTRAL, p_452785_, p_453561_, p_460435_);
      this.player = p_174940_;
      this.minecart = p_459402_;
      this.underwaterSound = p_174942_;
   }

   @Override
   protected boolean shouldNotPlayUnderwaterSound() {
      return this.underwaterSound != this.player.isUnderWater();
   }

   @Override
   protected float getEntitySpeed() {
      return (float)this.minecart.getDeltaMovement().horizontalDistance();
   }

   @Override
   protected boolean shoudlPlaySound() {
      return this.minecart.isOnRails() || !(this.minecart.getBehavior() instanceof NewMinecartBehavior);
   }
}
