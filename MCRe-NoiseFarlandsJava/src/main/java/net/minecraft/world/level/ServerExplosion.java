package net.minecraft.world.level;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ServerExplosion implements Explosion {
    private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();
    private static final int MAX_DROPS_PER_COMBINED_STACK = 16;
    private static final float LARGE_EXPLOSION_RADIUS = 2.0F;
    private final boolean fire;
    private final Explosion.BlockInteraction blockInteraction;
    private final ServerLevel level;
    private final Vec3 center;
    private final @Nullable Entity source;
    private final float radius;
    private final DamageSource damageSource;
    private final ExplosionDamageCalculator damageCalculator;
    private final Map<Player, Vec3> hitPlayers = new HashMap<>();

    public ServerExplosion(
        final ServerLevel level,
        final @Nullable Entity source,
        final @Nullable DamageSource damageSource,
        final @Nullable ExplosionDamageCalculator damageCalculator,
        final Vec3 center,
        final float radius,
        final boolean fire,
        final Explosion.BlockInteraction blockInteraction
    ) {
        this.level = level;
        this.source = source;
        this.radius = radius;
        this.center = center;
        this.fire = fire;
        this.blockInteraction = blockInteraction;
        this.damageSource = damageSource == null ? level.damageSources().explosion(this) : damageSource;
        this.damageCalculator = damageCalculator == null ? this.makeDamageCalculator(source) : damageCalculator;
    }

    private ExplosionDamageCalculator makeDamageCalculator(final @Nullable Entity source) {
        return source == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityBasedExplosionDamageCalculator(source);
    }

    public static float getSeenPercent(final Vec3 center, final Entity entity) {
        AABB bb = entity.getBoundingBox();
        double xs = 1.0 / ((bb.maxX - bb.minX) * 2.0 + 1.0);
        double ys = 1.0 / ((bb.maxY - bb.minY) * 2.0 + 1.0);
        double zs = 1.0 / ((bb.maxZ - bb.minZ) * 2.0 + 1.0);
        double xOffset = (1.0 - Math.floor(1.0 / xs) * xs) / 2.0;
        double zOffset = (1.0 - Math.floor(1.0 / zs) * zs) / 2.0;
        if (!(xs < 0.0) && !(ys < 0.0) && !(zs < 0.0)) {
            int hits = 0;
            int count = 0;

            for (double xx = 0.0; xx <= 1.0; xx += xs) {
                for (double yy = 0.0; yy <= 1.0; yy += ys) {
                    for (double zz = 0.0; zz <= 1.0; zz += zs) {
                        double x = Mth.lerp(xx, bb.minX, bb.maxX);
                        double y = Mth.lerp(yy, bb.minY, bb.maxY);
                        double z = Mth.lerp(zz, bb.minZ, bb.maxZ);
                        Vec3 from = new Vec3(x + xOffset, y, z + zOffset);
                        if (entity.level().clip(new ClipContext(from, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType()
                            == HitResult.Type.MISS) {
                            hits++;
                        }

                        count++;
                    }
                }
            }

            return (float)hits / count;
        } else {
            return 0.0F;
        }
    }

    @Override
    public float radius() {
        return this.radius;
    }

    @Override
    public Vec3 center() {
        return this.center;
    }

    private List<BlockPos> calculateExplodedPositions() {
        Set<BlockPos> toBlowSet = new HashSet<>();
        int size = 16;

        for (int xx = 0; xx < 16; xx++) {
            for (int yy = 0; yy < 16; yy++) {
                for (int zz = 0; zz < 16; zz++) {
                    if (xx == 0 || xx == 15 || yy == 0 || yy == 15 || zz == 0 || zz == 15) {
                        double xd = xx / 15.0F * 2.0F - 1.0F;
                        double yd = yy / 15.0F * 2.0F - 1.0F;
                        double zd = zz / 15.0F * 2.0F - 1.0F;
                        double d = Math.sqrt(xd * xd + yd * yd + zd * zd);
                        xd /= d;
                        yd /= d;
                        zd /= d;
                        float remainingPower = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
                        double xp = this.center.x;
                        double yp = this.center.y;
                        double zp = this.center.z;
                        float stepSize = 0.3F;

                        while (remainingPower > 0.0F) {
                            BlockPos pos = BlockPos.containing(xp, yp, zp);
                            BlockState block = this.level.getBlockState(pos);
                            FluidState fluid = this.level.getFluidState(pos);
                            if (!this.level.isInWorldBounds(pos)) {
                                break;
                            }

                            Optional<Float> resistance = this.damageCalculator.getBlockExplosionResistance(this, this.level, pos, block, fluid);
                            if (resistance.isPresent()) {
                                remainingPower -= (resistance.get() + 0.3F) * 0.3F;
                            }

                            if (remainingPower > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, pos, block, remainingPower)) {
                                toBlowSet.add(pos);
                            }

                            xp += xd * 0.3F;
                            yp += yd * 0.3F;
                            zp += zd * 0.3F;
                            remainingPower -= 0.22500001F;
                        }
                    }
                }
            }
        }

        return new ObjectArrayList<>(toBlowSet);
    }

    private void hurtEntities() {
        if (!(this.radius < 1.0E-5F)) {
            float doubleRadius = this.radius * 2.0F;
            int x0 = Mth.floor(this.center.x - doubleRadius - 1.0);
            int x1 = Mth.floor(this.center.x + doubleRadius + 1.0);
            int y0 = Mth.floor(this.center.y - doubleRadius - 1.0);
            int y1 = Mth.floor(this.center.y + doubleRadius + 1.0);
            int z0 = Mth.floor(this.center.z - doubleRadius - 1.0);
            int z1 = Mth.floor(this.center.z + doubleRadius + 1.0);

            for (Entity entity : this.level.getEntities(this.source, new AABB(x0, y0, z0, x1, y1, z1))) {
                if (!entity.ignoreExplosion(this)) {
                    double dist = Math.sqrt(entity.distanceToSqr(this.center)) / doubleRadius;
                    if (!(dist > 1.0)) {
                        Vec3 entityOrigin = entity instanceof PrimedTnt ? entity.position() : entity.getEyePosition();
                        Vec3 direction = entityOrigin.subtract(this.center).normalize();
                        boolean shouldDamageEntity = this.damageCalculator.shouldDamageEntity(this, entity);
                        float knockbackMultiplier = this.damageCalculator.getKnockbackMultiplier(entity);
                        float exposure = !shouldDamageEntity && knockbackMultiplier == 0.0F ? 0.0F : getSeenPercent(this.center, entity);
                        if (shouldDamageEntity) {
                            entity.hurtServer(this.level, this.damageSource, this.damageCalculator.getEntityDamageAmount(this, entity, exposure));
                        }

                        double knockbackResistance = entity instanceof LivingEntity livingEntity
                            ? livingEntity.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
                            : 0.0;
                        double knockbackPower = (1.0 - dist) * exposure * knockbackMultiplier * (1.0 - knockbackResistance);
                        Vec3 knockback = direction.scale(knockbackPower);
                        entity.push(knockback);
                        if (entity.is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && entity instanceof Projectile projectile) {
                            projectile.setOwner(this.damageSource.getEntity());
                        } else if (entity instanceof Player player && !player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
                            this.hitPlayers.put(player, knockback);
                        }

                        entity.onExplosionHit(this.source);
                    }
                }
            }
        }
    }

    private void interactWithBlocks(final List<BlockPos> targetBlocks) {
        List<ServerExplosion.StackCollector> stacks = new ArrayList<>();
        Util.shuffle(targetBlocks, this.level.random);

        for (BlockPos pos : targetBlocks) {
            this.level.getBlockState(pos).onExplosionHit(this.level, pos, this, (stackx, position) -> addOrAppendStack(stacks, stackx, position));
        }

        for (ServerExplosion.StackCollector stack : stacks) {
            Block.popResource(this.level, stack.pos, stack.stack);
        }
    }

    private void createFire(final List<BlockPos> targetBlocks) {
        for (BlockPos pos : targetBlocks) {
            if (this.level.random.nextInt(3) == 0 && this.level.getBlockState(pos).isAir() && this.level.getBlockState(pos.below()).isSolidRender()) {
                this.level.setBlockAndUpdate(pos, BaseFireBlock.getState(this.level, pos));
            }
        }
    }

    public int explode() {
        this.level.gameEvent(this.source, GameEvent.EXPLODE, this.center);
        List<BlockPos> toBlow = this.calculateExplodedPositions();
        this.hurtEntities();
        if (this.interactsWithBlocks()) {
            ProfilerFiller profiler = Profiler.get();
            profiler.push("explosion_blocks");
            this.interactWithBlocks(toBlow);
            profiler.pop();
        }

        if (this.fire) {
            this.createFire(toBlow);
        }

        return toBlow.size();
    }

    private static void addOrAppendStack(final List<ServerExplosion.StackCollector> stacks, final ItemStack stack, final BlockPos pos) {
        for (ServerExplosion.StackCollector stackCollector : stacks) {
            stackCollector.tryMerge(stack);
            if (stack.isEmpty()) {
                return;
            }
        }

        stacks.add(new ServerExplosion.StackCollector(pos, stack));
    }

    private boolean interactsWithBlocks() {
        return this.blockInteraction != Explosion.BlockInteraction.KEEP;
    }

    public Map<Player, Vec3> getHitPlayers() {
        return this.hitPlayers;
    }

    @Override
    public ServerLevel level() {
        return this.level;
    }

    @Override
    public @Nullable LivingEntity getIndirectSourceEntity() {
        return Explosion.getIndirectSourceEntity(this.source);
    }

    @Override
    public @Nullable Entity getDirectSourceEntity() {
        return this.source;
    }

    public DamageSource getDamageSource() {
        return this.damageSource;
    }

    @Override
    public Explosion.BlockInteraction getBlockInteraction() {
        return this.blockInteraction;
    }

    @Override
    public boolean canTriggerBlocks() {
        if (this.blockInteraction != Explosion.BlockInteraction.TRIGGER_BLOCK) {
            return false;
        } else {
            return this.source != null && this.source.is(EntityTypes.BREEZE_WIND_CHARGE) ? this.level.getGameRules().get(GameRules.MOB_GRIEFING) : true;
        }
    }

    @Override
    public boolean shouldAffectBlocklikeEntities() {
        boolean mobGriefingEnabled = this.level.getGameRules().get(GameRules.MOB_GRIEFING);
        boolean isNotWindCharge = this.source == null || !this.source.is(EntityTypes.BREEZE_WIND_CHARGE) && !this.source.is(EntityTypes.WIND_CHARGE);
        return mobGriefingEnabled ? isNotWindCharge : this.blockInteraction.shouldAffectBlocklikeEntities() && isNotWindCharge;
    }

    public boolean isSmall() {
        return this.radius < 2.0F || !this.interactsWithBlocks();
    }

    private static class StackCollector {
        private final BlockPos pos;
        private ItemStack stack;

        private StackCollector(final BlockPos pos, final ItemStack stack) {
            this.pos = pos;
            this.stack = stack;
        }

        public void tryMerge(final ItemStack input) {
            if (ItemEntity.areMergable(this.stack, input)) {
                this.stack = ItemEntity.merge(this.stack, input, 16);
            }
        }
    }
}