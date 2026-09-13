package net.minecraft.world.entity.monster.hoglin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public interface HoglinBase {
   int ATTACK_ANIMATION_DURATION = 10;
   float PROBABILITY_OF_SPAWNING_AS_BABY = 0.2F;

   int getAttackAnimationRemainingTicks();

   static boolean hurtAndThrowTarget(final ServerLevel level, final LivingEntity body, final LivingEntity target) {
      float attackDamage = (float)body.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float actualDamage;
      if (!body.isBaby() && (int)attackDamage > 0) {
         actualDamage = attackDamage / 2.0F + level.getRandom().nextInt((int)attackDamage);
      } else {
         actualDamage = attackDamage;
      }

      DamageSource damageSource = body.damageSources().mobAttack(body);
      boolean wasHurt = target.hurtServer(level, damageSource, actualDamage);
      if (wasHurt) {
         EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
         if (!body.isBaby()) {
            throwTarget(body, target);
         }
      }

      return wasHurt;
   }

   static void throwTarget(final LivingEntity body, final LivingEntity target) {
      double knockbackPower = body.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
      double knockbackResistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      double effectiveKnockbackPower = knockbackPower - knockbackResistance;
      if (!(effectiveKnockbackPower <= 0.0)) {
         double xd = target.getX() - body.getX();
         double zd = target.getZ() - body.getZ();
         RandomSource random = body.level().getRandom();
         float horizontalPushAngle = random.nextInt(21) - 10;
         double horizontalScale = effectiveKnockbackPower * (random.nextFloat() * 0.5F + 0.2F);
         Vec3 horizontalPushVector = new Vec3(xd, 0.0, zd).normalize().scale(horizontalScale).yRot(horizontalPushAngle);
         double verticalScale = effectiveKnockbackPower * random.nextFloat() * 0.5;
         target.push(horizontalPushVector.x, verticalScale, horizontalPushVector.z);
         target.syncVelocity = true;
      }
   }
}
