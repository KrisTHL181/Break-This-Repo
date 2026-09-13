package net.minecraft.world.food;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FoodData {
   private static final int DEFAULT_TICK_TIMER = 0;
   private static final float DEFAULT_EXHAUSTION_LEVEL = 0.0F;
   private int foodLevel = 20;
   private float saturationLevel = 5.0F;
   private float exhaustionLevel;
   private int tickTimer;

   private void add(final int food, final float saturation) {
      this.foodLevel = Mth.clamp(food + this.foodLevel, 0, 20);
      this.saturationLevel = Mth.clamp(saturation + this.saturationLevel, 0.0F, this.foodLevel);
   }

   public void eat(final int food, final float saturationModifier) {
      this.add(food, FoodConstants.saturationByModifier(food, saturationModifier));
   }

   public void eat(final FoodProperties foodProperties) {
      this.add(foodProperties.nutrition(), foodProperties.saturation());
   }

   public void tick(final ServerPlayer player) {
      ServerLevel level = player.level();
      Difficulty difficulty = level.getDifficulty();
      if (this.exhaustionLevel > 4.0F) {
         this.exhaustionLevel -= 4.0F;
         if (this.saturationLevel > 0.0F) {
            this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
         } else if (difficulty != Difficulty.PEACEFUL) {
            this.foodLevel = Math.max(this.foodLevel - 1, 0);
         }
      }

      boolean naturalRegen = level.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);
      if (naturalRegen && this.saturationLevel > 0.0F && player.isHurt() && this.foodLevel >= 20) {
         this.tickTimer++;
         if (this.tickTimer >= 10) {
            float saturationSpent = Math.min(this.saturationLevel, 6.0F);
            player.heal(saturationSpent / 6.0F);
            this.addExhaustion(saturationSpent);
            this.tickTimer = 0;
         }
      } else if (naturalRegen && this.foodLevel >= 18 && player.isHurt()) {
         this.tickTimer++;
         if (this.tickTimer >= 80) {
            player.heal(1.0F);
            this.addExhaustion(6.0F);
            this.tickTimer = 0;
         }
      } else if (this.foodLevel <= 0) {
         this.tickTimer++;
         if (this.tickTimer >= 80) {
            if (player.getHealth() > 10.0F || difficulty == Difficulty.HARD || player.getHealth() > 1.0F && difficulty == Difficulty.NORMAL) {
               player.hurtServer(level, player.damageSources().starve(), 1.0F);
            }

            this.tickTimer = 0;
         }
      } else {
         this.tickTimer = 0;
      }
   }

   public void readAdditionalSaveData(final ValueInput input) {
      this.foodLevel = input.getIntOr("foodLevel", 20);
      this.tickTimer = input.getIntOr("foodTickTimer", 0);
      this.saturationLevel = input.getFloatOr("foodSaturationLevel", 5.0F);
      this.exhaustionLevel = input.getFloatOr("foodExhaustionLevel", 0.0F);
   }

   public void addAdditionalSaveData(final ValueOutput output) {
      output.putInt("foodLevel", this.foodLevel);
      output.putInt("foodTickTimer", this.tickTimer);
      output.putFloat("foodSaturationLevel", this.saturationLevel);
      output.putFloat("foodExhaustionLevel", this.exhaustionLevel);
   }

   public int getFoodLevel() {
      return this.foodLevel;
   }

   public boolean hasEnoughFood() {
      return this.getFoodLevel() > 6.0F;
   }

   public boolean needsFood() {
      return this.foodLevel < 20;
   }

   public void addExhaustion(final float amount) {
      this.exhaustionLevel = Math.min(this.exhaustionLevel + amount, 40.0F);
   }

   public float getSaturationLevel() {
      return this.saturationLevel;
   }

   public void setFoodLevel(final int food) {
      this.foodLevel = food;
   }

   public void setSaturation(final float saturation) {
      this.saturationLevel = saturation;
   }
}
