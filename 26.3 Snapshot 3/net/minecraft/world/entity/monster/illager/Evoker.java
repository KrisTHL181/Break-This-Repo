package net.minecraft.world.entity.monster.illager;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

public class Evoker extends SpellcasterIllager {
   private @Nullable Sheep wololoTarget;

   public Evoker(final EntityType<? extends Evoker> type, final Level level) {
      super(type, level);
      this.xpReward = 10;
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new Evoker.EvokerCastingSpellGoal());
      this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 8.0F, 0.6, 1.0));
      this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Creaking.class, 8.0F, 0.6, 1.0));
      this.goalSelector.addGoal(4, new Evoker.EvokerSummonSpellGoal());
      this.goalSelector.addGoal(5, new Evoker.EvokerAttackSpellGoal());
      this.goalSelector.addGoal(6, new Evoker.EvokerWololoSpellGoal());
      this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6));
      this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true).setUnseenMemoryTicks(300));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false).setUnseenMemoryTicks(300));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, false));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 12.0).add(Attributes.MAX_HEALTH, 24.0);
   }

   @Override
   public SoundEvent getCelebrateSound() {
      return SoundEvents.EVOKER_CELEBRATE;
   }

   @Override
   protected boolean considersEntityAsAlly(final Entity other) {
      if (other == this) {
         return true;
      }

      if (super.considersEntityAsAlly(other)) {
         return true;
      }

      if (other instanceof Vex vex) {
         LivingEntity rootOwner = vex.getRootOwner();
         if (rootOwner != null && (rootOwner == this || super.considersEntityAsAlly(rootOwner))) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.EVOKER_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.EVOKER_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.EVOKER_HURT;
   }

   private void setWololoTarget(final @Nullable Sheep wololoTarget) {
      this.wololoTarget = wololoTarget;
   }

   private @Nullable Sheep getWololoTarget() {
      return this.wololoTarget;
   }

   @Override
   protected SoundEvent getCastingSoundEvent() {
      return SoundEvents.EVOKER_CAST_SPELL;
   }

   @Override
   public void applyRaidBuffs(final ServerLevel level, final int wave, final boolean isCaptain) {
   }

   private class EvokerAttackSpellGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
      @Override
      protected int getCastingTime() {
         return 40;
      }

      @Override
      protected int getCastingInterval() {
         return 100;
      }

      @Override
      protected void performSpellCasting() {
         LivingEntity target = Evoker.this.getTarget();
         double minY = Math.min(target.getY(), Evoker.this.getY());
         double maxY = Math.max(target.getY(), Evoker.this.getY()) + 1.0;
         float angleTowardsTarget = (float)Mth.atan2(target.getZ() - Evoker.this.getZ(), target.getX() - Evoker.this.getX());
         if (Evoker.this.distanceToSqr(target) < 9.0) {
            for (int i = 0; i < 5; i++) {
               float angle = angleTowardsTarget + i * (float) Math.PI * 0.4F;
               this.createSpellEntity(Evoker.this.getX() + Mth.cos(angle) * 1.5, Evoker.this.getZ() + Mth.sin(angle) * 1.5, minY, maxY, angle, 0);
            }

            for (int i = 0; i < 8; i++) {
               float angle = angleTowardsTarget + i * (float) Math.PI * 2.0F / 8.0F + (float) (Math.PI * 2.0 / 5.0);
               this.createSpellEntity(Evoker.this.getX() + Mth.cos(angle) * 2.5, Evoker.this.getZ() + Mth.sin(angle) * 2.5, minY, maxY, angle, 3);
            }
         } else {
            for (int i = 0; i < 16; i++) {
               double reach = 1.25 * (i + 1);
               int spellSpeed = 1 * i;
               this.createSpellEntity(
                  Evoker.this.getX() + Mth.cos(angleTowardsTarget) * reach,
                  Evoker.this.getZ() + Mth.sin(angleTowardsTarget) * reach,
                  minY,
                  maxY,
                  angleTowardsTarget,
                  spellSpeed
               );
            }
         }
      }

      private void createSpellEntity(final double x, final double z, final double minY, final double maxY, final float angle, final int delayTicks) {
         BlockPos pos = BlockPos.containing(x, maxY, z);
         boolean success = false;
         double topOffset = 0.0;

         do {
            BlockPos below = pos.below();
            BlockState belowState = Evoker.this.level().getBlockState(below);
            if (belowState.isFaceSturdy(Evoker.this.level(), below, Direction.UP)) {
               if (!Evoker.this.level().isEmptyBlock(pos)) {
                  BlockState blockState = Evoker.this.level().getBlockState(pos);
                  VoxelShape shape = blockState.getCollisionShape(Evoker.this.level(), pos);
                  if (!shape.isEmpty()) {
                     topOffset = shape.max(Direction.Axis.Y);
                  }
               }

               success = true;
               break;
            }

            pos = pos.below();
         } while (pos.getY() >= Mth.floor(minY) - 1);

         if (success) {
            Evoker.this.level().addFreshEntity(new EvokerFangs(Evoker.this.level(), x, pos.getY() + topOffset, z, angle, delayTicks, Evoker.this));
            Evoker.this.level().gameEvent(GameEvent.ENTITY_PLACE, new Vec3(x, pos.getY() + topOffset, z), GameEvent.Context.of(Evoker.this));
         }
      }

      @Override
      protected SoundEvent getSpellPrepareSound() {
         return SoundEvents.EVOKER_PREPARE_ATTACK;
      }

      @Override
      protected SpellcasterIllager.IllagerSpell getSpell() {
         return SpellcasterIllager.IllagerSpell.FANGS;
      }
   }

   private class EvokerCastingSpellGoal extends SpellcasterIllager.SpellcasterCastingSpellGoal {
      @Override
      public void tick() {
         if (Evoker.this.getTarget() != null) {
            Evoker.this.getLookControl().setLookAt(Evoker.this.getTarget(), Evoker.this.getMaxHeadYRot(), Evoker.this.getMaxHeadXRot());
         } else if (Evoker.this.getWololoTarget() != null) {
            Evoker.this.getLookControl().setLookAt(Evoker.this.getWololoTarget(), Evoker.this.getMaxHeadYRot(), Evoker.this.getMaxHeadXRot());
         }
      }
   }

   private class EvokerSummonSpellGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
      private final TargetingConditions vexCountTargeting = TargetingConditions.forNonCombat().range(16.0).ignoreLineOfSight().ignoreInvisibilityTesting();

      @Override
      public boolean canUse() {
         if (!super.canUse()) {
            return false;
         }

         int vexes = getServerLevel(Evoker.this.level())
            .getNearbyEntities(Vex.class, this.vexCountTargeting, Evoker.this, Evoker.this.getBoundingBox().inflate(16.0))
            .size();
         return Evoker.this.random.nextInt(8) + 1 > vexes;
      }

      @Override
      protected int getCastingTime() {
         return 100;
      }

      @Override
      protected int getCastingInterval() {
         return 340;
      }

      @Override
      protected void performSpellCasting() {
         ServerLevel serverLevel = (ServerLevel)Evoker.this.level();
         PlayerTeam evokerTeam = Evoker.this.getTeam();

         for (int i = 0; i < 3; i++) {
            BlockPos pos = Evoker.this.blockPosition().offset(-2 + Evoker.this.random.nextInt(5), 1, -2 + Evoker.this.random.nextInt(5));
            Vex vex = EntityTypes.VEX.create(Evoker.this.level(), EntitySpawnReason.MOB_SUMMONED);
            if (vex != null) {
               vex.snapTo(pos, 0.0F, 0.0F);
               vex.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
               vex.setOwner(Evoker.this);
               vex.setBoundOrigin(pos);
               vex.setLimitedLife(20 * (30 + Evoker.this.random.nextInt(90)));
               if (evokerTeam != null) {
                  serverLevel.getScoreboard().addPlayerToTeam(vex.getScoreboardName(), evokerTeam);
               }

               serverLevel.addFreshEntityWithPassengers(vex);
               serverLevel.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(Evoker.this));
            }
         }
      }

      @Override
      protected SoundEvent getSpellPrepareSound() {
         return SoundEvents.EVOKER_PREPARE_SUMMON;
      }

      @Override
      protected SpellcasterIllager.IllagerSpell getSpell() {
         return SpellcasterIllager.IllagerSpell.SUMMON_VEX;
      }
   }

   public class EvokerWololoSpellGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
      private final TargetingConditions wololoTargeting = TargetingConditions.forNonCombat()
         .range(16.0)
         .selector((target, level) -> ((Sheep)target).getColor() == DyeColor.BLUE);

      @Override
      public boolean canUse() {
         if (Evoker.this.getTarget() != null) {
            return false;
         }

         if (Evoker.this.isCastingSpell()) {
            return false;
         }

         if (Evoker.this.tickCount < this.nextAttackTickCount) {
            return false;
         }

         ServerLevel level = getServerLevel(Evoker.this.level());
         if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
         }

         List<Sheep> entities = level.getNearbyEntities(Sheep.class, this.wololoTargeting, Evoker.this, Evoker.this.getBoundingBox().inflate(16.0, 4.0, 16.0));
         if (entities.isEmpty()) {
            return false;
         }

         Evoker.this.setWololoTarget(entities.get(Evoker.this.random.nextInt(entities.size())));
         return true;
      }

      @Override
      public boolean canContinueToUse() {
         return Evoker.this.getWololoTarget() != null && this.attackWarmupDelay > 0;
      }

      @Override
      public void stop() {
         super.stop();
         Evoker.this.setWololoTarget(null);
      }

      @Override
      protected void performSpellCasting() {
         Sheep wololoTarget = Evoker.this.getWololoTarget();
         if (wololoTarget != null && wololoTarget.isAlive()) {
            wololoTarget.setColor(DyeColor.RED);
         }
      }

      @Override
      protected int getCastWarmupTime() {
         return 40;
      }

      @Override
      protected int getCastingTime() {
         return 60;
      }

      @Override
      protected int getCastingInterval() {
         return 140;
      }

      @Override
      protected SoundEvent getSpellPrepareSound() {
         return SoundEvents.EVOKER_PREPARE_WOLOLO;
      }

      @Override
      protected SpellcasterIllager.IllagerSpell getSpell() {
         return SpellcasterIllager.IllagerSpell.WOLOLO;
      }
   }
}
