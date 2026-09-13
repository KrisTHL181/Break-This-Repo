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

   protected void apply(Map<Identifier, EquipmentClientInfo> p_376723_, ResourceManager p_378073_, ProfilerFiller p_377463_) {
      this.equipmentAssets = p_376723_.entrySet()
         .stream()
         .collect(Collectors.toUnmodifiableMap(p_448441_ -> ResourceKey.create(EquipmentAssets.ROOT_ID, p_448441_.getKey()), Entry::getValue));
   }

   public EquipmentClientInfo get(ResourceKey<EquipmentAsset> p_376890_) {
      return this.equipmentAssets.getOrDefault(p_376890_, MISSING);
   }
}
