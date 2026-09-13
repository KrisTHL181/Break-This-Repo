package net.minecraft.client.resources.sounds;

import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class UnderLiquidAmbientSoundInstance extends AbstractTickableSoundInstance {
   public static final int FADE_DURATION = 40;
   private final LocalPlayer player;
   private final Predicate<LocalPlayer> isUnderLiquid;
   private int fade;

   public UnderLiquidAmbientSoundInstance(final SoundEvent event, final LocalPlayer player, final Predicate<LocalPlayer> isUnderLiquid) {
      super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
      this.player = player;
      this.isUnderLiquid = isUnderLiquid;
      this.looping = true;
      this.delay = 0;
      this.volume = 1.0F;
      this.relative = true;
   }

   public static UnderLiquidAmbientSoundInstance underwater(final LocalPlayer player) {
      return new UnderLiquidAmbientSoundInstance(SoundEvents.AMBIENT_UNDERWATER_LOOP, player, LocalPlayer::isUnderWater);
   }

   @Override
   public void tick() {
      if (!this.player.isRemoved() && this.fade >= 0) {
         if (this.isUnderLiquid.test(this.player)) {
            this.fade++;
         } else {
            this.fade -= 2;
         }

         this.fade = Math.min(this.fade, 40);
         this.volume = Math.max(0.0F, Math.min(this.fade / 40.0F, 1.0F));
      } else {
         this.stop();
      }
   }
}
