package net.minecraft.world.entity.animal.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SnowGolem extends AbstractGolem implements Shearable, RangedAttackMob {
   private static final EntityDataAccessor<Byte> DATA_PUMPKIN_ID = SynchedEntityData.defineId(SnowGolem.class, EntityDataSerializers.BYTE);
   private static final byte PUMPKIN_FLAG = 16;
   private static final boolean DEFAULT_PUMPKIN = true;

   public SnowGolem(EntityType<? extends SnowGolem> p_455193_, Level p_460841_) {
      super(p_455193_, p_460841_);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.25, 20, 10.0F));
      this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0, 1.0000001E-5F));
      this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, (p_459742_, p_452021_) -> p_459742_ instanceof Enemy));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.2F);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_453552_) {
      super.defineSynchedData(p_453552_);
      p_453552_.define(DATA_PUMPKIN_ID, (byte)16);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_453870_) {
      super.addAdditionalSaveData(p_453870_);
      p_453870_.putBoolean("Pumpkin", this.hasPumpkin());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_452467_) {
      super.readAdditionalSaveData(p_452467_);
      this.setPumpkin(p_452467_.getBooleanOr("Pumpkin", true));
   }

   @Override
   public boolean isSensitiveToWater() {
      return true;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.level() instanceof ServerLevel serverlevel) {
         if (serverlevel.environmentAttributes().getValue(EnvironmentAttributes.SNOW_GOLEM_MELTS, this.position())) {
            this.hurtServer(serverlevel, this.damageSources().onFire(), 1.0F);
         }

         if (!serverlevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return;
         }

         BlockState blockstate = Blocks.SNOW.defaultBlockState();

         for (int i = 0; i < 4; i++) {
            int j = Mth.floor(this.getX() + (i % 2 * 2 - 1) * 0.25F);
            int k = Mth.floor(this.getY());
            int l = Mth.floor(this.getZ() + (i / 2 % 2 * 2 - 1) * 0.25F);
            BlockPos blockpos = new BlockPos(j, k, l);
            if (this.level().getBlockState(blockpos).isAir() && blockstate.canSurvive(this.level(), blockpos)) {
               this.level().setBlockAndUpdate(blockpos, blockstate);
               this.level().gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(this, blockstate));
            }
         }
      }
   }

   @Override
   public void performRangedAttack(LivingEntity p_459857_, float p_457402_) {
      double d0 = p_459857_.getX() - this.getX();
      double d1 = p_459857_.getEyeY() - 1.1F;
      double d2 = p_459857_.getZ() - this.getZ();
      double d3 = Math.sqrt(d0 * d0 + d2 * d2) * 0.2F;
      if (this.level() instanceof ServerLevel serverlevel) {
         ItemStack itemstack = new ItemStack(Items.SNOWBALL);
         Projectile.spawnProjectile(
            new Snowball(serverlevel, this, itemstack), serverlevel, itemstack, p_456363_ -> p_456363_.shoot(d0, d1 + d3 - p_456363_.getY(), d2, 1.6F, 12.0F)
         );
      }

      this.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 0.4F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
   }

   @Override
   protected InteractionResult mobInteract(Player p_458095_, InteractionHand p_457948_) {
      ItemStack itemstack = p_458095_.getItemInHand(p_457948_);
      if (itemstack.is(Items.SHEARS) && this.readyForShearing()) {
         if (this.level() instanceof ServerLevel serverlevel) {
            this.shear(serverlevel, SoundSource.PLAYERS, itemstack);
            this.gameEvent(GameEvent.SHEAR, p_458095_);
            itemstack.hurtAndBreak(1, p_458095_, p_457948_.asEquipmentSlot());
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public void shear(ServerLevel p_455102_, SoundSource p_453971_, ItemStack p_456270_) {
      p_455102_.playSound(null, this, SoundEvents.SNOW_GOLEM_SHEAR, p_453971_, 1.0F, 1.0F);
      this.setPumpkin(false);
      this.dropFromShearingLootTable(
         p_455102_, BuiltInLootTables.SHEAR_SNOW_GOLEM, p_456270_, (p_454848_, p_457672_) -> this.spawnAtLocation(p_454848_, p_457672_, this.getEyeHeight())
      );
   }

   @Override
   public boolean readyForShearing() {
      return this.isAlive() && this.hasPumpkin();
   }

   public boolean hasPumpkin() {
      return (this.entityData.get(DATA_PUMPKIN_ID) & 16) != 0;
   }

   public void setPumpkin(boolean p_455627_) {
      byte b0 = this.entityData.get(DATA_PUMPKIN_ID);
      if (p_455627_) {
         this.entityData.set(DATA_PUMPKIN_ID, (byte)(b0 | 16));
      } else {
         this.entityData.set(DATA_PUMPKIN_ID, (byte)(b0 & -17));
      }
   }

   @Override
   protected @Nullable SoundEvent getAmbientSound() {
      return SoundEvents.SNOW_GOLEM_AMBIENT;
   }

   @Override
   protected @Nullable SoundEvent getHurtSound(DamageSource p_459394_) {
      return SoundEvents.SNOW_GOLEM_HURT;
   }

   @Override
   protected @Nullable SoundEvent getDeathSound() {
      return SoundEvents.SNOW_GOLEM_DEATH;
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.75F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }
}
