package net.minecraft.world.item.equipment.trim;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ProvidesTrimMaterial;

public class TrimMaterials {
   public static final ResourceKey<TrimMaterial> QUARTZ = registryKey("quartz");
   public static final ResourceKey<TrimMaterial> IRON = registryKey("iron");
   public static final ResourceKey<TrimMaterial> NETHERITE = registryKey("netherite");
   public static final ResourceKey<TrimMaterial> REDSTONE = registryKey("redstone");
   public static final ResourceKey<TrimMaterial> COPPER = registryKey("copper");
   public static final ResourceKey<TrimMaterial> GOLD = registryKey("gold");
   public static final ResourceKey<TrimMaterial> EMERALD = registryKey("emerald");
   public static final ResourceKey<TrimMaterial> DIAMOND = registryKey("diamond");
   public static final ResourceKey<TrimMaterial> LAPIS = registryKey("lapis");
   public static final ResourceKey<TrimMaterial> AMETHYST = registryKey("amethyst");
   public static final ResourceKey<TrimMaterial> RESIN = registryKey("resin");

   public static void bootstrap(BootstrapContext<TrimMaterial> p_368813_) {
      register(p_368813_, QUARTZ, Style.EMPTY.withColor(14931140), MaterialAssetGroup.QUARTZ);
      register(p_368813_, IRON, Style.EMPTY.withColor(15527148), MaterialAssetGroup.IRON);
      register(p_368813_, NETHERITE, Style.EMPTY.withColor(6445145), MaterialAssetGroup.NETHERITE);
      register(p_368813_, REDSTONE, Style.EMPTY.withColor(9901575), MaterialAssetGroup.REDSTONE);
      register(p_368813_, COPPER, Style.EMPTY.withColor(11823181), MaterialAssetGroup.COPPER);
      register(p_368813_, GOLD, Style.EMPTY.withColor(14594349), MaterialAssetGroup.GOLD);
      register(p_368813_, EMERALD, Style.EMPTY.withColor(1155126), MaterialAssetGroup.EMERALD);
      register(p_368813_, DIAMOND, Style.EMPTY.withColor(7269586), MaterialAssetGroup.DIAMOND);
      register(p_368813_, LAPIS, Style.EMPTY.withColor(4288151), MaterialAssetGroup.LAPIS);
      register(p_368813_, AMETHYST, Style.EMPTY.withColor(10116294), MaterialAssetGroup.AMETHYST);
      register(p_368813_, RESIN, Style.EMPTY.withColor(16545810), MaterialAssetGroup.RESIN);
   }

   public static Optional<Holder<TrimMaterial>> getFromIngredient(HolderLookup.Provider p_363557_, ItemStack p_369735_) {
      ProvidesTrimMaterial providestrimmaterial = p_369735_.get(DataComponents.PROVIDES_TRIM_MATERIAL);
      return providestrimmaterial != null ? providestrimmaterial.unwrap(p_363557_) : Optional.empty();
   }

   private static void register(BootstrapContext<TrimMaterial> p_369807_, ResourceKey<TrimMaterial> p_365636_, Style p_361695_, MaterialAssetGroup p_391878_) {
      Component component = Component.translatable(Util.makeDescriptionId("trim_material", p_365636_.identifier())).withStyle(p_361695_);
      p_369807_.register(p_365636_, new TrimMaterial(p_391878_, component));
   }

   private static ResourceKey<TrimMaterial> registryKey(String p_360778_) {
      return ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.withDefaultNamespace(p_360778_));
   }
}
