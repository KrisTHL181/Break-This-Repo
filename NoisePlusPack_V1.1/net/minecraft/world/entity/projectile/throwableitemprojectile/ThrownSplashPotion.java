package net.minecraft.world.entity.projectile.throwableitemprojectile;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
   public ThrownSplashPotion(EntityType<? extends ThrownSplashPotion> p_452338_, Level p_459718_) {
      super(p_452338_, p_459718_);
   }

   public ThrownSplashPotion(Level p_451544_, LivingEntity p_451989_, ItemStack p_457954_) {
      super(EntityType.SPLASH_POTION, p_451544_, p_451989_, p_457954_);
   }

   public ThrownSplashPotion(Level p_450863_, double p_453658_, double p_457554_, double p_453246_, ItemStack p_452271_) {
      super(EntityType.SPLASH_POTION, p_450863_, p_453658_, p_457554_, p_453246_, p_452271_);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.SPLASH_POTION;
   }

   @Override
   public void onHitAsPotion(ServerLevel p_457820_, ItemStack p_452246_, HitResult p_453819_) {
      PotionContents potioncontents = p_452246_.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      float f = p_452246_.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F);
      Iterable<MobEffectInstance> iterable = potioncontents.getAllEffects();
      AABB aabb = this.getBoundingBox().move(p_453819_.getLocation().subtract(this.position()));
      AABB aabb1 = aabb.inflate(4.0, 2.0, 4.0);
      List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, aabb1);
      float f1 = ProjectileUtil.computeMargin(this);
      if (!list.isEmpty()) {
         Entity entity = this.getEffectSource();

         for (LivingEntity livingentity : list) {
            if (livingentity.isAffectedByPotions()) {
               double d0 = aabb.distanceToSqr(livingentity.getBoundingBox().inflate(f1));
               if (d0 < 16.0) {
                  double d1 = 1.0 - Math.sqrt(d0) / 4.0;

                  for (MobEffectInstance mobeffectinstance : iterable) {
                     Holder<MobEffect> holder = mobeffectinstance.getEffect();
                     if (holder.value().isInstantenous()) {
                        holder.value().applyInstantenousEffect(p_457820_, this, this.getOwner(), livingentity, mobeffectinstance.getAmplifier(), d1);
                     } else {
                        int i = mobeffectinstance.mapDuration(p_454985_ -> (int)(d1 * p_454985_ * f + 0.5));
                        MobEffectInstance mobeffectinstance1 = new MobEffectInstance(
                           holder, i, mobeffectinstance.getAmplifier(), mobeffectinstance.isAmbient(), mobeffectinstance.isVisible()
                        );
                        if (!mobeffectinstance1.endsWithin(20)) {
                           livingentity.addEffect(mobeffectinstance1, entity);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
