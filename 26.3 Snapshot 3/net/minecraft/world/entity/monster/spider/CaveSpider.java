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
   public CaveSpider(final EntityType<? extends CaveSpider> type, final Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createCaveSpider() {
      return Spider.createAttributes().add(Attributes.MAX_HEALTH, 12.0);
   }

   @Override
   public boolean doHurtTarget(final ServerLevel level, final Entity target) {
      if (super.doHurtTarget(level, target)) {
         if (target instanceof LivingEntity livingEntity) {
            int poisonTime = 0;
            if (this.level().getDifficulty() == Difficulty.NORMAL) {
               poisonTime = 7;
            } else if (this.level().getDifficulty() == Difficulty.HARD) {
               poisonTime = 15;
            }

            if (poisonTime > 0) {
               livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, poisonTime * 20, 0), this);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      return groupData;
   }

   @Override
   public Vec3 getVehicleAttachmentPoint(final Entity vehicle) {
      return vehicle.getBbWidth() <= this.getBbWidth() ? new Vec3(0.0, 0.21875 * this.getScale(), 0.0) : super.getVehicleAttachmentPoint(vehicle);
   }
}
