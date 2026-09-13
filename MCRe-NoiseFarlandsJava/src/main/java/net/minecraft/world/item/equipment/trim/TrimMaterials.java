package net.minecraft.world.item.equipment.trim;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;

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

    public static void bootstrap(final BootstrapContext<TrimMaterial> context) {
        register(context, QUARTZ, Style.EMPTY.withColor(14931140), MaterialAssetGroup.QUARTZ);
        register(context, IRON, Style.EMPTY.withColor(15527148), MaterialAssetGroup.IRON);
        register(context, NETHERITE, Style.EMPTY.withColor(6445145), MaterialAssetGroup.NETHERITE);
        register(context, REDSTONE, Style.EMPTY.withColor(9901575), MaterialAssetGroup.REDSTONE);
        register(context, COPPER, Style.EMPTY.withColor(11823181), MaterialAssetGroup.COPPER);
        register(context, GOLD, Style.EMPTY.withColor(14594349), MaterialAssetGroup.GOLD);
        register(context, EMERALD, Style.EMPTY.withColor(1155126), MaterialAssetGroup.EMERALD);
        register(context, DIAMOND, Style.EMPTY.withColor(7269586), MaterialAssetGroup.DIAMOND);
        register(context, LAPIS, Style.EMPTY.withColor(4288151), MaterialAssetGroup.LAPIS);
        register(context, AMETHYST, Style.EMPTY.withColor(10116294), MaterialAssetGroup.AMETHYST);
        register(context, RESIN, Style.EMPTY.withColor(16545810), MaterialAssetGroup.RESIN);
    }

    private static void register(
        final BootstrapContext<TrimMaterial> context, final ResourceKey<TrimMaterial> registryKey, final Style hoverTextStyle, final MaterialAssetGroup assets
    ) {
        Component description = Component.translatable(Util.makeDescriptionId("trim_material", registryKey.identifier())).withStyle(hoverTextStyle);
        context.register(registryKey, new TrimMaterial(assets, description));
    }

    private static ResourceKey<TrimMaterial> registryKey(final String id) {
        return ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.withDefaultNamespace(id));
    }
}