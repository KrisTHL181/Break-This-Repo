package net.minecraft.world.entity.projectile.throwableitemprojectile;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

public class ThrownSplashPotion extends AbstractThrownPotion {
   public ThrownSplashPotion(final EntityType<? extends ThrownSplashPotion> type, final Level level) {
      super(type, level);
   }

   public ThrownSplashPotion(final Level level, final LivingEntity owner, final ItemStack itemStack) {
      super(EntityTypes.SPLASH_POTION, level, owner, itemStack);
   }

   public ThrownSplashPotion(final Level level, final double x, final double y, final double z, final ItemStack itemStack) {
      super(EntityTypes.SPLASH_POTION, level, x, y, z, itemStack);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.SPLASH_POTION;
   }

   @Override
   public void onHitAsPotion(final ServerLevel level, final ItemStack potionItem, final HitResult hitResult) {
      PotionContents contents = potionItem.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      float durationScale = potionItem.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F);
      Iterable<MobEffectInstance> mobEffects = contents.getAllEffects();
      AABB potionAabb = this.getBoundingBox().move(hitResult.getLocation().subtract(this.position()));
      AABB effectAabb = potionAabb.inflate(4.0, 2.0, 4.0);
      List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, effectAabb);
      float margin = ProjectileUtil.computeMargin(this);
      if (!entities.isEmpty()) {
         Entity effectSource = this.getEffectSource();

         for (LivingEntity entity : entities) {
            if (entity.isAffectedByPotions()) {
               double dist = potionAabb.distanceToSqr(entity.getBoundingBox().inflate(margin));
               if (dist < 16.0) {
                  double scale = 1.0 - Math.sqrt(dist) / 4.0;

                  for (MobEffectInstance effectInstance : mobEffects) {
                     Holder<MobEffect> effect = effectInstance.getEffect();
                     if (effect.value().isInstantaneous()) {
                        effect.value().applyInstantaneousEffect(level, this, this.getOwner(), entity, effectInstance.getAmplifier(), scale);
                     } else {
                        int duration = effectInstance.mapDuration(d -> (int)(scale * d * durationScale + 0.5));
                        MobEffectInstance newEffect = new MobEffectInstance(
                           effect, duration, effectInstance.getAmplifier(), effectInstance.isAmbient(), effectInstance.isVisible()
                        );
                        if (!newEffect.endsWithin(20)) {
                           entity.addEffect(newEffect, effectSource);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
