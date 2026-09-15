package net.minecraft.world.effect;

public class InstantenousMobEffect extends MobEffect {
   public InstantenousMobEffect(MobEffectCategory p_19440_, int p_19441_) {
      super(p_19440_, p_19441_);
   }

   @Override
   public boolean isInstantenous() {
      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_19444_, int p_19445_) {
      return p_19444_ >= 1;
   }
}
