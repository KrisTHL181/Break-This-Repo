package net.minecraft.client.renderer;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EndFlashState {
   public static final int SOUND_DELAY_IN_TICKS = 30;
   private static final int FLASH_INTERVAL_IN_TICKS = 600;
   private static final int MAX_FLASH_OFFSET_IN_TICKS = 200;
   private static final int MIN_FLASH_DURATION_IN_TICKS = 100;
   private static final int MAX_FLASH_DURATION_IN_TICKS = 380;
   private long flashSeed;
   private int offset;
   private int duration;
   private float intensity;
   private float oldIntensity;
   private float xAngle;
   private float yAngle;

   public void tick(long p_423891_) {
      this.calculateFlashParameters(p_423891_);
      this.oldIntensity = this.intensity;
      this.intensity = this.calculateIntensity(p_423891_);
   }

   private void calculateFlashParameters(long p_425983_) {
      long i = p_425983_ / 600L;
      if (i != this.flashSeed) {
         RandomSource randomsource = RandomSource.create(i);
         randomsource.nextFloat();
         this.offset = Mth.randomBetweenInclusive(randomsource, 0, 200);
         this.duration = Mth.randomBetweenInclusive(randomsource, 100, Math.min(380, 600 - this.offset));
         this.xAngle = Mth.randomBetween(randomsource, -60.0F, 10.0F);
         this.yAngle = Mth.randomBetween(randomsource, -180.0F, 180.0F);
         this.flashSeed = i;
      }
   }

   private float calculateIntensity(long p_427833_) {
      long i = p_427833_ % 600L;
      return i >= this.offset && i <= this.offset + this.duration ? Mth.sin((float)(i - this.offset) * (float) Math.PI / this.duration) : 0.0F;
   }

   public float getXAngle() {
      return this.xAngle;
   }

   public float getYAngle() {
      return this.yAngle;
   }

   public float getIntensity(float p_428577_) {
      return Mth.lerp(p_428577_, this.oldIntensity, this.intensity);
   }

   public boolean flashStartedThisTick() {
      return this.intensity > 0.0F && this.oldIntensity <= 0.0F;
   }
}
