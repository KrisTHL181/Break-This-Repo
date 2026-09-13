package net.minecraft.world.level.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.file.Path;
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
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.timeline.Timeline;

public record DimensionType(
   boolean hasFixedTime,
   boolean hasSkyLight,
   boolean hasCeiling,
   double coordinateScale,
   int minY,
   int height,
   int logicalHeight,
   TagKey<Block> infiniburn,
   float ambientLight,
   DimensionType.MonsterSettings monsterSettings,
   DimensionType.Skybox skybox,
   DimensionType.CardinalLightType cardinalLightType,
   EnvironmentAttributeMap attributes,
   HolderSet<Timeline> timelines
) {
   public static final int BITS_FOR_Y = BlockPos.PACKED_Y_LENGTH;
   public static final int MIN_HEIGHT = 16;
   public static final int Y_SIZE = (1 << BITS_FOR_Y) - 32;
   public static final int MAX_Y = (Y_SIZE >> 1) - 1;
   public static final int MIN_Y = MAX_Y - Y_SIZE + 1;
   public static final int WAY_ABOVE_MAX_Y = MAX_Y << 4;
   public static final int WAY_BELOW_MIN_Y = MIN_Y << 4;
   public static final Codec<DimensionType> DIRECT_CODEC = createDirectCodec(EnvironmentAttributeMap.CODEC);
   public static final Codec<DimensionType> NETWORK_CODEC = createDirectCodec(EnvironmentAttributeMap.NETWORK_CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<DimensionType>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.DIMENSION_TYPE);
   public static final float[] MOON_BRIGHTNESS_PER_PHASE = new float[]{1.0F, 0.75F, 0.5F, 0.25F, 0.0F, 0.25F, 0.5F, 0.75F};
   public static final Codec<Holder<DimensionType>> CODEC = RegistryFileCodec.create(Registries.DIMENSION_TYPE, DIRECT_CODEC);

   public DimensionType {
      if (height < 16) {
         throw new IllegalStateException("height has to be at least 16");
      }

      if (minY + height > MAX_Y + 1) {
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

   private static Codec<DimensionType> createDirectCodec(Codec<EnvironmentAttributeMap> p_460368_) {
      return ExtraCodecs.catchDecoderException(
         RecordCodecBuilder.create(
            p_449970_ -> p_449970_.group(
                  Codec.BOOL.optionalFieldOf("has_fixed_time", false).forGetter(DimensionType::hasFixedTime),
                  Codec.BOOL.fieldOf("has_skylight").forGetter(DimensionType::hasSkyLight),
                  Codec.BOOL.fieldOf("has_ceiling").forGetter(DimensionType::hasCeiling),
                  Codec.doubleRange(1.0E-5F, 3.0E7).fieldOf("coordinate_scale").forGetter(DimensionType::coordinateScale),
                  Codec.intRange(MIN_Y, MAX_Y).fieldOf("min_y").forGetter(DimensionType::minY),
                  Codec.intRange(16, Y_SIZE).fieldOf("height").forGetter(DimensionType::height),
                  Codec.intRange(0, Y_SIZE).fieldOf("logical_height").forGetter(DimensionType::logicalHeight),
                  TagKey.hashedCodec(Registries.BLOCK).fieldOf("infiniburn").forGetter(DimensionType::infiniburn),
                  Codec.FLOAT.fieldOf("ambient_light").forGetter(DimensionType::ambientLight),
                  DimensionType.MonsterSettings.CODEC.forGetter(DimensionType::monsterSettings),
                  DimensionType.Skybox.CODEC.optionalFieldOf("skybox", DimensionType.Skybox.OVERWORLD).forGetter(DimensionType::skybox),
                  DimensionType.CardinalLightType.CODEC
                     .optionalFieldOf("cardinal_light", DimensionType.CardinalLightType.DEFAULT)
                     .forGetter(DimensionType::cardinalLightType),
                  p_460368_.optionalFieldOf("attributes", EnvironmentAttributeMap.EMPTY).forGetter(DimensionType::attributes),
                  RegistryCodecs.homogeneousList(Registries.TIMELINE).optionalFieldOf("timelines", HolderSet.empty()).forGetter(DimensionType::timelines)
               )
               .apply(p_449970_, DimensionType::new)
         )
      );
   }

   public static double getTeleportationScale(DimensionType p_63909_, DimensionType p_63910_) {
      double d0 = p_63909_.coordinateScale();
      double d1 = p_63910_.coordinateScale();
      return d0 / d1;
   }

   public static Path getStorageFolder(ResourceKey<Level> p_196976_, Path p_196977_) {
      if (p_196976_ == Level.OVERWORLD) {
         return p_196977_;
      } else if (p_196976_ == Level.END) {
         return p_196977_.resolve("DIM1");
      } else {
         return p_196976_ == Level.NETHER
            ? p_196977_.resolve("DIM-1")
            : p_196977_.resolve("dimensions").resolve(p_196976_.identifier().getNamespace()).resolve(p_196976_.identifier().getPath());
      }
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

   public enum CardinalLightType implements StringRepresentable {
      DEFAULT("default"),
      NETHER("nether");

      public static final Codec<DimensionType.CardinalLightType> CODEC = StringRepresentable.fromEnum(DimensionType.CardinalLightType::values);
      private final String name;

      CardinalLightType(final String p_457817_) {
         this.name = p_457817_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   public record MonsterSettings(IntProvider monsterSpawnLightTest, int monsterSpawnBlockLightLimit) {
      public static final MapCodec<DimensionType.MonsterSettings> CODEC = RecordCodecBuilder.mapCodec(
         p_449971_ -> p_449971_.group(
               IntProvider.codec(0, 15).fieldOf("monster_spawn_light_level").forGetter(DimensionType.MonsterSettings::monsterSpawnLightTest),
               Codec.intRange(0, 15).fieldOf("monster_spawn_block_light_limit").forGetter(DimensionType.MonsterSettings::monsterSpawnBlockLightLimit)
            )
            .apply(p_449971_, DimensionType.MonsterSettings::new)
      );
   }

   public enum Skybox implements StringRepresentable {
      NONE("none"),
      OVERWORLD("overworld"),
      END("end");

      public static final Codec<DimensionType.Skybox> CODEC = StringRepresentable.fromEnum(DimensionType.Skybox::values);
      private final String name;

      Skybox(final String p_460730_) {
         this.name = p_460730_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
