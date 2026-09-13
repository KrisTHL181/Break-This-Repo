package net.minecraft.world.entity.projectile.throwableitemprojectile;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public abstract class AbstractThrownPotion extends ThrowableItemProjectile {
    public static final double SPLASH_RANGE = 4.0;
    protected static final double SPLASH_RANGE_SQ = 16.0;
    public static final Predicate<LivingEntity> WATER_SENSITIVE_OR_ON_FIRE = livingEntity -> livingEntity.isSensitiveToWater() || livingEntity.isOnFire();

    public AbstractThrownPotion(final EntityType<? extends AbstractThrownPotion> type, final Level level) {
        super(type, level);
    }

    public AbstractThrownPotion(final EntityType<? extends AbstractThrownPotion> type, final Level level, final LivingEntity owner, final ItemStack itemStack) {
        super(type, owner, level, itemStack);
    }

    public AbstractThrownPotion(
        final EntityType<? extends AbstractThrownPotion> type, final Level level, final double x, final double y, final double z, final ItemStack itemStack
    ) {
        super(type, x, y, z, level, itemStack);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHitBlock(final BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!this.level().isClientSide()) {
            ItemStack potionItemStack = this.getItem();
            Direction hitDirection = hitResult.getDirection();
            BlockPos blockHitPos = hitResult.getBlockPos();
            BlockPos blockEffectPos = blockHitPos.relative(hitDirection);
            PotionContents potion = potionItemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (potion.is(Potions.WATER)) {
                this.dowseFire(blockEffectPos);
                this.dowseFire(blockEffectPos.relative(hitDirection.getOpposite()));

                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    this.dowseFire(blockEffectPos.relative(direction));
                }
            }
        }
    }

    @Override
    protected void onHit(final HitResult hitResult) {
        super.onHit(hitResult);
        if (this.level() instanceof ServerLevel level) {
            ItemStack potionItemStack = this.getItem();
            PotionContents potion = potionItemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (potion.is(Potions.WATER)) {
                this.onHitAsWater(level);
            } else if (potion.hasEffects()) {
                this.onHitAsPotion(level, potionItemStack, hitResult);
            }

            int type = potion.potion().isPresent() && potion.potion().get().value().hasInstantEffects() ? 2007 : 2002;
            level.levelEvent(type, this.blockPosition(), potion.getColor());
            this.discard();
        }
    }

    private void onHitAsWater(final ServerLevel level) {
        AABB aabb = this.getBoundingBox().inflate(4.0, 2.0, 4.0);

        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, aabb, WATER_SENSITIVE_OR_ON_FIRE)) {
            double dist = this.distanceToSqr(entity);
            if (dist < 16.0) {
                if (entity.isSensitiveToWater()) {
                    entity.hurtServer(level, this.damageSources().indirectMagic(this, this.getOwner()), 1.0F);
                }

                if (entity.isOnFire() && entity.isAlive()) {
                    entity.extinguishFire();
                }
            }
        }

        for (Axolotl axolotl : this.level().getEntitiesOfClass(Axolotl.class, aabb)) {
            axolotl.rehydrate();
        }
    }

    protected abstract void onHitAsPotion(ServerLevel level, ItemStack potionItem, HitResult hitResult);

    private void dowseFire(final BlockPos pos) {
        BlockState blockState = this.level().getBlockState(pos);
        if (blockState.is(BlockTags.FIRE)) {
            this.level().destroyBlock(pos, false, this);
        } else if (AbstractCandleBlock.isLit(blockState)) {
            AbstractCandleBlock.extinguish(null, blockState, this.level(), pos);
        } else if (CampfireBlock.isLitCampfire(blockState)) {
            this.level().levelEvent(null, 1009, pos, 0);
            CampfireBlock.dowse(this.getOwner(), this.level(), pos, blockState);
            this.level().setBlockAndUpdate(pos, blockState.setValue(CampfireBlock.LIT, false));
        }
    }

    @Override
    public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(final LivingEntity hurtEntity, final DamageSource damageSource) {
        double dx = hurtEntity.position().x - this.position().x;
        double dz = hurtEntity.position().z - this.position().z;
        return DoubleDoubleImmutablePair.of(dx, dz);
    }
}