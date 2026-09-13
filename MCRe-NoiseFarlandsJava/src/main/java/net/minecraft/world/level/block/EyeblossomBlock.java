package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class EyeblossomBlock extends FlowerBlock {
    public static final MapCodec<EyeblossomBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(Codec.BOOL.fieldOf("open").forGetter(e -> e.type.open), propertiesCodec()).apply(i, EyeblossomBlock::new)
    );
    private static final int EYEBLOSSOM_XZ_RANGE = 3;
    private static final int EYEBLOSSOM_Y_RANGE = 2;
    private final EyeblossomBlock.Type type;

    @Override
    public MapCodec<? extends EyeblossomBlock> codec() {
        return CODEC;
    }

    public EyeblossomBlock(final EyeblossomBlock.Type type, final BlockBehaviour.Properties properties) {
        super(type.effect, type.effectDuration, properties);
        this.type = type;
    }

    public EyeblossomBlock(final boolean open, final BlockBehaviour.Properties properties) {
        super(EyeblossomBlock.Type.fromBoolean(open).effect, EyeblossomBlock.Type.fromBoolean(open).effectDuration, properties);
        this.type = EyeblossomBlock.Type.fromBoolean(open);
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (this.type.emitSounds() && random.nextInt(700) == 0) {
            BlockState below = level.getBlockState(pos.below());
            if (below.is(Blocks.PALE_MOSS_BLOCK)) {
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.EYEBLOSSOM_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false);
            }
        }
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (this.tryChangingState(state, level, pos, random)) {
            level.playSound(null, pos, this.type.transform().longSwitchSound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        super.randomTick(state, level, pos, random);
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (this.tryChangingState(state, level, pos, random)) {
            level.playSound(null, pos, this.type.transform().shortSwitchSound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        super.tick(state, level, pos, random);
    }

    private boolean tryChangingState(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        boolean shouldBeOpen = level.environmentAttributes().getValue(EnvironmentAttributes.EYEBLOSSOM_OPEN, pos).toBoolean(this.type.open);
        if (shouldBeOpen == this.type.open) {
            return false;
        }

        EyeblossomBlock.Type newType = this.type.transform();
        level.setBlock(pos, newType.state(), 3);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
        newType.spawnTransformParticle(level, pos, random);
        BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3)).forEach(nearby -> {
            BlockState nearbyState = level.getBlockState(nearby);
            if (nearbyState == state) {
                double distance = Math.sqrt(pos.distSqr(nearby));
                int delay = random.nextIntBetweenInclusive((int)(distance * 5.0), (int)(distance * 10.0));
                level.scheduleTick(nearby, state.getBlock(), delay);
            }
        });
        return true;
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
        if (!level.isClientSide()
            && level.getDifficulty() != Difficulty.PEACEFUL
            && entity instanceof Bee bee
            && Bee.attractsBees(state)
            && !bee.hasEffect(MobEffects.POISON)) {
            bee.addEffect(this.getBeeInteractionEffect());
        }
    }

    @Override
    public MobEffectInstance getBeeInteractionEffect() {
        return new MobEffectInstance(MobEffects.POISON, 25);
    }

    public enum Type {
        OPEN(true, MobEffects.BLINDNESS, 11.0F, SoundEvents.EYEBLOSSOM_OPEN_LONG, SoundEvents.EYEBLOSSOM_OPEN, 16545810),
        CLOSED(false, MobEffects.NAUSEA, 7.0F, SoundEvents.EYEBLOSSOM_CLOSE_LONG, SoundEvents.EYEBLOSSOM_CLOSE, 6250335);

        private final boolean open;
        private final Holder<MobEffect> effect;
        private final float effectDuration;
        private final SoundEvent longSwitchSound;
        private final SoundEvent shortSwitchSound;
        private final int particleColor;

        Type(
            final boolean open,
            final Holder<MobEffect> effect,
            final float duration,
            final SoundEvent longSwitchSound,
            final SoundEvent shortSwitchSound,
            final int particleColor
        ) {
            this.open = open;
            this.effect = effect;
            this.effectDuration = duration;
            this.longSwitchSound = longSwitchSound;
            this.shortSwitchSound = shortSwitchSound;
            this.particleColor = particleColor;
        }

        public Block block() {
            return this.open ? Blocks.OPEN_EYEBLOSSOM : Blocks.CLOSED_EYEBLOSSOM;
        }

        public BlockState state() {
            return this.block().defaultBlockState();
        }

        public EyeblossomBlock.Type transform() {
            return fromBoolean(!this.open);
        }

        public boolean emitSounds() {
            return this.open;
        }

        public static EyeblossomBlock.Type fromBoolean(final boolean open) {
            return open ? OPEN : CLOSED;
        }

        public void spawnTransformParticle(final ServerLevel level, final BlockPos pos, final RandomSource random) {
            Vec3 start = Vec3.atCenterOf(pos);
            double lifetime = 0.5 + random.nextDouble();
            Vec3 velocity = new Vec3(random.nextDouble() - 0.5, random.nextDouble() + 1.0, random.nextDouble() - 0.5);
            Vec3 target = start.add(velocity.scale(lifetime));
            TrailParticleOption particle = new TrailParticleOption(target, this.particleColor, (int)(20.0 * lifetime));
            level.sendParticles(particle, start.x, start.y, start.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        public SoundEvent longSwitchSound() {
            return this.longSwitchSound;
        }
    }
}