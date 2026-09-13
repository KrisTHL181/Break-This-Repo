package net.minecraft.world.entity.monster.illager;

import java.util.EnumSet;
import java.util.function.IntFunction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class SpellcasterIllager extends AbstractIllager {
   private static final EntityDataAccessor<Byte> DATA_SPELL_CASTING_ID = SynchedEntityData.defineId(SpellcasterIllager.class, EntityDataSerializers.BYTE);
   private static final int DEFAULT_SPELLCASTING_TICKS = 0;
   protected int spellCastingTickCount = 0;
   private SpellcasterIllager.IllagerSpell currentSpell = SpellcasterIllager.IllagerSpell.NONE;

   protected SpellcasterIllager(EntityType<? extends SpellcasterIllager> p_455504_, Level p_452521_) {
      super(p_455504_, p_452521_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_459215_) {
      super.defineSynchedData(p_459215_);
      p_459215_.define(DATA_SPELL_CASTING_ID, (byte)0);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_454519_) {
      super.readAdditionalSaveData(p_454519_);
      this.spellCastingTickCount = p_454519_.getIntOr("SpellTicks", 0);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_456717_) {
      super.addAdditionalSaveData(p_456717_);
      p_456717_.putInt("SpellTicks", this.spellCastingTickCount);
   }

   @Override
   public AbstractIllager.IllagerArmPose getArmPose() {
      if (this.isCastingSpell()) {
         return AbstractIllager.IllagerArmPose.SPELLCASTING;
      } else {
         return this.isCelebrating() ? AbstractIllager.IllagerArmPose.CELEBRATING : AbstractIllager.IllagerArmPose.CROSSED;
      }
   }

   public boolean isCastingSpell() {
      return this.level().isClientSide() ? this.entityData.get(DATA_SPELL_CASTING_ID) > 0 : this.spellCastingTickCount > 0;
   }

   public void setIsCastingSpell(SpellcasterIllager.IllagerSpell p_458607_) {
      this.currentSpell = p_458607_;
      this.entityData.set(DATA_SPELL_CASTING_ID, (byte)p_458607_.id);
   }

   protected SpellcasterIllager.IllagerSpell getCurrentSpell() {
      return !this.level().isClientSide() ? this.currentSpell : SpellcasterIllager.IllagerSpell.byId(this.entityData.get(DATA_SPELL_CASTING_ID));
   }

   @Override
   protected void customServerAiStep(ServerLevel p_455158_) {
      super.customServerAiStep(p_455158_);
      if (this.spellCastingTickCount > 0) {
         this.spellCastingTickCount--;
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide() && this.isCastingSpell()) {
         SpellcasterIllager.IllagerSpell spellcasterillager$illagerspell = this.getCurrentSpell();
         float f = (float)spellcasterillager$illagerspell.spellColor[0];
         float f1 = (float)spellcasterillager$illagerspell.spellColor[1];
         float f2 = (float)spellcasterillager$illagerspell.spellColor[2];
         float f3 = this.yBodyRot * (float) (Math.PI / 180.0) + Mth.cos(this.tickCount * 0.6662F) * 0.25F;
         float f4 = Mth.cos(f3);
         float f5 = Mth.sin(f3);
         double d0 = 0.6 * this.getScale();
         double d1 = 1.8 * this.getScale();
         this.level()
            .addParticle(
               ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, f, f1, f2),
               this.getX() + f4 * d0,
               this.getY() + d1,
               this.getZ() + f5 * d0,
               0.0,
               0.0,
               0.0
            );
         this.level()
            .addParticle(
               ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, f, f1, f2),
               this.getX() - f4 * d0,
               this.getY() + d1,
               this.getZ() - f5 * d0,
               0.0,
               0.0,
               0.0
            );
      }
   }

   protected int getSpellCastingTime() {
      return this.spellCastingTickCount;
   }

   protected abstract SoundEvent getCastingSoundEvent();

   protected enum IllagerSpell {
      NONE(0, 0.0, 0.0, 0.0),
      SUMMON_VEX(1, 0.7, 0.7, 0.8),
      FANGS(2, 0.4, 0.3, 0.35),
      WOLOLO(3, 0.7, 0.5, 0.2),
      DISAPPEAR(4, 0.3, 0.3, 0.8),
      BLINDNESS(5, 0.1, 0.1, 0.2);

      private static final IntFunction<SpellcasterIllager.IllagerSpell> BY_ID = ByIdMap.continuous(
         p_451143_ -> p_451143_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
      );
      final int id;
      final double[] spellColor;

      IllagerSpell(final int p_455005_, final double p_454711_, final double p_451646_, final double p_455192_) {
         this.id = p_455005_;
         this.spellColor = new double[]{p_454711_, p_451646_, p_455192_};
      }

      public static SpellcasterIllager.IllagerSpell byId(int p_453358_) {
         return BY_ID.apply(p_453358_);
      }
   }

   protected class SpellcasterCastingSpellGoal extends Goal {
      public SpellcasterCastingSpellGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         return SpellcasterIllager.this.getSpellCastingTime() > 0;
      }

      @Override
      public void start() {
         super.start();
         SpellcasterIllager.this.navigation.stop();
      }

      @Override
      public void stop() {
         super.stop();
         SpellcasterIllager.this.setIsCastingSpell(SpellcasterIllager.IllagerSpell.NONE);
      }

      @Override
      public void tick() {
         if (SpellcasterIllager.this.getTarget() != null) {
            SpellcasterIllager.this.getLookControl()
               .setLookAt(SpellcasterIllager.this.getTarget(), SpellcasterIllager.this.getMaxHeadYRot(), SpellcasterIllager.this.getMaxHeadXRot());
         }
      }
   }

   protected abstract class SpellcasterUseSpellGoal extends Goal {
      protected int attackWarmupDelay;
      protected int nextAttackTickCount;

      @Override
      public boolean canUse() {
         LivingEntity livingentity = SpellcasterIllager.this.getTarget();
         if (livingentity != null && livingentity.isAlive()) {
            return SpellcasterIllager.this.isCastingSpell() ? false : SpellcasterIllager.this.tickCount >= this.nextAttackTickCount;
         } else {
            return false;
         }
      }

      @Override
      public boolean canContinueToUse() {
         LivingEntity livingentity = SpellcasterIllager.this.getTarget();
         return livingentity != null && livingentity.isAlive() && this.attackWarmupDelay > 0;
      }

      @Override
      public void start() {
         this.attackWarmupDelay = this.adjustedTickDelay(this.getCastWarmupTime());
         SpellcasterIllager.this.spellCastingTickCount = this.getCastingTime();
         this.nextAttackTickCount = SpellcasterIllager.this.tickCount + this.getCastingInterval();
         SoundEvent soundevent = this.getSpellPrepareSound();
         if (soundevent != null) {
            SpellcasterIllager.this.playSound(soundevent, 1.0F, 1.0F);
         }

         SpellcasterIllager.this.setIsCastingSpell(this.getSpell());
      }

      @Override
      public void tick() {
         this.attackWarmupDelay--;
         if (this.attackWarmupDelay == 0) {
            this.performSpellCasting();
            SpellcasterIllager.this.playSound(SpellcasterIllager.this.getCastingSoundEvent(), 1.0F, 1.0F);
         }
      }

      protected abstract void performSpellCasting();

      protected int getCastWarmupTime() {
         return 20;
      }

      protected abstract int getCastingTime();

      protected abstract int getCastingInterval();

      protected abstract @Nullable SoundEvent getSpellPrepareSound();

      protected abstract SpellcasterIllager.IllagerSpell getSpell();
   }
}
