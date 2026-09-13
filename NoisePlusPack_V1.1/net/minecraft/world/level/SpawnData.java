package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EquipmentTable;

public record SpawnData(CompoundTag entityToSpawn, Optional<SpawnData.CustomSpawnRules> customSpawnRules, Optional<EquipmentTable> equipment) {
   public static final String ENTITY_TAG = "entity";
   public static final Codec<SpawnData> CODEC = RecordCodecBuilder.create(
      p_327238_ -> p_327238_.group(
            CompoundTag.CODEC.fieldOf("entity").forGetter(p_186576_ -> p_186576_.entityToSpawn),
            SpawnData.CustomSpawnRules.CODEC.optionalFieldOf("custom_spawn_rules").forGetter(p_186569_ -> p_186569_.customSpawnRules),
            EquipmentTable.CODEC.optionalFieldOf("equipment").forGetter(p_327239_ -> p_327239_.equipment)
         )
         .apply(p_327238_, SpawnData::new)
   );
   public static final Codec<WeightedList<SpawnData>> LIST_CODEC = WeightedList.codec(CODEC);

   public SpawnData() {
      this(new CompoundTag(), Optional.empty(), Optional.empty());
   }

   public SpawnData {
      Optional<Identifier> optional = entityToSpawn.read("id", Identifier.CODEC);
      if (optional.isPresent()) {
         entityToSpawn.store("id", Identifier.CODEC, optional.get());
      } else {
         entityToSpawn.remove("id");
      }
   }

   public CompoundTag getEntityToSpawn() {
      return this.entityToSpawn;
   }

   public Optional<SpawnData.CustomSpawnRules> getCustomSpawnRules() {
      return this.customSpawnRules;
   }

   public Optional<EquipmentTable> getEquipment() {
      return this.equipment;
   }

   public record CustomSpawnRules(InclusiveRange<Integer> blockLightLimit, InclusiveRange<Integer> skyLightLimit) {
      private static final InclusiveRange<Integer> LIGHT_RANGE = new InclusiveRange<>(0, 15);
      public static final Codec<SpawnData.CustomSpawnRules> CODEC = RecordCodecBuilder.create(
         p_286217_ -> p_286217_.group(
               lightLimit("block_light_limit").forGetter(p_186600_ -> p_186600_.blockLightLimit),
               lightLimit("sky_light_limit").forGetter(p_186595_ -> p_186595_.skyLightLimit)
            )
            .apply(p_286217_, SpawnData.CustomSpawnRules::new)
      );

      private static DataResult<InclusiveRange<Integer>> checkLightBoundaries(InclusiveRange<Integer> p_186593_) {
         return !LIGHT_RANGE.contains(p_186593_) ? DataResult.error(() -> "Light values must be withing range " + LIGHT_RANGE) : DataResult.success(p_186593_);
      }

      private static MapCodec<InclusiveRange<Integer>> lightLimit(String p_286409_) {
         return InclusiveRange.INT.lenientOptionalFieldOf(p_286409_, LIGHT_RANGE).validate(SpawnData.CustomSpawnRules::checkLightBoundaries);
      }

      public boolean isValidPosition(BlockPos p_327859_, ServerLevel p_328424_) {
         return this.blockLightLimit.isValueInRange(p_328424_.getBrightness(LightLayer.BLOCK, p_327859_))
            && this.skyLightLimit.isValueInRange(p_328424_.getBrightness(LightLayer.SKY, p_327859_));
      }
   }
}
