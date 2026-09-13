package net.minecraft.world.effect;

public class InstantaneousMobEffect extends MobEffect {
   public InstantaneousMobEffect(final MobEffectCategory category, final int color) {
      super(category, color);
   }

   @Override
   public boolean isInstantaneous() {
      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(final int remainingDuration, final int amplification) {
      return remainingDuration >= 1;
   }
}
