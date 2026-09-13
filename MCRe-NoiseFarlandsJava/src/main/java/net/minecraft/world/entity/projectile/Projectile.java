package net.minecraft.world.entity.projectile;

import com.google.common.base.MoreObjects;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class Projectile extends Entity implements TraceableEntity {
    private static final boolean DEFAULT_LEFT_OWNER = false;
    private static final boolean DEFAULT_HAS_BEEN_SHOT = false;
    protected @Nullable EntityReference<Entity> owner;
    private boolean leftOwner = false;
    private boolean leftOwnerChecked;
    private boolean hasBeenShot = false;
    private @Nullable Entity lastDeflectedBy;

    protected Projectile(final EntityType<? extends Projectile> type, final Level level) {
        super(type, level);
    }

    protected void setOwner(final @Nullable EntityReference<Entity> owner) {
        this.owner = owner;
    }

    public void setOwner(final @Nullable Entity owner) {
        this.setOwner(EntityReference.of(owner));
    }

    @Override
    public @Nullable Entity getOwner() {
        return EntityReference.getEntity(this.owner, this.level());
    }

    public Entity getEffectSource() {
        return MoreObjects.firstNonNull(this.getOwner(), this);
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        EntityReference.store(this.owner, output, "Owner");
        if (this.leftOwner) {
            output.putBoolean("LeftOwner", true);
        }

        output.putBoolean("HasBeenShot", this.hasBeenShot);
    }

    protected boolean ownedBy(final Entity entity) {
        return this.owner != null && this.owner.matches(entity);
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        this.setOwner(EntityReference.read(input, "Owner"));
        this.leftOwner = input.getBooleanOr("LeftOwner", false);
        this.hasBeenShot = input.getBooleanOr("HasBeenShot", false);
    }

    @Override
    public void restoreFrom(final Entity oldEntity) {
        super.restoreFrom(oldEntity);
        if (oldEntity instanceof Projectile projectile) {
            this.owner = projectile.owner;
        }
    }

    @Override
    public void tick() {
        if (!this.hasBeenShot) {
            this.gameEvent(GameEvent.PROJECTILE_SHOOT, this.getOwner());
            this.hasBeenShot = true;
        }

        this.checkLeftOwner();
        super.tick();
        this.leftOwnerChecked = false;
    }

    protected void checkLeftOwner() {
        if (!this.leftOwner && !this.leftOwnerChecked) {
            this.leftOwner = this.isOutsideOwnerCollisionRange();
            this.leftOwnerChecked = true;
        }
    }

    private boolean isOutsideOwnerCollisionRange() {
        Entity owner = this.getOwner();
        if (owner != null) {
            AABB aabb = this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0);
            return owner.getRootVehicle()
                .getSelfAndPassengers()
                .filter(EntitySelector.CAN_BE_PICKED)
                .noneMatch(entity -> aabb.intersects(entity.getBoundingBox()));
        } else {
            return true;
        }
    }

    public Vec3 getMovementToShoot(final double xd, final double yd, final double zd, final float pow, final float uncertainty) {
        return new Vec3(xd, yd, zd)
            .normalize()
            .add(
                this.random.triangle(0.0, 0.0172275 * uncertainty),
                this.random.triangle(0.0, 0.0172275 * uncertainty),
                this.random.triangle(0.0, 0.0172275 * uncertainty)
            )
            .scale(pow);
    }

    public void shoot(final double xd, final double yd, final double zd, final float pow, final float uncertainty) {
        Vec3 movement = this.getMovementToShoot(xd, yd, zd, pow, uncertainty);
        this.setDeltaMovement(movement);
        this.needsSync = true;
        double sd = movement.horizontalDistance();
        this.setYRot((float)(Mth.atan2(movement.x, movement.z) * 180.0F / (float)Math.PI));
        this.setXRot((float)(Mth.atan2(movement.y, sd) * 180.0F / (float)Math.PI));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public void shootFromRotation(final Entity source, final float xRot, final float yRot, final float yOffset, final float pow, final float uncertainty) {
        float xd = -Mth.sin(yRot * (float) (Math.PI / 180.0)) * Mth.cos(xRot * (float) (Math.PI / 180.0));
        float yd = -Mth.sin((xRot + yOffset) * (float) (Math.PI / 180.0));
        float zd = Mth.cos(yRot * (float) (Math.PI / 180.0)) * Mth.cos(xRot * (float) (Math.PI / 180.0));
        this.shoot(xd, yd, zd, pow, uncertainty);
        Vec3 sourceMovement = source.getKnownMovement();
        this.setDeltaMovement(this.getDeltaMovement().add(sourceMovement.x, source.onGround() ? 0.0 : sourceMovement.y, sourceMovement.z));
    }

    @Override
    public void onAboveBubbleColumn(final boolean dragDown, final BlockPos pos) {
        double yd = dragDown ? -0.03 : 0.1;
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, yd, 0.0));
        sendBubbleColumnParticles(this.level(), pos);
    }

    @Override
    public void onInsideBubbleColumn(final boolean dragDown) {
        double yd = dragDown ? -0.03 : 0.06;
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, yd, 0.0));
        this.resetFallDistance();
    }

    public static <T extends Projectile> T spawnProjectileFromRotation(
        final Projectile.ProjectileFactory<T> creator,
        final ServerLevel serverLevel,
        final ItemStack itemStack,
        final LivingEntity source,
        final float yOffset,
        final float pow,
        final float uncertainty
    ) {
        return spawnProjectile(
            creator.create(serverLevel, source, itemStack),
            serverLevel,
            itemStack,
            projectile -> projectile.shootFromRotation(source, source.getXRot(), source.getYRot(), yOffset, pow, uncertainty)
        );
    }

    public static <T extends Projectile> T spawnProjectileUsingShoot(
        final Projectile.ProjectileFactory<T> creator,
        final ServerLevel serverLevel,
        final ItemStack itemStack,
        final LivingEntity source,
        final double targetX,
        final double targetY,
        final double targetZ,
        final float pow,
        final float uncertainty
    ) {
        return spawnProjectile(
            creator.create(serverLevel, source, itemStack), serverLevel, itemStack, projectile -> projectile.shoot(targetX, targetY, targetZ, pow, uncertainty)
        );
    }

    public static <T extends Projectile> T spawnProjectileUsingShoot(
        final T projectile,
        final ServerLevel serverLevel,
        final ItemStack itemStack,
        final double targetX,
        final double targetY,
        final double targetZ,
        final float pow,
        final float uncertainty
    ) {
        return spawnProjectile(projectile, serverLevel, itemStack, i -> projectile.shoot(targetX, targetY, targetZ, pow, uncertainty));
    }

    public static <T extends Projectile> T spawnProjectile(final T projectile, final ServerLevel serverLevel, final ItemStack itemStack) {
        return spawnProjectile(projectile, serverLevel, itemStack, ignored -> {});
    }

    public static <T extends Projectile> T spawnProjectile(
        final T projectile, final ServerLevel serverLevel, final ItemStack itemStack, final Consumer<T> shootFunction
    ) {
        shootFunction.accept(projectile);
        serverLevel.addFreshEntity(projectile);
        projectile.applyOnProjectileSpawned(serverLevel, itemStack);
        return projectile;
    }

    public void applyOnProjectileSpawned(final ServerLevel serverLevel, final ItemStack pickupItemStack) {
        EnchantmentHelper.onProjectileSpawned(serverLevel, pickupItemStack, this, item -> {});
        if (this instanceof AbstractArrow arrow) {
            ItemStack weapon = arrow.getWeaponItem();
            if (weapon != null && !weapon.isEmpty() && !pickupItemStack.getItem().equals(weapon.getItem())) {
                EnchantmentHelper.onProjectileSpawned(serverLevel, weapon, this, arrow::onItemBreak);
            }
        }
    }

    protected ProjectileDeflection hitTargetOrDeflectSelf(final HitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult)hitResult;
            Entity entity = entityHitResult.getEntity();
            ProjectileDeflection deflection = entity.deflection(this);
            if (deflection != ProjectileDeflection.NONE) {
                if (entity != this.lastDeflectedBy && this.deflect(deflection, entity, this.owner, false)) {
                    this.lastDeflectedBy = entity;
                }

                return deflection;
            }
        } else if (this.shouldBounceOnWorldBorder() && hitResult instanceof BlockHitResult blockHit && blockHit.isWorldBorderHit()) {
            ProjectileDeflection deflection = ProjectileDeflection.REVERSE;
            if (this.deflect(deflection, null, this.owner, false)) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.2));
                return deflection;
            }
        }

        this.onHit(hitResult);
        return ProjectileDeflection.NONE;
    }

    protected boolean shouldBounceOnWorldBorder() {
        return false;
    }

    public boolean deflect(
        final ProjectileDeflection deflection,
        final @Nullable Entity deflectingEntity,
        final @Nullable EntityReference<Entity> newOwner,
        final boolean byAttack
    ) {
        deflection.deflect(this, deflectingEntity, this.random);
        if (!this.level().isClientSide()) {
            this.setOwner(newOwner);
            this.onDeflection(byAttack);
        }

        return true;
    }

    protected void onDeflection(final boolean byAttack) {
    }

    protected void onItemBreak(final Item item) {
    }

    protected void onHit(final HitResult hitResult) {
        HitResult.Type type = hitResult.getType();
        if (type == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult)hitResult;
            Entity entityHit = entityHitResult.getEntity();
            if (entityHit.is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && entityHit instanceof Projectile projectile) {
                projectile.deflect(ProjectileDeflection.AIM_DEFLECT, this.getOwner(), this.owner, true);
            }

            this.onHitEntity(entityHitResult);
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, hitResult.getLocation(), GameEvent.Context.of(this, null));
        } else if (type == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult)hitResult;
            this.onHitBlock(blockHit);
            BlockPos target = blockHit.getBlockPos();
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, target, GameEvent.Context.of(this, this.level().getBlockState(target)));
        }
    }

    protected void onHitEntity(final EntityHitResult hitResult) {
    }

    protected void onHitBlock(final BlockHitResult hitResult) {
        BlockState state = this.level().getBlockState(hitResult.getBlockPos());
        state.onProjectileHit(this.level(), state, hitResult, this);
    }

    protected boolean canHitEntity(final Entity entity) {
        if (!entity.canBeHitByProjectile()) {
            return false;
        }

        Entity owner = this.getOwner();
        return owner == null || this.leftOwner || !owner.isPassengerOfSameVehicle(entity);
    }

    protected void updateRotation() {
        Vec3 movement = this.getDeltaMovement();
        double sd = movement.horizontalDistance();
        this.setXRot(lerpRotation(this.xRotO, (float)(Mth.atan2(movement.y, sd) * 180.0F / (float)Math.PI)));
        this.setYRot(lerpRotation(this.yRotO, (float)(Mth.atan2(movement.x, movement.z) * 180.0F / (float)Math.PI)));
    }

    protected static float lerpRotation(float rotO, final float rot) {
        while (rot - rotO < -180.0F) {
            rotO -= 360.0F;
        }

        while (rot - rotO >= 180.0F) {
            rotO += 360.0F;
        }

        return Mth.lerp(0.2F, rotO, rot);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(final ServerEntity serverEntity) {
        Entity owner = this.getOwner();
        return new ClientboundAddEntityPacket(this, serverEntity, owner == null ? 0 : owner.getId());
    }

    @Override
    public void recreateFromPacket(final ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        Entity owner = this.level().getEntity(packet.getData());
        if (owner != null) {
            this.setOwner(owner);
        }
    }

    @Override
    public boolean mayInteract(final ServerLevel level, final BlockPos pos) {
        Entity owner = this.getOwner();
        return owner instanceof Player ? owner.mayInteract(level, pos) : owner == null || level.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    public boolean mayBreak(final ServerLevel level) {
        return this.is(EntityTypeTags.IMPACT_PROJECTILES) && level.getGameRules().get(GameRules.PROJECTILES_CAN_BREAK_BLOCKS);
    }

    @Override
    public boolean isPickable() {
        return this.is(EntityTypeTags.REDIRECTABLE_PROJECTILE);
    }

    @Override
    public float getPickRadius() {
        return this.isPickable() ? 1.0F : 0.0F;
    }

    public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(final LivingEntity hurtEntity, final DamageSource damageSource) {
        double dx = this.getDeltaMovement().x;
        double dz = this.getDeltaMovement().z;
        return DoubleDoubleImmutablePair.of(dx, dz);
    }

    @Override
    public int getDimensionChangingDelay() {
        return 2;
    }

    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
        if (!this.isInvulnerableToBase(source)) {
            this.markHurt();
        }

        return false;
    }

    @FunctionalInterface
    public interface ProjectileFactory<T extends Projectile> {
        T create(final ServerLevel level, LivingEntity entity, ItemStack itemStack);
    }
}