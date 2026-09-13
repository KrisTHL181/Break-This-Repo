package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.Optionull;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkCatalystBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class SculkCatalystBlockEntity extends BlockEntity implements GameEventListener.Provider<SculkCatalystBlockEntity.CatalystListener> {
   private final SculkCatalystBlockEntity.CatalystListener catalystListener;

   public SculkCatalystBlockEntity(BlockPos p_222774_, BlockState p_222775_) {
      super(BlockEntityType.SCULK_CATALYST, p_222774_, p_222775_);
      this.catalystListener = new SculkCatalystBlockEntity.CatalystListener(p_222775_, new BlockPositionSource(p_222774_));
   }

   public static void serverTick(Level p_222780_, BlockPos p_222781_, BlockState p_222782_, SculkCatalystBlockEntity p_222783_) {
      p_222783_.catalystListener.getSculkSpreader().updateCursors(p_222780_, p_222781_, p_222780_.getRandom(), true);
   }

   @Override
   protected void loadAdditional(ValueInput p_409394_) {
      super.loadAdditional(p_409394_);
      this.catalystListener.sculkSpreader.load(p_409394_);
   }

   @Override
   protected void saveAdditional(ValueOutput p_409243_) {
      this.catalystListener.sculkSpreader.save(p_409243_);
      super.saveAdditional(p_409243_);
   }

   public SculkCatalystBlockEntity.CatalystListener getListener() {
      return this.catalystListener;
   }

   public static class CatalystListener implements GameEventListener {
      public static final int PULSE_TICKS = 8;
      final SculkSpreader sculkSpreader;
      private final BlockState blockState;
      private final PositionSource positionSource;

      public CatalystListener(BlockState p_283224_, PositionSource p_283095_) {
         this.blockState = p_283224_;
         this.positionSource = p_283095_;
         this.sculkSpreader = SculkSpreader.createLevelSpreader();
      }

      @Override
      public PositionSource getListenerSource() {
         return this.positionSource;
      }

      @Override
      public int getListenerRadius() {
         return 8;
      }

      @Override
      public GameEventListener.DeliveryMode getDeliveryMode() {
         return GameEventListener.DeliveryMode.BY_DISTANCE;
      }

      @Override
      public boolean handleGameEvent(ServerLevel p_283470_, Holder<GameEvent> p_332335_, GameEvent.Context p_283014_, Vec3 p_282350_) {
         if (p_332335_.is(GameEvent.ENTITY_DIE) && p_283014_.sourceEntity() instanceof LivingEntity livingentity) {
            if (!livingentity.wasExperienceConsumed()) {
               DamageSource damagesource = livingentity.getLastDamageSource();
               int i = livingentity.getExperienceReward(p_283470_, Optionull.map(damagesource, DamageSource::getEntity));
               if (livingentity.shouldDropExperience() && i > 0) {
                  this.sculkSpreader.addCursors(BlockPos.containing(p_282350_.relative(Direction.UP, 0.5)), i);
                  this.tryAwardItSpreadsAdvancement(p_283470_, livingentity);
               }

               livingentity.skipDropExperience();
               this.positionSource
                  .getPosition(p_283470_)
                  .ifPresent(p_449926_ -> this.bloom(p_283470_, BlockPos.containing(p_449926_), this.blockState, p_283470_.getRandom()));
            }

            return true;
         } else {
            return false;
         }
      }

      @VisibleForTesting
      public SculkSpreader getSculkSpreader() {
         return this.sculkSpreader;
      }

      private void bloom(ServerLevel p_281501_, BlockPos p_281448_, BlockState p_281966_, RandomSource p_283606_) {
         p_281501_.setBlock(p_281448_, p_281966_.setValue(SculkCatalystBlock.PULSE, true), 3);
         p_281501_.scheduleTick(p_281448_, p_281966_.getBlock(), 8);
         p_281501_.sendParticles(ParticleTypes.SCULK_SOUL, p_281448_.getX() + 0.5, p_281448_.getY() + 1.15, p_281448_.getZ() + 0.5, 2, 0.2, 0.0, 0.2, 0.0);
         p_281501_.playSound(null, p_281448_, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 2.0F, 0.6F + p_283606_.nextFloat() * 0.4F);
      }

      private void tryAwardItSpreadsAdvancement(Level p_281279_, LivingEntity p_281378_) {
         if (p_281378_.getLastHurtByMob() instanceof ServerPlayer serverplayer) {
            DamageSource damagesource = p_281378_.getLastDamageSource() == null
               ? p_281279_.damageSources().playerAttack(serverplayer)
               : p_281378_.getLastDamageSource();
            CriteriaTriggers.KILL_MOB_NEAR_SCULK_CATALYST.trigger(serverplayer, p_281378_, damagesource);
         }
      }
   }
}
