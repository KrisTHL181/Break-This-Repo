package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class MobSpawnSettings {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float DEFAULT_CREATURE_SPAWN_PROBABILITY = 0.1F;
    public static final WeightedList<MobSpawnSettings.SpawnerData> EMPTY_MOB_LIST = WeightedList.of();
    public static final MobSpawnSettings EMPTY = new MobSpawnSettings.Builder().build();
    public static final MapCodec<MobSpawnSettings> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Codec.floatRange(0.0F, 0.9999999F).optionalFieldOf("creature_spawn_probability", 0.1F).forGetter(b -> b.creatureGenerationProbability),
                Codec.simpleMap(
                        MobCategory.CODEC,
                        WeightedList.codec(MobSpawnSettings.SpawnerData.CODEC).promotePartial(Util.prefix("Spawn data: ", LOGGER::error)),
                        StringRepresentable.keys(MobCategory.values())
                    )
                    .fieldOf("spawners")
                    .forGetter(b -> b.spawners),
                Codec.simpleMap(BuiltInRegistries.ENTITY_TYPE.byNameCodec(), MobSpawnSettings.MobSpawnCost.CODEC, BuiltInRegistries.ENTITY_TYPE)
                    .fieldOf("spawn_costs")
                    .forGetter(b -> b.mobSpawnCosts)
            )
            .apply(i, MobSpawnSettings::new)
    );
    private final float creatureGenerationProbability;
    private final Map<MobCategory, WeightedList<MobSpawnSettings.SpawnerData>> spawners;
    private final Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts;

    private MobSpawnSettings(
        final float creatureGenerationProbability,
        final Map<MobCategory, WeightedList<MobSpawnSettings.SpawnerData>> spawners,
        final Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts
    ) {
        this.creatureGenerationProbability = creatureGenerationProbability;
        this.spawners = ImmutableMap.copyOf(spawners);
        this.mobSpawnCosts = ImmutableMap.copyOf(mobSpawnCosts);
    }

    public WeightedList<MobSpawnSettings.SpawnerData> getMobs(final MobCategory category) {
        return this.spawners.getOrDefault(category, EMPTY_MOB_LIST);
    }

    public MobSpawnSettings.@Nullable MobSpawnCost getMobSpawnCost(final EntityType<?> type) {
        return this.mobSpawnCosts.get(type);
    }

    public float getCreatureProbability() {
        return this.creatureGenerationProbability;
    }

    public static class Builder {
        private final Map<MobCategory, WeightedList.Builder<MobSpawnSettings.SpawnerData>> spawners = Util.makeEnumMap(
            MobCategory.class, c -> WeightedList.builder()
        );
        private final Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts = Maps.newLinkedHashMap();
        private float creatureGenerationProbability = 0.1F;

        public MobSpawnSettings.Builder addSpawn(final MobCategory category, final int weight, final MobSpawnSettings.SpawnerData spawnerData) {
            this.spawners.get(category).add(spawnerData, weight);
            return this;
        }

        public MobSpawnSettings.Builder addMobCharge(final EntityType<?> type, final double charge, final double energyBudget) {
            this.mobSpawnCosts.put(type, new MobSpawnSettings.MobSpawnCost(energyBudget, charge));
            return this;
        }

        public MobSpawnSettings.Builder creatureGenerationProbability(final float creatureGenerationProbability) {
            this.creatureGenerationProbability = creatureGenerationProbability;
            return this;
        }

        public MobSpawnSettings build() {
            return new MobSpawnSettings(
                this.creatureGenerationProbability,
                this.spawners.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().build())),
                ImmutableMap.copyOf(this.mobSpawnCosts)
            );
        }
    }

    public record MobSpawnCost(double energyBudget, double charge) {
        public static final Codec<MobSpawnSettings.MobSpawnCost> CODEC = RecordCodecBuilder.create(
            i -> i.group(Codec.DOUBLE.fieldOf("energy_budget").forGetter(e -> e.energyBudget), Codec.DOUBLE.fieldOf("charge").forGetter(e -> e.charge))
                .apply(i, MobSpawnSettings.MobSpawnCost::new)
        );
    }

    public record SpawnerData(EntityType<?> type, int minCount, int maxCount) {
        public static final MapCodec<MobSpawnSettings.SpawnerData> CODEC = RecordCodecBuilder.<MobSpawnSettings.SpawnerData>mapCodec(
                i -> i.group(
                        BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(d -> d.type),
                        ExtraCodecs.POSITIVE_INT.fieldOf("minCount").forGetter(e -> e.minCount),
                        ExtraCodecs.POSITIVE_INT.fieldOf("maxCount").forGetter(e -> e.maxCount)
                    )
                    .apply(i, MobSpawnSettings.SpawnerData::new)
            )
            .validate(
                spawnerData -> spawnerData.minCount > spawnerData.maxCount
                    ? DataResult.error(() -> "minCount needs to be smaller or equal to maxCount")
                    : DataResult.success(spawnerData)
            );

        public SpawnerData {
            type = type.getCategory() == MobCategory.MISC ? EntityTypes.PIG : type;
        }

        @Override
        public String toString() {
            return EntityType.getKey(this.type) + "*(" + this.minCount + "-" + this.maxCount + ")";
        }
    }
}