package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.MoonPhase;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Time extends NeedleDirectionHelper implements RangeSelectItemModelProperty {
    public static final MapCodec<Time> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Codec.BOOL.optionalFieldOf("wobble", true).forGetter(NeedleDirectionHelper::wobble),
                Time.TimeSource.CODEC.fieldOf("source").forGetter(o -> o.source)
            )
            .apply(i, Time::new)
    );
    private final Time.TimeSource source;
    private final RandomSource randomSource = RandomSource.create();
    private final NeedleDirectionHelper.Wobbler wobbler;

    public Time(final boolean wooble, final Time.TimeSource source) {
        super(wooble);
        this.source = source;
        this.wobbler = this.newWobbler(0.9F);
    }

    @Override
    protected float calculate(final ItemStack itemStack, final ClientLevel level, final int seed, final ItemOwner owner) {
        float targetRotation = this.source.get(level, itemStack, owner, this.randomSource);
        long gameTime = level.getGameTime();
        if (this.wobbler.shouldUpdate(gameTime)) {
            this.wobbler.update(gameTime, targetRotation);
        }

        return this.wobbler.rotation();
    }

    @Override
    public MapCodec<Time> type() {
        return MAP_CODEC;
    }

    @OnlyIn(Dist.CLIENT)
    public enum TimeSource implements StringRepresentable {
        RANDOM("random") {
            @Override
            public float get(final ClientLevel level, final ItemStack itemStack, final ItemOwner owner, final RandomSource random) {
                return random.nextFloat();
            }
        },
        DAYTIME("daytime") {
            @Override
            public float get(final ClientLevel level, final ItemStack itemStack, final ItemOwner owner, final RandomSource random) {
                return level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, owner.position()) / 360.0F;
            }
        },
        MOON_PHASE("moon_phase") {
            @Override
            public float get(final ClientLevel level, final ItemStack itemStack, final ItemOwner owner, final RandomSource random) {
                return (float)level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, owner.position()).index() / MoonPhase.COUNT;
            }
        };

        public static final Codec<Time.TimeSource> CODEC = StringRepresentable.fromEnum(Time.TimeSource::values);
        private final String name;

        TimeSource(final String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public abstract float get(final ClientLevel level, final ItemStack itemStack, final ItemOwner owner, final RandomSource random);
    }
}