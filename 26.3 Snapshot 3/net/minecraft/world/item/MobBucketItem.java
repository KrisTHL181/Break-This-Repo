package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MobBucketItem extends BucketItem {
   private final EntityType<? extends Mob> type;
   private final SoundEvent emptySound;

   public MobBucketItem(final EntityType<? extends Mob> type, final Fluid content, final SoundEvent emptySound, final Item.Properties properties) {
      super(content, properties);
      this.type = type;
      this.emptySound = emptySound;
   }

   @Override
   public void checkExtraContent(final @Nullable LivingEntity user, final Level level, final ItemStack itemStack, final BlockPos pos) {
      if (level instanceof ServerLevel serverLevel) {
         this.spawn(serverLevel, itemStack, pos);
         level.gameEvent(user, GameEvent.ENTITY_PLACE, pos);
      }
   }

   @Override
   protected void playEmptySound(final @Nullable LivingEntity user, final LevelAccessor level, final BlockPos pos) {
      level.playSound(user, pos, this.emptySound, SoundSource.NEUTRAL, 1.0F, 1.0F);
   }

   private void spawn(final ServerLevel level, final ItemStack itemStack, final BlockPos spawnPos) {
      Mob mob = this.type.create(level, EntityType.createDefaultStackConfig(level, itemStack, null), spawnPos, EntitySpawnReason.BUCKET, true, false);
      if (mob instanceof Bucketable bucketable) {
         CustomData entityData = itemStack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY);
         bucketable.loadFromBucketTag(entityData.copyTag());
         bucketable.setFromBucket(true);
      }

      if (mob != null) {
         level.addFreshEntityWithPassengers(mob);
         mob.playAmbientSound();
      }
   }

   @Override
   public boolean emptyContents(final @Nullable LivingEntity user, final Level level, final BlockPos pos, final @Nullable BlockHitResult hitResult) {
      if (this.content == Fluids.EMPTY) {
         this.playEmptySound(user, level, pos);
         return true;
      } else {
         return super.emptyContents(user, level, pos, hitResult);
      }
   }

   @Override
   public ClipContext.Fluid getFluidContext() {
      return ClipContext.Fluid.NONE;
   }
}
