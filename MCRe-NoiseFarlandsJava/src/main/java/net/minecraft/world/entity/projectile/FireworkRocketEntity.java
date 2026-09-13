package net.minecraft.world.entity.projectile;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FireworkRocketEntity extends Projectile implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> DATA_ID_FIREWORKS_ITEM = SynchedEntityData.defineId(
        FireworkRocketEntity.class, EntityDataSerializers.ITEM_STACK
    );
    private static final EntityDataAccessor<OptionalInt> DATA_ATTACHED_TO_TARGET = SynchedEntityData.defineId(
        FireworkRocketEntity.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT
    );
    private static final EntityDataAccessor<Boolean> DATA_SHOT_AT_ANGLE = SynchedEntityData.defineId(FireworkRocketEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int DEFAULT_LIFE = 0;
    private static final int DEFAULT_LIFE_TIME = 0;
    private static final boolean DEFAULT_SHOT_AT_ANGLE = false;
    private int life = 0;
    private int lifetime = 0;
    private @Nullable LivingEntity attachedToEntity;

    public FireworkRocketEntity(final EntityType<? extends FireworkRocketEntity> type, final Level level) {
        super(type, level);
    }

    public FireworkRocketEntity(final Level level, final double x, final double y, final double z, final ItemStack sourceItemStack) {
        super(EntityTypes.FIREWORK_ROCKET, level);
        this.life = 0;
        this.setPos(x, y, z);
        this.entityData.set(DATA_ID_FIREWORKS_ITEM, sourceItemStack.copy());
        int flightCount = 1;
        Fireworks fireworks = sourceItemStack.get(DataComponents.FIREWORKS);
        if (fireworks != null) {
            flightCount += fireworks.flightDuration();
        }

        this.setDeltaMovement(this.random.triangle(0.0, 0.002297), 0.05, this.random.triangle(0.0, 0.002297));
        this.lifetime = 10 * flightCount + this.random.nextInt(6) + this.random.nextInt(7);
    }

    public FireworkRocketEntity(
        final Level level, final @Nullable Entity owner, final double x, final double y, final double z, final ItemStack sourceItemStack
    ) {
        this(level, x, y, z, sourceItemStack);
        this.setOwner(owner);
    }

    public FireworkRocketEntity(final Level level, final ItemStack sourceItemStack, final LivingEntity stuckTo) {
        this(level, stuckTo, stuckTo.getX(), stuckTo.getY(), stuckTo.getZ(), sourceItemStack);
        this.entityData.set(DATA_ATTACHED_TO_TARGET, OptionalInt.of(stuckTo.getId()));
        this.attachedToEntity = stuckTo;
    }

    public FireworkRocketEntity(final Level level, final ItemStack sourceItemStack, final double x, final double y, final double z, final boolean shotAtAngle) {
        this(level, x, y, z, sourceItemStack);
        this.entityData.set(DATA_SHOT_AT_ANGLE, shotAtAngle);
    }

    public FireworkRocketEntity(
        final Level level, final ItemStack sourceItemStack, final Entity owner, final double x, final double y, final double z, final boolean shotAtAngle
    ) {
        this(level, sourceItemStack, x, y, z, shotAtAngle);
        this.setOwner(owner);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
        entityData.define(DATA_ID_FIREWORKS_ITEM, getDefaultItem());
        entityData.define(DATA_ATTACHED_TO_TARGET, OptionalInt.empty());
        entityData.define(DATA_SHOT_AT_ANGLE, false);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(final double distance) {
        return distance < 4096.0 && !this.isAttachedToEntity();
    }

    @Override
    public boolean shouldRender(final double camX, final double camY, final double camZ) {
        return super.shouldRender(camX, camY, camZ) && !this.isAttachedToEntity();
    }

    @Override
    public void tick() {
        super.tick();
        HitResult hitResult;
        if (this.isAttachedToEntity()) {
            if (this.attachedToEntity == null) {
                this.entityData.get(DATA_ATTACHED_TO_TARGET).ifPresent(id -> {
                    if (this.level().getEntity(id) instanceof LivingEntity livingEntity) {
                        this.attachedToEntity = livingEntity;
                    }
                });
            }

            if (this.attachedToEntity != null) {
                Vec3 handAngle;
                if (this.attachedToEntity.isFallFlying()) {
                    Vec3 lookAngle = this.attachedToEntity.getLookAngle();
                    double power = 1.5;
                    double powerAdd = 0.1;
                    Vec3 movement = this.attachedToEntity.getDeltaMovement();
                    this.attachedToEntity
                        .setDeltaMovement(
                            movement.add(
                                lookAngle.x * 0.1 + (lookAngle.x * 1.5 - movement.x) * 0.5,
                                lookAngle.y * 0.1 + (lookAngle.y * 1.5 - movement.y) * 0.5,
                                lookAngle.z * 0.1 + (lookAngle.z * 1.5 - movement.z) * 0.5
                            )
                        );
                    handAngle = this.attachedToEntity.getHandHoldingItemAngle(Items.FIREWORK_ROCKET);
                } else {
                    handAngle = Vec3.ZERO;
                }

                this.setPos(this.attachedToEntity.getX() + handAngle.x, this.attachedToEntity.getY() + handAngle.y, this.attachedToEntity.getZ() + handAngle.z);
                this.setDeltaMovement(this.attachedToEntity.getDeltaMovement());
            }

            hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        } else {
            if (!this.isShotAtAngle()) {
                double horizontalAcceleration = this.horizontalCollision ? 1.0 : 1.15;
                this.setDeltaMovement(this.getDeltaMovement().multiply(horizontalAcceleration, 1.0, horizontalAcceleration).add(0.0, 0.04, 0.0));
            }

            Vec3 movement = this.getDeltaMovement();
            hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            this.move(MoverType.SELF, movement);
            this.applyEffectsFromBlocks();
            this.setDeltaMovement(movement);
        }

        if (!this.noPhysics && this.isAlive() && hitResult.getType() != HitResult.Type.MISS) {
            this.hitTargetOrDeflectSelf(hitResult);
            this.needsSync = true;
        }

        this.updateRotation();
        if (this.life == 0 && !this.isSilent()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 3.0F, 1.0F);
        }

        this.life++;
        if (this.level().isClientSide() && this.life % 2 < 2) {
            this.level()
                .addParticle(
                    ParticleTypes.FIREWORK,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    this.random.nextGaussian() * 0.05,
                    -this.getDeltaMovement().y * 0.5,
                    this.random.nextGaussian() * 0.05
                );
        }

        if (this.life > this.lifetime && this.level() instanceof ServerLevel level) {
            this.explode(level);
        }
    }

    private void explode(final ServerLevel level) {
        level.broadcastEntityEvent(this, (byte)17);
        this.gameEvent(GameEvent.EXPLODE, this.getOwner());
        this.dealExplosionDamage(level);
        this.discard();
    }

    @Override
    protected void onHitEntity(final EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (this.level() instanceof ServerLevel level) {
            this.explode(level);
        }
    }

    @Override
    protected void onHitBlock(final BlockHitResult hitResult) {
        BlockPos pos = new BlockPos(hitResult.getBlockPos());
        this.level().getBlockState(pos).entityInside(this.level(), pos, this, InsideBlockEffectApplier.NOOP, true);
        if (this.level() instanceof ServerLevel level && this.hasExplosion()) {
            this.explode(level);
        }

        super.onHitBlock(hitResult);
    }

    private boolean hasExplosion() {
        return !this.getExplosions().isEmpty();
    }

    private void dealExplosionDamage(final ServerLevel level) {
        float damageAmount = 0.0F;
        List<FireworkExplosion> explosions = this.getExplosions();
        if (!explosions.isEmpty()) {
            damageAmount = 5.0F + explosions.size() * 2;
        }

        if (damageAmount > 0.0F) {
            if (this.attachedToEntity != null) {
                this.attachedToEntity.hurtServer(level, this.damageSources().fireworks(this, this.getOwner()), 5.0F + explosions.size() * 2);
            }

            double radius = 5.0;
            Vec3 rocketPos = this.position();

            for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5.0))) {
                if (target != this.attachedToEntity && !(this.distanceToSqr(target) > 25.0)) {
                    boolean canSee = false;

                    for (int testStep = 0; testStep < 2; testStep++) {
                        Vec3 to = new Vec3(target.getX(), target.getY(0.5 * testStep), target.getZ());
                        HitResult clip = this.level().clip(new ClipContext(rocketPos, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                        if (clip.getType() == HitResult.Type.MISS) {
                            canSee = true;
                            break;
                        }
                    }

                    if (canSee) {
                        float damage = damageAmount * (float)Math.sqrt((5.0 - this.distanceTo(target)) / 5.0);
                        target.hurtServer(level, this.damageSources().fireworks(this, this.getOwner()), damage);
                    }
                }
            }
        }
    }

    private boolean isAttachedToEntity() {
        return this.entityData.get(DATA_ATTACHED_TO_TARGET).isPresent();
    }

    public boolean isShotAtAngle() {
        return this.entityData.get(DATA_SHOT_AT_ANGLE);
    }

    @Override
    public void handleEntityEvent(final byte id) {
        if (id == 17 && this.level().isClientSide()) {
            Vec3 movement = this.getDeltaMovement();
            this.level().createFireworks(this.getX(), this.getY(), this.getZ(), movement.x, movement.y, movement.z, this.getExplosions());
        }

        super.handleEntityEvent(id);
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Life", this.life);
        output.putInt("LifeTime", this.lifetime);
        output.store("FireworksItem", ItemStack.CODEC, this.getItem());
        output.putBoolean("ShotAtAngle", this.entityData.get(DATA_SHOT_AT_ANGLE));
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        super.readAdditionalSaveData(input);
        this.life = input.getIntOr("Life", 0);
        this.lifetime = input.getIntOr("LifeTime", 0);
        this.entityData.set(DATA_ID_FIREWORKS_ITEM, input.read("FireworksItem", ItemStack.CODEC).orElse(getDefaultItem()));
        this.entityData.set(DATA_SHOT_AT_ANGLE, input.getBooleanOr("ShotAtAngle", false));
    }

    private List<FireworkExplosion> getExplosions() {
        ItemStack sourceItemStack = this.entityData.get(DATA_ID_FIREWORKS_ITEM);
        Fireworks fireworks = sourceItemStack.get(DataComponents.FIREWORKS);
        return fireworks != null ? fireworks.explosions() : List.of();
    }

    @Override
    public ItemStack getItem() {
        return this.entityData.get(DATA_ID_FIREWORKS_ITEM);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    private static ItemStack getDefaultItem() {
        return new ItemStack(Items.FIREWORK_ROCKET);
    }

    @Override
    public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(final LivingEntity hurtEntity, final DamageSource damageSource) {
        double dx = hurtEntity.position().x - this.position().x;
        double dz = hurtEntity.position().z - this.position().z;
        return DoubleDoubleImmutablePair.of(dx, dz);
    }
}