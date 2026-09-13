package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MinecartSoundInstance extends AbstractTickableSoundInstance {
   private static final float VOLUME_MIN = 0.0F;
   private static final float VOLUME_MAX = 0.7F;
   private static final float PITCH_MIN = 0.0F;
   private static final float PITCH_MAX = 1.0F;
   private static final float PITCH_DELTA = 0.0025F;
   private final AbstractMinecart minecart;
   private float pitch = 0.0F;

   public MinecartSoundInstance(AbstractMinecart p_456614_) {
      super(SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
      this.minecart = p_456614_;
      this.looping = true;
      this.delay = 0;
      this.volume = 0.0F;
      this.x = (float)p_456614_.getX();
      this.y = (float)p_456614_.getY();
      this.z = (float)p_456614_.getZ();
   }

   @Override
   public boolean canPlaySound() {
      return !this.minecart.isSilent();
   }

   @Override
   public boolean canStartSilent() {
      return true;
   }

   @Override
   public void tick() {
      if (this.minecart.isRemoved()) {
         this.stop();
      } else {
         this.x = (float)this.minecart.getX();
         this.y = (float)this.minecart.getY();
         this.z = (float)this.minecart.getZ();
         float f = (float)this.minecart.getDeltaMovement().horizontalDistance();
         boolean flag = !this.minecart.isOnRails() && this.minecart.getBehavior() instanceof NewMinecartBehavior;
         if (f >= 0.01F && this.minecart.level().tickRateManager().runsNormally() && !flag) {
            this.pitch = Mth.clamp(this.pitch + 0.0025F, 0.0F, 1.0F);
            this.volume = Mth.lerp(Mth.clamp(f, 0.0F, 0.5F), 0.0F, 0.7F);
         } else {
            this.pitch = 0.0F;
            this.volume = 0.0F;
         }
      }
   }
}
