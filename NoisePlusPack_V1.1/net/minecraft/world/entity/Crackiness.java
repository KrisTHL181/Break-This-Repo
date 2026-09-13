package net.minecraft.world.entity;

import net.minecraft.world.item.ItemStack;

public class Crackiness {
   public static final Crackiness GOLEM = new Crackiness(0.75F, 0.5F, 0.25F);
   public static final Crackiness WOLF_ARMOR = new Crackiness(0.95F, 0.69F, 0.32F);
   private final float fractionLow;
   private final float fractionMedium;
   private final float fractionHigh;

   private Crackiness(float p_332482_, float p_329781_, float p_335121_) {
      this.fractionLow = p_332482_;
      this.fractionMedium = p_329781_;
      this.fractionHigh = p_335121_;
   }

   public Crackiness.Level byFraction(float p_330247_) {
      if (p_330247_ < this.fractionHigh) {
         return Crackiness.Level.HIGH;
      } else if (p_330247_ < this.fractionMedium) {
         return Crackiness.Level.MEDIUM;
      } else {
         return p_330247_ < this.fractionLow ? Crackiness.Level.LOW : Crackiness.Level.NONE;
      }
   }

   public Crackiness.Level byDamage(ItemStack p_328846_) {
      return !p_328846_.isDamageableItem() ? Crackiness.Level.NONE : this.byDamage(p_328846_.getDamageValue(), p_328846_.getMaxDamage());
   }

   public Crackiness.Level byDamage(int p_329022_, int p_332255_) {
      return this.byFraction((float)(p_332255_ - p_329022_) / p_332255_);
   }

   public enum Level {
      NONE,
      LOW,
      MEDIUM,
      HIGH;
   }
}
