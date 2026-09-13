package net.minecraft.world.entity;

import net.minecraft.world.item.ItemStack;

public class Crackiness {
   public static final Crackiness GOLEM = new Crackiness(0.75F, 0.5F, 0.25F);
   public static final Crackiness WOLF_ARMOR = new Crackiness(0.95F, 0.69F, 0.32F);
   private final float fractionLow;
   private final float fractionMedium;
   private final float fractionHigh;

   private Crackiness(final float fractionLow, final float fractionMedium, final float fractionHigh) {
      this.fractionLow = fractionLow;
      this.fractionMedium = fractionMedium;
      this.fractionHigh = fractionHigh;
   }

   public Crackiness.Level byFraction(final float fraction) {
      if (fraction < this.fractionHigh) {
         return Crackiness.Level.HIGH;
      } else if (fraction < this.fractionMedium) {
         return Crackiness.Level.MEDIUM;
      } else {
         return fraction < this.fractionLow ? Crackiness.Level.LOW : Crackiness.Level.NONE;
      }
   }

   public Crackiness.Level byDamage(final ItemStack item) {
      return !item.isDamageableItem() ? Crackiness.Level.NONE : this.byDamage(item.getDamageValue(), item.getMaxDamage());
   }

   public Crackiness.Level byDamage(final int damage, final int maxDamage) {
      return this.byFraction((float)(maxDamage - damage) / maxDamage);
   }

   public enum Level {
      NONE,
      LOW,
      MEDIUM,
      HIGH;
   }
}
