package net.minecraft.client.data.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EquipmentAssetProvider implements DataProvider {
   private final PackOutput.PathProvider pathProvider;

   public EquipmentAssetProvider(PackOutput p_378812_) {
      this.pathProvider = p_378812_.createPathProvider(PackOutput.Target.RESOURCE_PACK, "equipment");
   }

   private static void bootstrap(BiConsumer<ResourceKey<EquipmentAsset>, EquipmentClientInfo> p_375964_) {
      p_375964_.accept(
         EquipmentAssets.LEATHER,
         EquipmentClientInfo.builder()
            .addHumanoidLayers(Identifier.withDefaultNamespace("leather"), true)
            .addHumanoidLayers(Identifier.withDefaultNamespace("leather_overlay"), false)
            .addLayers(
               EquipmentClientInfo.LayerType.HORSE_BODY,
               EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace("leather"), true),
               EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace("leather_overlay"), false)
            )
            .build()
      );
      p_375964_.accept(EquipmentAssets.CHAINMAIL, onlyHumanoid("chainmail"));
      p_375964_.accept(EquipmentAssets.COPPER, humanoidAndMountArmor("copper"));
      p_375964_.accept(EquipmentAssets.IRON, humanoidAndMountArmor("iron"));
      p_375964_.accept(EquipmentAssets.GOLD, humanoidAndMountArmor("gold"));
      p_375964_.accept(EquipmentAssets.DIAMOND, humanoidAndMountArmor("diamond"));
      p_375964_.accept(
         EquipmentAssets.TURTLE_SCUTE, EquipmentClientInfo.builder().addMainHumanoidLayer(Identifier.withDefaultNamespace("turtle_scute"), false).build()
      );
      p_375964_.accept(EquipmentAssets.NETHERITE, humanoidAndMountArmor("netherite"));
      p_375964_.accept(
         EquipmentAssets.ARMADILLO_SCUTE,
         EquipmentClientInfo.builder()
            .addLayers(EquipmentClientInfo.LayerType.WOLF_BODY, EquipmentClientInfo.Layer.onlyIfDyed(Identifier.withDefaultNamespace("armadillo_scute"), false))
            .addLayers(
               EquipmentClientInfo.LayerType.WOLF_BODY, EquipmentClientInfo.Layer.onlyIfDyed(Identifier.withDefaultNamespace("armadillo_scute_overlay"), true)
            )
            .build()
      );
      p_375964_.accept(
         EquipmentAssets.ELYTRA,
         EquipmentClientInfo.builder()
            .addLayers(EquipmentClientInfo.LayerType.WINGS, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace("elytra"), Optional.empty(), true))
            .build()
      );
      EquipmentClientInfo.Layer equipmentclientinfo$layer = new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace("saddle"));
      p_375964_.accept(
         EquipmentAssets.SADDLE,
         EquipmentClientInfo.builder()
            .addLayers(EquipmentClientInfo.LayerType.PIG_SADDLE, equipmentclientinfo$layer)
            .addLayers(EquipmentClientInfo.LayerType.STRIDER_SADDLE, equipmentclientinfo$layer)
            .addLayers(EquipmentClientInfo.LayerType.CAMEL_SADDLE, equipmentclientinfo$layer)
            .addLayers(EquipmentClientInfo.LayerType.CAMEL_HUSK_SADDLE, equipmentclientinfo$layer)
            .addLayers(EquipmentClientInfo.LayerType.HORSE_SADDLE, equipmentclientinfo$layer)
            .addLayers(EquipmentClientInfo.LayerType.DONKEY_SADDLE, equipmentclientinfo$layer)
            .addLayers(EquipmentClientInfo.LayerType.MULE_SADDLE, equipmentclientinfo$layer)
            .addLayers(EquipmentClientInfo.LayerType.SKELETON_HORSE_SADDLE, equipmentclientinfo$layer)
            .addLayers(EquipmentClientInfo.LayerType.ZOMBIE_HORSE_SADDLE, equipmentclientinfo$layer)
            .addLayers(EquipmentClientInfo.LayerType.NAUTILUS_SADDLE, equipmentclientinfo$layer)
            .build()
      );

      for (Entry<DyeColor, ResourceKey<EquipmentAsset>> entry : EquipmentAssets.HARNESSES.entrySet()) {
         DyeColor dyecolor = entry.getKey();
         ResourceKey<EquipmentAsset> resourcekey = entry.getValue();
         p_375964_.accept(
            resourcekey,
            EquipmentClientInfo.builder()
               .addLayers(
                  EquipmentClientInfo.LayerType.HAPPY_GHAST_BODY,
                  EquipmentClientInfo.Layer.onlyIfDyed(Identifier.withDefaultNamespace(dyecolor.getSerializedName() + "_harness"), false)
               )
               .build()
         );
      }

      for (Entry<DyeColor, ResourceKey<EquipmentAsset>> entry1 : EquipmentAssets.CARPETS.entrySet()) {
         DyeColor dyecolor1 = entry1.getKey();
         ResourceKey<EquipmentAsset> resourcekey1 = entry1.getValue();
         p_375964_.accept(
            resourcekey1,
            EquipmentClientInfo.builder()
               .addLayers(
                  EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace(dyecolor1.getSerializedName()))
               )
               .build()
         );
      }

      p_375964_.accept(
         EquipmentAssets.TRADER_LLAMA,
         EquipmentClientInfo.builder()
            .addLayers(EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace("trader_llama")))
            .build()
      );
   }

   private static EquipmentClientInfo onlyHumanoid(String p_376219_) {
      return EquipmentClientInfo.builder().addHumanoidLayers(Identifier.withDefaultNamespace(p_376219_)).build();
   }

   private static EquipmentClientInfo humanoidAndMountArmor(String p_452938_) {
      return EquipmentClientInfo.builder()
         .addHumanoidLayers(Identifier.withDefaultNamespace(p_452938_))
         .addLayers(EquipmentClientInfo.LayerType.HORSE_BODY, EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace(p_452938_), false))
         .addLayers(EquipmentClientInfo.LayerType.NAUTILUS_BODY, EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace(p_452938_), false))
         .build();
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_376319_) {
      Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> map = new HashMap<>();
      bootstrap((p_376477_, p_377690_) -> {
         if (map.putIfAbsent(p_376477_, p_377690_) != null) {
            throw new IllegalStateException("Tried to register equipment asset twice for id: " + p_376477_);
         }
      });
      return DataProvider.saveAll(p_376319_, EquipmentClientInfo.CODEC, this.pathProvider::json, map);
   }

   @Override
   public String getName() {
      return "Equipment Asset Definitions";
   }
}
