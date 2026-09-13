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

public class SnowGolem extends AbstractGolem implements RangedAttackMob, Shearable {
    private static final EntityDataAccessor<Byte> DATA_PUMPKIN_ID = SynchedEntityData.defineId(SnowGolem.class, EntityDataSerializers.BYTE);
    private static final byte PUMPKIN_FLAG = 16;
    private static final boolean DEFAULT_PUMPKIN = true;

    public SnowGolem(final EntityType<? extends SnowGolem> type, final Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.25, 20, 10.0F));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0, 1.0000001E-5F));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, (target, level) -> target instanceof Enemy));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.2F);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_PUMPKIN_ID, (byte)16);
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Pumpkin", this.hasPumpkin());
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setPumpkin(input.getBooleanOr("Pumpkin", true));
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.environmentAttributes().getValue(EnvironmentAttributes.SNOW_GOLEM_MELTS, this.position())) {
                this.hurtServer(serverLevel, this.damageSources().onFire(), 1.0F);
            }

            if (!serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
                return;
            }

            BlockState snow = Blocks.SNOW.defaultBlockState();

            for (int i = 0; i < 4; i++) {
                int xx = Mth.floor(this.getX() + (i % 2 * 2 - 1) * 0.25F);
                int yy = Mth.floor(this.getY());
                int zz = Mth.floor(this.getZ() + (i / 2 % 2 * 2 - 1) * 0.25F);
                BlockPos snowPos = new BlockPos(xx, yy, zz);
                if (this.level().getBlockState(snowPos).isAir() && snow.canSurvive(this.level(), snowPos)) {
                    this.level().setBlockAndUpdate(snowPos, snow);
                    this.level().gameEvent(GameEvent.BLOCK_PLACE, snowPos, GameEvent.Context.of(this, snow));
                }
            }
        }
    }

    @Override
    public void performRangedAttack(final LivingEntity target, final float power) {
        double xd = target.getX() - this.getX();
        double yd = target.getEyeY() - 1.1F;
        double zd = target.getZ() - this.getZ();
        double yo = Math.sqrt(xd * xd + zd * zd) * 0.2F;
        if (this.level() instanceof ServerLevel serverLevel) {
            ItemStack itemStack = new ItemStack(Items.SNOWBALL);
            Projectile.spawnProjectile(
                new Snowball(serverLevel, this, itemStack),
                serverLevel,
                itemStack,
                projectile -> projectile.shoot(xd, yd + yo - projectile.getY(), zd, 1.6F, 12.0F)
            );
        }

        this.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 0.4F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    @Override
    protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.SHEARS) && this.readyForShearing()) {
            if (this.level() instanceof ServerLevel level) {
                this.shear(level, SoundSource.PLAYERS, itemStack);
                this.gameEvent(GameEvent.SHEAR, player);
                itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            }

            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public void shear(final ServerLevel level, final SoundSource soundSource, final ItemStack tool) {
        level.playSound(null, this, SoundEvents.SNOW_GOLEM_SHEAR, soundSource, 1.0F, 1.0F);
        this.setPumpkin(false);
        this.dropFromShearingLootTable(level, BuiltInLootTables.SHEAR_SNOW_GOLEM, tool, (l, drop) -> this.spawnAtLocation(l, drop, this.getEyeHeight()));
    }

    @Override
    public boolean readyForShearing() {
        return this.hasPumpkin();
    }

    public boolean hasPumpkin() {
        return (this.entityData.get(DATA_PUMPKIN_ID) & 16) != 0;
    }

    public void setPumpkin(final boolean pumpkin) {
        byte current = this.entityData.get(DATA_PUMPKIN_ID);
        if (pumpkin) {
            this.entityData.set(DATA_PUMPKIN_ID, (byte)(current | 16));
        } else {
            this.entityData.set(DATA_PUMPKIN_ID, (byte)(current & -17));
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.SNOW_GOLEM_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(final DamageSource source) {
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