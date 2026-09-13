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
import net.minecraft.world.entity.MobCategory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class MobSpawnSettings {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final float DEFAULT_CREATURE_SPAWN_PROBABILITY = 0.1F;
   public static final WeightedList<MobSpawnSettings.SpawnerData> EMPTY_MOB_LIST = WeightedList.of();
   public static final MobSpawnSettings EMPTY = new MobSpawnSettings.Builder().build();
   public static final MapCodec<MobSpawnSettings> CODEC = RecordCodecBuilder.mapCodec(
      p_187051_ -> p_187051_.group(
            Codec.floatRange(0.0F, 0.9999999F)
               .optionalFieldOf("creature_spawn_probability", 0.1F)
               .forGetter(p_187055_ -> p_187055_.creatureGenerationProbability),
            Codec.simpleMap(
                  MobCategory.CODEC,
                  WeightedList.codec(MobSpawnSettings.SpawnerData.CODEC).promotePartial(Util.prefix("Spawn data: ", LOGGER::error)),
                  StringRepresentable.keys(MobCategory.values())
               )
               .fieldOf("spawners")
               .forGetter(p_187053_ -> p_187053_.spawners),
            Codec.simpleMap(BuiltInRegistries.ENTITY_TYPE.byNameCodec(), MobSpawnSettings.MobSpawnCost.CODEC, BuiltInRegistries.ENTITY_TYPE)
               .fieldOf("spawn_costs")
               .forGetter(p_187049_ -> p_187049_.mobSpawnCosts)
         )
         .apply(p_187051_, MobSpawnSettings::new)
   );
   private final float creatureGenerationProbability;
   private final Map<MobCategory, WeightedList<MobSpawnSettings.SpawnerData>> spawners;
   private final Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts;

   MobSpawnSettings(
      float p_196689_, Map<MobCategory, WeightedList<MobSpawnSettings.SpawnerData>> p_196690_, Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> p_196691_
   ) {
      this.creatureGenerationProbability = p_196689_;
      this.spawners = ImmutableMap.copyOf(p_196690_);
      this.mobSpawnCosts = ImmutableMap.copyOf(p_196691_);
   }

   public WeightedList<MobSpawnSettings.SpawnerData> getMobs(MobCategory p_151799_) {
      return this.spawners.getOrDefault(p_151799_, EMPTY_MOB_LIST);
   }

   public MobSpawnSettings.@Nullable MobSpawnCost getMobSpawnCost(EntityType<?> p_48346_) {
      return this.mobSpawnCosts.get(p_48346_);
   }

   public float getCreatureProbability() {
      return this.creatureGenerationProbability;
   }

   public static class Builder {
      private final Map<MobCategory, WeightedList.Builder<MobSpawnSettings.SpawnerData>> spawners = Util.makeEnumMap(
         MobCategory.class, p_390891_ -> WeightedList.builder()
      );
      private final Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts = Maps.newLinkedHashMap();
      private float creatureGenerationProbability = 0.1F;

      public MobSpawnSettings.Builder addSpawn(MobCategory p_48377_, int p_395205_, MobSpawnSettings.SpawnerData p_48378_) {
         this.spawners.get(p_48377_).add(p_48378_, p_395205_);
         return this;
      }

      public MobSpawnSettings.Builder addMobCharge(EntityType<?> p_48371_, double p_48372_, double p_48373_) {
         this.mobSpawnCosts.put(p_48371_, new MobSpawnSettings.MobSpawnCost(p_48373_, p_48372_));
         return this;
      }

      public MobSpawnSettings.Builder creatureGenerationProbability(float p_48369_) {
         this.creatureGenerationProbability = p_48369_;
         return this;
      }

      public MobSpawnSettings build() {
         return new MobSpawnSettings(
            this.creatureGenerationProbability,
            this.spawners
               .entrySet()
               .stream()
               .collect(ImmutableMap.toImmutableMap(Entry::getKey, p_390890_ -> ((WeightedList.Builder)p_390890_.getValue()).build())),
            ImmutableMap.copyOf(this.mobSpawnCosts)
         );
      }
   }

   public record MobSpawnCost(double energyBudget, double charge) {
      public static final Codec<MobSpawnSettings.MobSpawnCost> CODEC = RecordCodecBuilder.create(
         p_48399_ -> p_48399_.group(
               Codec.DOUBLE.fieldOf("energy_budget").forGetter(p_151813_ -> p_151813_.energyBudget),
               Codec.DOUBLE.fieldOf("charge").forGetter(p_151811_ -> p_151811_.charge)
            )
            .apply(p_48399_, MobSpawnSettings.MobSpawnCost::new)
      );
   }

   public record SpawnerData(EntityType<?> type, int minCount, int maxCount) {
      public static final MapCodec<MobSpawnSettings.SpawnerData> CODEC = RecordCodecBuilder.mapCodec(
            p_390892_ -> p_390892_.group(
                  BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(p_151826_ -> p_151826_.type()),
                  ExtraCodecs.POSITIVE_INT.fieldOf("minCount").forGetter(p_151824_ -> p_151824_.minCount),
                  ExtraCodecs.POSITIVE_INT.fieldOf("maxCount").forGetter(p_151820_ -> p_151820_.maxCount)
               )
               .apply(p_390892_, MobSpawnSettings.SpawnerData::new)
         )
         .validate(
            p_275168_ -> p_275168_.minCount > p_275168_.maxCount
               ? DataResult.error(() -> "minCount needs to be smaller or equal to maxCount")
               : DataResult.success(p_275168_)
         );

      public SpawnerData {
         type = type.getCategory() == MobCategory.MISC ? EntityType.PIG : type;
      }

      @Override
      public String toString() {
         return EntityType.getKey(this.type) + "*(" + this.minCount + "-" + this.maxCount + ")";
      }
   }
}
