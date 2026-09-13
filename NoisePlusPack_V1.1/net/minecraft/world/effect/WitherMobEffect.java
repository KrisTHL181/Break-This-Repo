package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public class WitherMobEffect extends MobEffect {
   public static final int DAMAGE_INTERVAL = 40;

   protected WitherMobEffect(MobEffectCategory p_300352_, int p_298007_) {
      super(p_300352_, p_298007_);
   }

   @Override
   public boolean applyEffectTick(ServerLevel p_365526_, LivingEntity p_299783_, int p_298645_) {
      p_299783_.hurtServer(p_365526_, p_299783_.damageSources().wither(), 1.0F);
      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_299625_, int p_297396_) {
      int i = 40 >> p_297396_;
      return i > 0 ? p_299625_ % i == 0 : true;
   }
}
