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
   Map<DyeColor, ResourceKey<EquipmentAsset>> CARPETS = Util.makeEnumMap(DyeColor.class, p_378269_ -> createId(p_378269_.getSerializedName() + "_carpet"));
   ResourceKey<EquipmentAsset> TRADER_LLAMA = createId("trader_llama");
   Map<DyeColor, ResourceKey<EquipmentAsset>> HARNESSES = Util.makeEnumMap(DyeColor.class, p_405662_ -> createId(p_405662_.getSerializedName() + "_harness"));

   static ResourceKey<EquipmentAsset> createId(String p_377499_) {
      return ResourceKey.create(ROOT_ID, Identifier.withDefaultNamespace(p_377499_));
   }
}
