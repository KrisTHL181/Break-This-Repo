package net.minecraft.world;

import javax.annotation.concurrent.Immutable;
import net.minecraft.util.Mth;

@Immutable
public class DifficultyInstance {
   private static final float DIFFICULTY_TIME_GLOBAL_OFFSET = -72000.0F;
   private static final float MAX_DIFFICULTY_TIME_GLOBAL = 1440000.0F;
   private static final float MAX_DIFFICULTY_TIME_LOCAL = 3600000.0F;
   private final Difficulty base;
   private final float effectiveDifficulty;

   public DifficultyInstance(final Difficulty base, final long totalGameTime, final long localGameTime, final float moonBrightness) {
      this.base = base;
      this.effectiveDifficulty = this.calculateDifficulty(base, totalGameTime, localGameTime, moonBrightness);
   }

   public Difficulty getDifficulty() {
      return this.base;
   }

   public float getEffectiveDifficulty() {
      return this.effectiveDifficulty;
   }

   public boolean isHard() {
      return this.effectiveDifficulty >= Difficulty.HARD.ordinal();
   }

   public boolean isHarderThan(final float requiredDifficulty) {
      return this.effectiveDifficulty > requiredDifficulty;
   }

   public float getSpecialMultiplier() {
      if (this.effectiveDifficulty < 2.0F) {
         return 0.0F;
      } else {
         return this.effectiveDifficulty > 4.0F ? 1.0F : (this.effectiveDifficulty - 2.0F) / 2.0F;
      }
   }

   private float calculateDifficulty(final Difficulty base, final long totalGameTime, final long localGameTime, final float moonBrightness) {
      if (base == Difficulty.PEACEFUL) {
         return 0.0F;
      }

      boolean isHard = base == Difficulty.HARD;
      float scale = 0.75F;
      float globalScale = Mth.clamp(((float)totalGameTime + -72000.0F) / 1440000.0F, 0.0F, 1.0F) * 0.25F;
      scale += globalScale;
      float localScale = 0.0F;
      localScale += Mth.clamp((float)localGameTime / 3600000.0F, 0.0F, 1.0F) * (isHard ? 1.0F : 0.75F);
      localScale += Mth.clamp(moonBrightness * 0.25F, 0.0F, globalScale);
      if (base == Difficulty.EASY) {
         localScale *= 0.5F;
      }

      scale += localScale;
      return base.getId() * scale;
   }
}
