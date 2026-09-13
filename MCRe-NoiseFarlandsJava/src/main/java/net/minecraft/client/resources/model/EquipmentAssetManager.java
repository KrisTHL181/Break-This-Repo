package net.minecraft.client.resources.model;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EquipmentAssetManager extends SimpleJsonResourceReloadListener<EquipmentClientInfo> {
    public static final EquipmentClientInfo MISSING = new EquipmentClientInfo(Map.of());
    private static final FileToIdConverter ASSET_LISTER = FileToIdConverter.json("equipment");
    private Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentAssets = Map.of();

    public EquipmentAssetManager() {
        super(EquipmentClientInfo.CODEC, ASSET_LISTER);
    }

    protected void apply(final Map<Identifier, EquipmentClientInfo> preparations, final ResourceManager manager, final ProfilerFiller profiler) {
        this.equipmentAssets = preparations.entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(e -> ResourceKey.create(EquipmentAssets.ROOT_ID, e.getKey()), Entry::getValue));
    }

    public EquipmentClientInfo get(final ResourceKey<EquipmentAsset> id) {
        return this.equipmentAssets.getOrDefault(id, MISSING);
    }
}