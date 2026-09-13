package net.minecraft.world.entity.monster.spider;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CaveSpider extends Spider {
   public CaveSpider(EntityType<? extends CaveSpider> p_451551_, Level p_458214_) {
      super(p_451551_, p_458214_);
   }

   public static AttributeSupplier.Builder createCaveSpider() {
      return Spider.createAttributes().add(Attributes.MAX_HEALTH, 12.0);
   }

   @Override
   public boolean doHurtTarget(ServerLevel p_455476_, Entity p_451211_) {
      if (super.doHurtTarget(p_455476_, p_451211_)) {
         if (p_451211_ instanceof LivingEntity) {
            int i = 0;
            if (this.level().getDifficulty() == Difficulty.NORMAL) {
               i = 7;
            } else if (this.level().getDifficulty() == Difficulty.HARD) {
               i = 15;
            }

            if (i > 0) {
               ((LivingEntity)p_451211_).addEffect(new MobEffectInstance(MobEffects.POISON, i * 20, 0), this);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_450688_, DifficultyInstance p_457285_, EntitySpawnReason p_459808_, @Nullable SpawnGroupData p_454546_
   ) {
      return p_454546_;
   }

   @Override
   public Vec3 getVehicleAttachmentPoint(Entity p_457535_) {
      return p_457535_.getBbWidth() <= this.getBbWidth() ? new Vec3(0.0, 0.21875 * this.getScale(), 0.0) : super.getVehicleAttachmentPoint(p_457535_);
   }
}
