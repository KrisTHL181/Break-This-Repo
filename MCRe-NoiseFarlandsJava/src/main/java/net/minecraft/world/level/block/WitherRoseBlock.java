package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WitherRoseBlock extends FlowerBlock {
    public static final MapCodec<WitherRoseBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(EFFECTS_FIELD.forGetter(FlowerBlock::getSuspiciousEffects), propertiesCodec()).apply(i, WitherRoseBlock::new)
    );

    @Override
    public MapCodec<WitherRoseBlock> codec() {
        return CODEC;
    }

    public WitherRoseBlock(final Holder<MobEffect> mobEffect, final float effectSeconds, final BlockBehaviour.Properties properties) {
        this(makeEffectList(mobEffect, effectSeconds), properties);
    }

    public WitherRoseBlock(final SuspiciousStewEffects suspiciousStewEffects, final BlockBehaviour.Properties properties) {
        super(suspiciousStewEffects, properties);
    }

    @Override
    protected boolean mayPlaceOn(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.is(BlockTags.SUPPORTS_WITHER_ROSE);
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        VoxelShape shape = this.getShape(state, level, pos, CollisionContext.empty());
        Vec3 shapeCenter = shape.bounds().getCenter();
        double x = pos.getX() + shapeCenter.x;
        double z = pos.getZ() + shapeCenter.z;

        for (int i = 0; i < 3; i++) {
            if (random.nextBoolean()) {
                level.addParticle(
                    ParticleTypes.SMOKE, x + random.nextDouble() / 5.0, pos.getY() + (0.5 - random.nextDouble()), z + random.nextDouble() / 5.0, 0.0, 0.0, 0.0
                );
            }
        }
    }

    @Override
    protected void entityInside(
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Entity entity,
        final InsideBlockEffectApplier effectApplier,
        final boolean isPrecise
    ) {
        if (level instanceof ServerLevel serverLevel
            && level.getDifficulty() != Difficulty.PEACEFUL
            && entity instanceof LivingEntity livingEntity
            && !livingEntity.isInvulnerableTo(serverLevel, level.damageSources().wither())) {
            livingEntity.addEffect(this.getBeeInteractionEffect());
        }
    }

    @Override
    public MobEffectInstance getBeeInteractionEffect() {
        return new MobEffectInstance(MobEffects.WITHER, 40);
    }
}