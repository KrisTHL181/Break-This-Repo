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

    public EquipmentAssetProvider(final PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "equipment");
    }

    private static void bootstrap(final BiConsumer<ResourceKey<EquipmentAsset>, EquipmentClientInfo> consumer) {
        consumer.accept(
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
        consumer.accept(EquipmentAssets.CHAINMAIL, onlyHumanoid("chainmail"));
        consumer.accept(EquipmentAssets.COPPER, humanoidAndMountArmor("copper"));
        consumer.accept(EquipmentAssets.IRON, humanoidAndMountArmor("iron"));
        consumer.accept(EquipmentAssets.GOLD, humanoidAndMountArmor("gold"));
        consumer.accept(EquipmentAssets.DIAMOND, humanoidAndMountArmor("diamond"));
        consumer.accept(
            EquipmentAssets.TURTLE_SCUTE, EquipmentClientInfo.builder().addMainHumanoidLayer(Identifier.withDefaultNamespace("turtle_scute"), false).build()
        );
        consumer.accept(EquipmentAssets.NETHERITE, humanoidAndMountArmor("netherite"));
        consumer.accept(
            EquipmentAssets.ARMADILLO_SCUTE,
            EquipmentClientInfo.builder()
                .addLayers(
                    EquipmentClientInfo.LayerType.WOLF_BODY, EquipmentClientInfo.Layer.onlyIfDyed(Identifier.withDefaultNamespace("armadillo_scute"), false)
                )
                .addLayers(
                    EquipmentClientInfo.LayerType.WOLF_BODY,
                    EquipmentClientInfo.Layer.onlyIfDyed(Identifier.withDefaultNamespace("armadillo_scute_overlay"), true)
                )
                .build()
        );
        consumer.accept(
            EquipmentAssets.ELYTRA,
            EquipmentClientInfo.builder()
                .addLayers(
                    EquipmentClientInfo.LayerType.WINGS, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace("elytra"), Optional.empty(), true)
                )
                .build()
        );
        EquipmentClientInfo.Layer saddleLayer = new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace("saddle"));
        consumer.accept(
            EquipmentAssets.SADDLE,
            EquipmentClientInfo.builder()
                .addLayers(EquipmentClientInfo.LayerType.PIG_SADDLE, saddleLayer)
                .addLayers(EquipmentClientInfo.LayerType.STRIDER_SADDLE, saddleLayer)
                .addLayers(EquipmentClientInfo.LayerType.CAMEL_SADDLE, saddleLayer)
                .addLayers(EquipmentClientInfo.LayerType.CAMEL_HUSK_SADDLE, saddleLayer)
                .addLayers(EquipmentClientInfo.LayerType.HORSE_SADDLE, saddleLayer)
                .addLayers(EquipmentClientInfo.LayerType.DONKEY_SADDLE, saddleLayer)
                .addLayers(EquipmentClientInfo.LayerType.MULE_SADDLE, saddleLayer)
                .addLayers(EquipmentClientInfo.LayerType.SKELETON_HORSE_SADDLE, saddleLayer)
                .addLayers(EquipmentClientInfo.LayerType.ZOMBIE_HORSE_SADDLE, saddleLayer)
                .addLayers(EquipmentClientInfo.LayerType.NAUTILUS_SADDLE, saddleLayer)
                .build()
        );

        for (Entry<DyeColor, ResourceKey<EquipmentAsset>> entry : EquipmentAssets.HARNESSES.entrySet()) {
            DyeColor color = entry.getKey();
            ResourceKey<EquipmentAsset> id = entry.getValue();
            consumer.accept(
                id,
                EquipmentClientInfo.builder()
                    .addLayers(
                        EquipmentClientInfo.LayerType.HAPPY_GHAST_BODY,
                        EquipmentClientInfo.Layer.onlyIfDyed(Identifier.withDefaultNamespace(color.getSerializedName() + "_harness"), false)
                    )
                    .build()
            );
        }

        for (Entry<DyeColor, ResourceKey<EquipmentAsset>> entry : EquipmentAssets.CARPETS.entrySet()) {
            DyeColor color = entry.getKey();
            ResourceKey<EquipmentAsset> id = entry.getValue();
            consumer.accept(
                id,
                EquipmentClientInfo.builder()
                    .addLayers(
                        EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace(color.getSerializedName()))
                    )
                    .build()
            );
        }

        consumer.accept(
            EquipmentAssets.TRADER_LLAMA,
            EquipmentClientInfo.builder()
                .addLayers(EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace("trader_llama")))
                .build()
        );
        consumer.accept(
            EquipmentAssets.TRADER_LLAMA_BABY,
            EquipmentClientInfo.builder()
                .addLayers(EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace("trader_llama_baby")))
                .build()
        );
    }

    private static EquipmentClientInfo onlyHumanoid(final String name) {
        return EquipmentClientInfo.builder().addHumanoidLayers(Identifier.withDefaultNamespace(name)).build();
    }

    private static EquipmentClientInfo humanoidAndMountArmor(final String name) {
        return EquipmentClientInfo.builder()
            .addHumanoidLayers(Identifier.withDefaultNamespace(name))
            .addLayers(EquipmentClientInfo.LayerType.HORSE_BODY, EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace(name), false))
            .addLayers(EquipmentClientInfo.LayerType.NAUTILUS_BODY, EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace(name), false))
            .build();
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentAssets = new HashMap<>();
        bootstrap((id, asset) -> {
            if (equipmentAssets.putIfAbsent(id, asset) != null) {
                throw new IllegalStateException("Tried to register equipment asset twice for id: " + id);
            }
        });
        return DataProvider.saveAll(cache, EquipmentClientInfo.CODEC, this.pathProvider::json, equipmentAssets);
    }

    @Override
    public String getName() {
        return "Equipment Asset Definitions";
    }
}