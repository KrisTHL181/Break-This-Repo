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

   protected SpellcasterIllager(final EntityType<? extends SpellcasterIllager> type, final Level level) {
      super(type, level);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_SPELL_CASTING_ID, (byte)0);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.spellCastingTickCount = input.getIntOr("SpellTicks", 0);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putInt("SpellTicks", this.spellCastingTickCount);
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

   public void setIsCastingSpell(final SpellcasterIllager.IllagerSpell spell) {
      this.currentSpell = spell;
      this.entityData.set(DATA_SPELL_CASTING_ID, (byte)spell.id);
   }

   protected SpellcasterIllager.IllagerSpell getCurrentSpell() {
      return !this.level().isClientSide() ? this.currentSpell : SpellcasterIllager.IllagerSpell.byId(this.entityData.get(DATA_SPELL_CASTING_ID));
   }

   @Override
   protected void customServerAiStep(final ServerLevel level) {
      super.customServerAiStep(level);
      if (this.spellCastingTickCount > 0) {
         this.spellCastingTickCount--;
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide() && this.isCastingSpell()) {
         SpellcasterIllager.IllagerSpell spell = this.getCurrentSpell();
         float red = (float)spell.spellColor[0];
         float green = (float)spell.spellColor[1];
         float blue = (float)spell.spellColor[2];
         float bodyAngle = this.yBodyRot * (float) (Math.PI / 180.0) + Mth.cos(this.tickCount * 0.6662F) * 0.25F;
         float cos = Mth.cos(bodyAngle);
         float sin = Mth.sin(bodyAngle);
         double handDistance = 0.6 * this.getScale();
         double handHeight = 1.8 * this.getScale();
         this.level()
            .addParticle(
               ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, red, green, blue),
               this.getX() + cos * handDistance,
               this.getY() + handHeight,
               this.getZ() + sin * handDistance,
               0.0,
               0.0,
               0.0
            );
         this.level()
            .addParticle(
               ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, red, green, blue),
               this.getX() - cos * handDistance,
               this.getY() + handHeight,
               this.getZ() - sin * handDistance,
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

      private static final IntFunction<SpellcasterIllager.IllagerSpell> BY_ID = ByIdMap.continuous(e -> e.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
      private final int id;
      private final double[] spellColor;

      IllagerSpell(final int id, final double red, final double green, final double blue) {
         this.id = id;
         this.spellColor = new double[]{red, green, blue};
      }

      public static SpellcasterIllager.IllagerSpell byId(final int id) {
         return BY_ID.apply(id);
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
         LivingEntity target = SpellcasterIllager.this.getTarget();
         if (target == null || !target.isAlive()) {
            return false;
         } else {
            return SpellcasterIllager.this.isCastingSpell() ? false : SpellcasterIllager.this.tickCount >= this.nextAttackTickCount;
         }
      }

      @Override
      public boolean canContinueToUse() {
         LivingEntity target = SpellcasterIllager.this.getTarget();
         return target != null && target.isAlive() && this.attackWarmupDelay > 0;
      }

      @Override
      public void start() {
         this.attackWarmupDelay = this.adjustedTickDelay(this.getCastWarmupTime());
         SpellcasterIllager.this.spellCastingTickCount = this.getCastingTime();
         this.nextAttackTickCount = SpellcasterIllager.this.tickCount + this.getCastingInterval();
         SoundEvent spellPrepareSound = this.getSpellPrepareSound();
         if (spellPrepareSound != null) {
            SpellcasterIllager.this.playSound(spellPrepareSound, 1.0F, 1.0F);
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
