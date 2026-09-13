package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record EquipmentClientInfo(Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> layers) {
   private static final Codec<List<EquipmentClientInfo.Layer>> LAYER_LIST_CODEC = ExtraCodecs.nonEmptyList(EquipmentClientInfo.Layer.CODEC.listOf());
   public static final Codec<EquipmentClientInfo> CODEC = RecordCodecBuilder.create(
      p_376111_ -> p_376111_.group(
            ExtraCodecs.nonEmptyMap(Codec.unboundedMap(EquipmentClientInfo.LayerType.CODEC, LAYER_LIST_CODEC))
               .fieldOf("layers")
               .forGetter(EquipmentClientInfo::layers)
         )
         .apply(p_376111_, EquipmentClientInfo::new)
   );

   public static EquipmentClientInfo.Builder builder() {
      return new EquipmentClientInfo.Builder();
   }

   public List<EquipmentClientInfo.Layer> getLayers(EquipmentClientInfo.LayerType p_377530_) {
      return this.layers.getOrDefault(p_377530_, List.of());
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> layersByType = new EnumMap<>(EquipmentClientInfo.LayerType.class);

      Builder() {
      }

      public EquipmentClientInfo.Builder addHumanoidLayers(Identifier p_450319_) {
         return this.addHumanoidLayers(p_450319_, false);
      }

      public EquipmentClientInfo.Builder addHumanoidLayers(Identifier p_454277_, boolean p_377923_) {
         this.addLayers(EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, EquipmentClientInfo.Layer.leatherDyeable(p_454277_, p_377923_));
         this.addMainHumanoidLayer(p_454277_, p_377923_);
         return this;
      }

      public EquipmentClientInfo.Builder addMainHumanoidLayer(Identifier p_459123_, boolean p_378714_) {
         return this.addLayers(EquipmentClientInfo.LayerType.HUMANOID, EquipmentClientInfo.Layer.leatherDyeable(p_459123_, p_378714_));
      }

      public EquipmentClientInfo.Builder addLayers(EquipmentClientInfo.LayerType p_377620_, EquipmentClientInfo.Layer... p_377277_) {
         Collections.addAll(this.layersByType.computeIfAbsent(p_377620_, p_376726_ -> new ArrayList<>()), p_377277_);
         return this;
      }

      public EquipmentClientInfo build() {
         return new EquipmentClientInfo(
            this.layersByType
               .entrySet()
               .stream()
               .collect(ImmutableMap.toImmutableMap(Entry::getKey, p_378270_ -> List.copyOf((Collection)p_378270_.getValue())))
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Dyeable(Optional<Integer> colorWhenUndyed) {
      public static final Codec<EquipmentClientInfo.Dyeable> CODEC = RecordCodecBuilder.create(
         p_377022_ -> p_377022_.group(ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("color_when_undyed").forGetter(EquipmentClientInfo.Dyeable::colorWhenUndyed))
            .apply(p_377022_, EquipmentClientInfo.Dyeable::new)
      );
   }

   @OnlyIn(Dist.CLIENT)
   public record Layer(Identifier textureId, Optional<EquipmentClientInfo.Dyeable> dyeable, boolean usePlayerTexture) {
      public static final Codec<EquipmentClientInfo.Layer> CODEC = RecordCodecBuilder.create(
         p_448442_ -> p_448442_.group(
               Identifier.CODEC.fieldOf("texture").forGetter(EquipmentClientInfo.Layer::textureId),
               EquipmentClientInfo.Dyeable.CODEC.optionalFieldOf("dyeable").forGetter(EquipmentClientInfo.Layer::dyeable),
               Codec.BOOL.optionalFieldOf("use_player_texture", false).forGetter(EquipmentClientInfo.Layer::usePlayerTexture)
            )
            .apply(p_448442_, EquipmentClientInfo.Layer::new)
      );

      public Layer(Identifier p_450881_) {
         this(p_450881_, Optional.empty(), false);
      }

      public static EquipmentClientInfo.Layer leatherDyeable(Identifier p_459107_, boolean p_377080_) {
         return new EquipmentClientInfo.Layer(
            p_459107_, p_377080_ ? Optional.of(new EquipmentClientInfo.Dyeable(Optional.of(-6265536))) : Optional.empty(), false
         );
      }

      public static EquipmentClientInfo.Layer onlyIfDyed(Identifier p_459455_, boolean p_376357_) {
         return new EquipmentClientInfo.Layer(p_459455_, p_376357_ ? Optional.of(new EquipmentClientInfo.Dyeable(Optional.empty())) : Optional.empty(), false);
      }

      public Identifier getTextureLocation(EquipmentClientInfo.LayerType p_375959_) {
         return this.textureId.withPath(p_376895_ -> "textures/entity/equipment/" + p_375959_.getSerializedName() + "/" + p_376895_ + ".png");
      }
   }

   @OnlyIn(Dist.CLIENT)
   public enum LayerType implements StringRepresentable {
      HUMANOID("humanoid"),
      HUMANOID_LEGGINGS("humanoid_leggings"),
      WINGS("wings"),
      WOLF_BODY("wolf_body"),
      HORSE_BODY("horse_body"),
      LLAMA_BODY("llama_body"),
      PIG_SADDLE("pig_saddle"),
      STRIDER_SADDLE("strider_saddle"),
      CAMEL_SADDLE("camel_saddle"),
      CAMEL_HUSK_SADDLE("camel_husk_saddle"),
      HORSE_SADDLE("horse_saddle"),
      DONKEY_SADDLE("donkey_saddle"),
      MULE_SADDLE("mule_saddle"),
      ZOMBIE_HORSE_SADDLE("zombie_horse_saddle"),
      SKELETON_HORSE_SADDLE("skeleton_horse_saddle"),
      HAPPY_GHAST_BODY("happy_ghast_body"),
      NAUTILUS_SADDLE("nautilus_saddle"),
      NAUTILUS_BODY("nautilus_body");

      public static final Codec<EquipmentClientInfo.LayerType> CODEC = StringRepresentable.fromEnum(EquipmentClientInfo.LayerType::values);
      private final String id;

      LayerType(final String p_378823_) {
         this.id = p_378823_;
      }

      @Override
      public String getSerializedName() {
         return this.id;
      }

      public String trimAssetPrefix() {
         return "trims/entity/" + this.id;
      }
   }
}
