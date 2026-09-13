package net.minecraft.world.level.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.timeline.Timeline;

public record DimensionType(
    boolean hasFixedTime,
    boolean hasSkyLight,
    boolean hasCeiling,
    boolean hasEnderDragonFight,
    double coordinateScale,
    int minY,
    int height,
    int logicalHeight,
    HolderSet<Block> infiniburn,
    float ambientLight,
    DimensionType.MonsterSettings monsterSettings,
    DimensionType.Skybox skybox,
    CardinalLighting.Type cardinalLightType,
    EnvironmentAttributeMap attributes,
    HolderSet<Timeline> timelines,
    Optional<Holder<WorldClock>> defaultClock
) {
    // 🔧 MCRe NoiseFarlands: 解除打包位宽限制——高度可到 int 极限
    public static final int BITS_FOR_Y = 12;
    public static final int MIN_HEIGHT = 16;
    // 世界高度容量（height 上限）
    public static final int Y_SIZE = Integer.MAX_VALUE;
    // 数据包允许的 Y 范围：-2147483646 ~ 2147483646（不再是 -2032~2031）
    public static final int MAX_Y = 2147483646;
    public static final int MIN_Y = -2147483646;
    // 哨兵：远高于/远低于世界范围（直接用 int 极限，避免 <<4 溢出）
    public static final int WAY_ABOVE_MAX_Y = Integer.MAX_VALUE;
    public static final int WAY_BELOW_MIN_Y = Integer.MIN_VALUE;
    public static final Codec<DimensionType> DIRECT_CODEC = createDirectCodec(EnvironmentAttributeMap.CODEC);
    public static final Codec<DimensionType> NETWORK_CODEC = createDirectCodec(EnvironmentAttributeMap.NETWORK_CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<DimensionType>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.DIMENSION_TYPE);
    public static final float[] MOON_BRIGHTNESS_PER_PHASE = new float[]{1.0F, 0.75F, 0.5F, 0.25F, 0.0F, 0.25F, 0.5F, 0.75F};
    public static final Codec<Holder<DimensionType>> CODEC = RegistryFileCodec.create(Registries.DIMENSION_TYPE, DIRECT_CODEC);

    public DimensionType {
        if (height < 16) {
            throw new IllegalStateException("height has to be at least 16");
        }

        // 🔧 MCRe：改用 long 比较，防止 min_y + height 在 int 上限附近溢出误判
        if ((long) minY + height > MAX_Y + 1) {
            throw new IllegalStateException("min_y + height cannot be higher than: " + (MAX_Y + 1));
        }

        if (logicalHeight > height) {
            throw new IllegalStateException("logical_height cannot be higher than height");
        }

        if (height % 16 != 0) {
            throw new IllegalStateException("height has to be multiple of 16");
        }

        if (minY % 16 != 0) {
            throw new IllegalStateException("min_y has to be a multiple of 16");
        }
    }

    private static Codec<DimensionType> createDirectCodec(final Codec<EnvironmentAttributeMap> attributeMapCodec) {
        return ExtraCodecs.catchDecoderException(
            RecordCodecBuilder.create(
                i -> i.group(
                        Codec.BOOL.optionalFieldOf("has_fixed_time", false).forGetter(DimensionType::hasFixedTime),
                        Codec.BOOL.fieldOf("has_skylight").forGetter(DimensionType::hasSkyLight),
                        Codec.BOOL.fieldOf("has_ceiling").forGetter(DimensionType::hasCeiling),
                        Codec.BOOL.fieldOf("has_ender_dragon_fight").forGetter(DimensionType::hasEnderDragonFight),
                        Codec.doubleRange(1.0E-5F, Double.POSITIVE_INFINITY).fieldOf("coordinate_scale").forGetter(DimensionType::coordinateScale),
                        Codec.intRange(MIN_Y, MAX_Y).fieldOf("min_y").forGetter(DimensionType::minY),
                        Codec.intRange(16, Y_SIZE).fieldOf("height").forGetter(DimensionType::height),
                        Codec.intRange(0, Y_SIZE).fieldOf("logical_height").forGetter(DimensionType::logicalHeight),
                        RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("infiniburn").forGetter(DimensionType::infiniburn),
                        Codec.FLOAT.fieldOf("ambient_light").forGetter(DimensionType::ambientLight),
                        DimensionType.MonsterSettings.CODEC.forGetter(DimensionType::monsterSettings),
                        DimensionType.Skybox.CODEC.optionalFieldOf("skybox", DimensionType.Skybox.OVERWORLD).forGetter(DimensionType::skybox),
                        CardinalLighting.Type.CODEC
                            .optionalFieldOf("cardinal_light", CardinalLighting.Type.DEFAULT)
                            .forGetter(DimensionType::cardinalLightType),
                        attributeMapCodec.optionalFieldOf("attributes", EnvironmentAttributeMap.EMPTY).forGetter(DimensionType::attributes),
                        RegistryCodecs.homogeneousList(Registries.TIMELINE).optionalFieldOf("timelines", HolderSet.empty()).forGetter(DimensionType::timelines),
                        WorldClock.CODEC.optionalFieldOf("default_clock").forGetter(DimensionType::defaultClock)
                    )
                    .apply(i, DimensionType::new)
            )
        );
    }

    public static double getTeleportationScale(final DimensionType lastDimensionType, final DimensionType newDimensionType) {
        double oldScale = lastDimensionType.coordinateScale();
        double newScale = newDimensionType.coordinateScale();
        return oldScale / newScale;
    }

    public static Path getStorageFolder(final ResourceKey<Level> name, final Path baseFolder) {
        return name.identifier().resolveAgainst(baseFolder.resolve("dimensions"));
    }

    public IntProvider monsterSpawnLightTest() {
        return this.monsterSettings.monsterSpawnLightTest();
    }

    public int monsterSpawnBlockLightLimit() {
        return this.monsterSettings.monsterSpawnBlockLightLimit();
    }

    public boolean hasEndFlashes() {
        return this.skybox == DimensionType.Skybox.END;
    }

    public record MonsterSettings(IntProvider monsterSpawnLightTest, int monsterSpawnBlockLightLimit) {
        public static final MapCodec<DimensionType.MonsterSettings> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    IntProviders.codec(0, 15).fieldOf("monster_spawn_light_level").forGetter(DimensionType.MonsterSettings::monsterSpawnLightTest),
                    Codec.intRange(0, 15).fieldOf("monster_spawn_block_light_limit").forGetter(DimensionType.MonsterSettings::monsterSpawnBlockLightLimit)
                )
                .apply(i, DimensionType.MonsterSettings::new)
        );
    }

    public enum Skybox implements StringRepresentable {
        NONE("none"),
        OVERWORLD("overworld"),
        END("end");

        public static final Codec<DimensionType.Skybox> CODEC = StringRepresentable.fromEnum(DimensionType.Skybox::values);
        private final String name;

        Skybox(final String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}