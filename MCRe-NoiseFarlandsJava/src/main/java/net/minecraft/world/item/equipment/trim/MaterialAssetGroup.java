package net.minecraft.world.item.equipment.trim;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public record MaterialAssetGroup(MaterialAssetGroup.AssetInfo base, Map<ResourceKey<EquipmentAsset>, MaterialAssetGroup.AssetInfo> overrides) {
    public static final String SEPARATOR = "_";
    public static final MapCodec<MaterialAssetGroup> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                MaterialAssetGroup.AssetInfo.CODEC.fieldOf("asset_name").forGetter(MaterialAssetGroup::base),
                Codec.unboundedMap(ResourceKey.codec(EquipmentAssets.ROOT_ID), MaterialAssetGroup.AssetInfo.CODEC)
                    .optionalFieldOf("override_armor_assets", Map.of())
                    .forGetter(MaterialAssetGroup::overrides)
            )
            .apply(i, MaterialAssetGroup::new)
    );
    public static final StreamCodec<ByteBuf, MaterialAssetGroup> STREAM_CODEC = StreamCodec.composite(
        MaterialAssetGroup.AssetInfo.STREAM_CODEC,
        MaterialAssetGroup::base,
        ByteBufCodecs.map(Object2ObjectOpenHashMap::new, ResourceKey.streamCodec(EquipmentAssets.ROOT_ID), MaterialAssetGroup.AssetInfo.STREAM_CODEC),
        MaterialAssetGroup::overrides,
        MaterialAssetGroup::new
    );
    public static final MaterialAssetGroup QUARTZ = create("quartz");
    public static final MaterialAssetGroup IRON = create("iron", Map.of(EquipmentAssets.IRON, "iron_darker"));
    public static final MaterialAssetGroup NETHERITE = create("netherite", Map.of(EquipmentAssets.NETHERITE, "netherite_darker"));
    public static final MaterialAssetGroup REDSTONE = create("redstone");
    public static final MaterialAssetGroup COPPER = create("copper", Map.of(EquipmentAssets.COPPER, "copper_darker"));
    public static final MaterialAssetGroup GOLD = create("gold", Map.of(EquipmentAssets.GOLD, "gold_darker"));
    public static final MaterialAssetGroup EMERALD = create("emerald");
    public static final MaterialAssetGroup DIAMOND = create("diamond", Map.of(EquipmentAssets.DIAMOND, "diamond_darker"));
    public static final MaterialAssetGroup LAPIS = create("lapis");
    public static final MaterialAssetGroup AMETHYST = create("amethyst");
    public static final MaterialAssetGroup RESIN = create("resin");

    public static MaterialAssetGroup create(final String base) {
        return new MaterialAssetGroup(new MaterialAssetGroup.AssetInfo(base), Map.of());
    }

    public static MaterialAssetGroup create(final String base, final Map<ResourceKey<EquipmentAsset>, String> overrides) {
        return new MaterialAssetGroup(new MaterialAssetGroup.AssetInfo(base), Map.copyOf(Maps.transformValues(overrides, MaterialAssetGroup.AssetInfo::new)));
    }

    public MaterialAssetGroup.AssetInfo assetId(final ResourceKey<EquipmentAsset> equipmentAssetId) {
        return this.overrides.getOrDefault(equipmentAssetId, this.base);
    }

    public record AssetInfo(String suffix) {
        public static final Codec<MaterialAssetGroup.AssetInfo> CODEC = ExtraCodecs.RESOURCE_PATH_CODEC
            .xmap(MaterialAssetGroup.AssetInfo::new, MaterialAssetGroup.AssetInfo::suffix);
        public static final StreamCodec<ByteBuf, MaterialAssetGroup.AssetInfo> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(MaterialAssetGroup.AssetInfo::new, MaterialAssetGroup.AssetInfo::suffix);

        public AssetInfo {
            if (!Identifier.isValidPath(suffix)) {
                throw new IllegalArgumentException("Invalid string to use as a resource path element: " + suffix);
            }
        }
    }
}