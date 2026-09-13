package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ConduitBlockEntity extends BlockEntity {
   private static final int BLOCK_REFRESH_RATE = 2;
   private static final int EFFECT_DURATION = 13;
   private static final float ROTATION_SPEED = -0.0375F;
   private static final int MIN_ACTIVE_SIZE = 16;
   private static final int MIN_KILL_SIZE = 42;
   private static final int KILL_RANGE = 8;
   private static final Block[] VALID_BLOCKS = new Block[]{Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.DARK_PRISMARINE};
   public int tickCount;
   private float activeRotation;
   private boolean isActive;
   private boolean isHunting;
   private final List<BlockPos> effectBlocks = Lists.newArrayList();
   private @Nullable EntityReference<LivingEntity> destroyTarget;
   private long nextAmbientSoundActivation;

   public ConduitBlockEntity(BlockPos p_155397_, BlockState p_155398_) {
      super(BlockEntityType.CONDUIT, p_155397_, p_155398_);
   }

   @Override
   protected void loadAdditional(ValueInput p_405930_) {
      super.loadAdditional(p_405930_);
      this.destroyTarget = EntityReference.read(p_405930_, "Target");
   }

   @Override
   protected void saveAdditional(ValueOutput p_409807_) {
      super.saveAdditional(p_409807_);
      EntityReference.store(this.destroyTarget, p_409807_, "Target");
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_327672_) {
      return this.saveCustomOnly(p_327672_);
   }

   public static void clientTick(Level p_155404_, BlockPos p_155405_, BlockState p_155406_, ConduitBlockEntity p_155407_) {
      p_155407_.tickCount++;
      long i = p_155404_.getGameTime();
      List<BlockPos> list = p_155407_.effectBlocks;
      if (i % 40L == 0L) {
         p_155407_.isActive = updateShape(p_155404_, p_155405_, list);
         updateHunting(p_155407_, list);
      }

      LivingEntity livingentity = EntityReference.getLivingEntity(p_155407_.destroyTarget, p_155404_);
      animationTick(p_155404_, p_155405_, list, livingentity, p_155407_.tickCount);
      if (p_155407_.isActive()) {
         p_155407_.activeRotation++;
      }
   }

   public static void serverTick(Level p_155439_, BlockPos p_155440_, BlockState p_155441_, ConduitBlockEntity p_155442_) {
      p_155442_.tickCount++;
      long i = p_155439_.getGameTime();
      List<BlockPos> list = p_155442_.effectBlocks;
      if (i % 40L == 0L) {
         boolean flag = updateShape(p_155439_, p_155440_, list);
         if (flag != p_155442_.isActive) {
            SoundEvent soundevent = flag ? SoundEvents.CONDUIT_ACTIVATE : SoundEvents.CONDUIT_DEACTIVATE;
            p_155439_.playSound(null, p_155440_, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         p_155442_.isActive = flag;
         updateHunting(p_155442_, list);
         if (flag) {
            applyEffects(p_155439_, p_155440_, list);
            updateAndAttackTarget((ServerLevel)p_155439_, p_155440_, p_155441_, p_155442_, list.size() >= 42);
         }
      }

      if (p_155442_.isActive()) {
         if (i % 80L == 0L) {
            p_155439_.playSound(null, p_155440_, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         if (i > p_155442_.nextAmbientSoundActivation) {
            p_155442_.nextAmbientSoundActivation = i + 60L + p_155439_.getRandom().nextInt(40);
            p_155439_.playSound(null, p_155440_, SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.BLOCKS, 1.0F, 1.0F);
         }
      }
   }

   private static void updateHunting(ConduitBlockEntity p_155429_, List<BlockPos> p_155430_) {
      p_155429_.setHunting(p_155430_.size() >= 42);
   }

   private static boolean updateShape(Level p_155415_, BlockPos p_155416_, List<BlockPos> p_155417_) {
      p_155417_.clear();

      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
               BlockPos blockpos = p_155416_.offset(i, j, k);
               if (!p_155415_.isWaterAt(blockpos)) {
                  return false;
               }
            }
         }
      }

      for (int j1 = -2; j1 <= 2; j1++) {
         for (int k1 = -2; k1 <= 2; k1++) {
            for (int l1 = -2; l1 <= 2; l1++) {
               int i2 = Math.abs(j1);
               int l = Math.abs(k1);
               int i1 = Math.abs(l1);
               if ((i2 > 1 || l > 1 || i1 > 1) && (j1 == 0 && (l == 2 || i1 == 2) || k1 == 0 && (i2 == 2 || i1 == 2) || l1 == 0 && (i2 == 2 || l == 2))) {
                  BlockPos blockpos1 = p_155416_.offset(j1, k1, l1);
                  BlockState blockstate = p_155415_.getBlockState(blockpos1);

                  for (Block block : VALID_BLOCKS) {
                     if (blockstate.is(block)) {
                        p_155417_.add(blockpos1);
                     }
                  }
               }
            }
         }
      }

      return p_155417_.size() >= 16;
   }

   private static void applyEffects(Level p_155444_, BlockPos p_155445_, List<BlockPos> p_155446_) {
      int i = p_155446_.size();
      int j = i / 7 * 16;
      int k = p_155445_.getX();
      int l = p_155445_.getY();
      int i1 = p_155445_.getZ();
      AABB aabb = new AABB(k, l, i1, k + 1, l + 1, i1 + 1).inflate(j).expandTowards(0.0, p_155444_.getHeight(), 0.0);
      List<Player> list = p_155444_.getEntitiesOfClass(Player.class, aabb);
      if (!list.isEmpty()) {
         for (Player player : list) {
            if (p_155445_.closerThan(player.blockPosition(), j) && player.isInWaterOrRain()) {
               player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260, 0, true, true));
            }
         }
      }
   }

   private static void updateAndAttackTarget(ServerLevel p_409086_, BlockPos p_408285_, BlockState p_408764_, ConduitBlockEntity p_406103_, boolean p_405900_) {
      EntityReference<LivingEntity> entityreference = updateDestroyTarget(p_406103_.destroyTarget, p_409086_, p_408285_, p_405900_);
      LivingEntity livingentity = EntityReference.getLivingEntity(entityreference, p_409086_);
      if (livingentity != null) {
         p_409086_.playSound(
            null, livingentity.getX(), livingentity.getY(), livingentity.getZ(), SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.BLOCKS, 1.0F, 1.0F
         );
         livingentity.hurtServer(p_409086_, p_409086_.damageSources().magic(), 4.0F);
      }

      if (!Objects.equals(entityreference, p_406103_.destroyTarget)) {
         p_406103_.destroyTarget = entityreference;
         p_409086_.sendBlockUpdated(p_408285_, p_408764_, p_408764_, 2);
      }
   }

   private static @Nullable EntityReference<LivingEntity> updateDestroyTarget(
      @Nullable EntityReference<LivingEntity> p_408219_, ServerLevel p_406553_, BlockPos p_155410_, boolean p_406113_
   ) {
      if (!p_406113_) {
         return null;
      }

      if (p_408219_ == null) {
         return selectNewTarget(p_406553_, p_155410_);
      }

      LivingEntity livingentity = EntityReference.getLivingEntity(p_408219_, p_406553_);
      return livingentity != null && livingentity.isAlive() && p_155410_.closerThan(livingentity.blockPosition(), 8.0) ? p_408219_ : null;
   }

   private static @Nullable EntityReference<LivingEntity> selectNewTarget(ServerLevel p_406173_, BlockPos p_409120_) {
      List<LivingEntity> list = p_406173_.getEntitiesOfClass(
         LivingEntity.class, getDestroyRangeAABB(p_409120_), p_449916_ -> p_449916_ instanceof Enemy && p_449916_.isInWaterOrRain()
      );
      return list.isEmpty() ? null : EntityReference.of(Util.getRandom(list, p_406173_.random));
   }

   private static AABB getDestroyRangeAABB(BlockPos p_155432_) {
      return new AABB(p_155432_).inflate(8.0);
   }

   private static void animationTick(Level p_155419_, BlockPos p_155420_, List<BlockPos> p_155421_, @Nullable Entity p_155422_, int p_155423_) {
      RandomSource randomsource = p_155419_.random;
      double d0 = Mth.sin((p_155423_ + 35) * 0.1F) / 2.0F + 0.5F;
      d0 = (d0 * d0 + d0) * 0.3F;
      Vec3 vec3 = new Vec3(p_155420_.getX() + 0.5, p_155420_.getY() + 1.5 + d0, p_155420_.getZ() + 0.5);

      for (BlockPos blockpos : p_155421_) {
         if (randomsource.nextInt(50) == 0) {
            BlockPos blockpos1 = blockpos.subtract(p_155420_);
            float f = -0.5F + randomsource.nextFloat() + blockpos1.getX();
            float f1 = -2.0F + randomsource.nextFloat() + blockpos1.getY();
            float f2 = -0.5F + randomsource.nextFloat() + blockpos1.getZ();
            p_155419_.addParticle(ParticleTypes.NAUTILUS, vec3.x, vec3.y, vec3.z, f, f1, f2);
         }
      }

      if (p_155422_ != null) {
         Vec3 vec31 = new Vec3(p_155422_.getX(), p_155422_.getEyeY(), p_155422_.getZ());
         float f3 = (-0.5F + randomsource.nextFloat()) * (3.0F + p_155422_.getBbWidth());
         float f4 = -1.0F + randomsource.nextFloat() * p_155422_.getBbHeight();
         float f5 = (-0.5F + randomsource.nextFloat()) * (3.0F + p_155422_.getBbWidth());
         Vec3 vec32 = new Vec3(f3, f4, f5);
         p_155419_.addParticle(ParticleTypes.NAUTILUS, vec31.x, vec31.y, vec31.z, vec32.x, vec32.y, vec32.z);
      }
   }

   public boolean isActive() {
      return this.isActive;
   }

   public boolean isHunting() {
      return this.isHunting;
   }

   private void setHunting(boolean p_59215_) {
      this.isHunting = p_59215_;
   }

   public float getActiveRotation(float p_59198_) {
      return (this.activeRotation + p_59198_) * -0.0375F;
   }
}
