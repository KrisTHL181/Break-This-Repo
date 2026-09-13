package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class MobBucketItem extends BucketItem {
   private final EntityType<? extends Mob> type;
   private final SoundEvent emptySound;

   public MobBucketItem(EntityType<? extends Mob> p_151137_, Fluid p_151138_, SoundEvent p_151139_, Item.Properties p_151140_) {
      super(p_151138_, p_151140_);
      this.type = p_151137_;
      this.emptySound = p_151139_;
   }

   @Override
   public void checkExtraContent(@Nullable LivingEntity p_391293_, Level p_151147_, ItemStack p_151148_, BlockPos p_151149_) {
      if (p_151147_ instanceof ServerLevel) {
         this.spawn((ServerLevel)p_151147_, p_151148_, p_151149_);
         p_151147_.gameEvent(p_391293_, GameEvent.ENTITY_PLACE, p_151149_);
      }
   }

   @Override
   protected void playEmptySound(@Nullable LivingEntity p_391719_, LevelAccessor p_151152_, BlockPos p_151153_) {
      p_151152_.playSound(p_391719_, p_151153_, this.emptySound, SoundSource.NEUTRAL, 1.0F, 1.0F);
   }

   private void spawn(ServerLevel p_151142_, ItemStack p_151143_, BlockPos p_151144_) {
      Mob mob = this.type.create(p_151142_, EntityType.createDefaultStackConfig(p_151142_, p_151143_, null), p_151144_, EntitySpawnReason.BUCKET, true, false);
      if (mob instanceof Bucketable bucketable) {
         CustomData customdata = p_151143_.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY);
         bucketable.loadFromBucketTag(customdata.copyTag());
         bucketable.setFromBucket(true);
      }

      if (mob != null) {
         p_151142_.addFreshEntityWithPassengers(mob);
         mob.playAmbientSound();
      }
   }
}
