package net.minecraft.world.entity.monster.hoglin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public interface HoglinBase {
   int ATTACK_ANIMATION_DURATION = 10;
   float PROBABILITY_OF_SPAWNING_AS_BABY = 0.2F;

   int getAttackAnimationRemainingTicks();

   static boolean hurtAndThrowTarget(ServerLevel p_368083_, LivingEntity p_34643_, LivingEntity p_34644_) {
      float f1 = (float)p_34643_.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float f;
      if (!p_34643_.isBaby() && (int)f1 > 0) {
         f = f1 / 2.0F + p_368083_.random.nextInt((int)f1);
      } else {
         f = f1;
      }

      DamageSource damagesource = p_34643_.damageSources().mobAttack(p_34643_);
      boolean flag = p_34644_.hurtServer(p_368083_, damagesource, f);
      if (flag) {
         EnchantmentHelper.doPostAttackEffects(p_368083_, p_34644_, damagesource);
         if (!p_34643_.isBaby()) {
            throwTarget(p_34643_, p_34644_);
         }
      }

      return flag;
   }

   static void throwTarget(LivingEntity p_34646_, LivingEntity p_34647_) {
      double d0 = p_34646_.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
      double d1 = p_34647_.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      double d2 = d0 - d1;
      if (!(d2 <= 0.0)) {
         double d3 = p_34647_.getX() - p_34646_.getX();
         double d4 = p_34647_.getZ() - p_34646_.getZ();
         float f = p_34646_.level().random.nextInt(21) - 10;
         double d5 = d2 * (p_34646_.level().random.nextFloat() * 0.5F + 0.2F);
         Vec3 vec3 = new Vec3(d3, 0.0, d4).normalize().scale(d5).yRot(f);
         double d6 = d2 * p_34646_.level().random.nextFloat() * 0.5;
         p_34647_.push(vec3.x, d6, vec3.z);
         p_34647_.hurtMarked = true;
      }
   }
}
