package net.minecraft.world.item.equipment;

import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;

public interface EquipmentAssets {
   ResourceKey<? extends Registry<EquipmentAsset>> ROOT_ID = ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("equipment_asset"));
   ResourceKey<EquipmentAsset> LEATHER = createId("leather");
   ResourceKey<EquipmentAsset> COPPER = createId("copper");
   ResourceKey<EquipmentAsset> CHAINMAIL = createId("chainmail");
   ResourceKey<EquipmentAsset> IRON = createId("iron");
   ResourceKey<EquipmentAsset> GOLD = createId("gold");
   ResourceKey<EquipmentAsset> DIAMOND = createId("diamond");
   ResourceKey<EquipmentAsset> TURTLE_SCUTE = createId("turtle_scute");
   ResourceKey<EquipmentAsset> NETHERITE = createId("netherite");
   ResourceKey<EquipmentAsset> ARMADILLO_SCUTE = createId("armadillo_scute");
   ResourceKey<EquipmentAsset> ELYTRA = createId("elytra");
   ResourceKey<EquipmentAsset> SADDLE = createId("saddle");
   Map<DyeColor, ResourceKey<EquipmentAsset>> CARPETS = Util.makeEnumMap(DyeColor.class, color -> createId(color.getSerializedName() + "_carpet"));
   ResourceKey<EquipmentAsset> TRADER_LLAMA = createId("trader_llama");
   ResourceKey<EquipmentAsset> TRADER_LLAMA_BABY = createId("trader_llama_baby");
   Map<DyeColor, ResourceKey<EquipmentAsset>> HARNESSES = Util.makeEnumMap(DyeColor.class, color -> createId(color.getSerializedName() + "_harness"));

   static ResourceKey<EquipmentAsset> createId(final String name) {
      return ResourceKey.create(ROOT_ID, Identifier.withDefaultNamespace(name));
   }
}
